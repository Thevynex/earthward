"""Prepare a bounded, immutable Copernicus GLO-30 pilot elevation grid.

The input is one public Cloud Optimized GeoTIFF. The output is a compact JSON grid
plus checksum/provenance manifest. Existing output directories are never replaced.
"""
from __future__ import annotations
import argparse, hashlib, json, math, re, shutil, tempfile
from datetime import datetime, timezone
from pathlib import Path

MAX_SOURCE_BYTES = 128 * 1024 * 1024
MAX_CELLS = 100_000
SOURCE_TEMPLATE = ('https://copernicus-dem-30m.s3.eu-central-1.amazonaws.com/'
                   'Copernicus_DSM_COG_10_N40_00_E029_00_DEM/'
                   'Copernicus_DSM_COG_10_N40_00_E029_00_DEM.tif')


def validate_area(area):
    if not isinstance(area, dict) or type(area.get('schema_version')) is not int or area['schema_version'] != 1:
        raise ValueError('Unsupported area schema')
    package_id = area.get('package_id')
    if not isinstance(package_id, str) or not re.fullmatch(r'[a-z][a-z0-9_-]{0,63}', package_id):
        raise ValueError('Invalid package id')
    bbox = area.get('bbox_south_west_north_east')
    if not isinstance(bbox, list) or len(bbox) != 4 or not all(type(v) in (int, float) and math.isfinite(v) for v in bbox):
        raise ValueError('Invalid bbox')
    south, west, north, east = bbox
    if not (40 <= south < north <= 41 and 29 <= west < east <= 30):
        raise ValueError('Pilot must remain inside the pinned N40 E029 tile')
    return package_id, tuple(map(float, bbox))


def encode(value):
    return (json.dumps(value, ensure_ascii=False, allow_nan=False, sort_keys=True, indent=2) + '\n').encode()


def build_grid(values, bounds, transform):
    rows = [[float(value) for value in row] for row in values]
    if not rows or not rows[0] or any(len(row) != len(rows[0]) for row in rows):
        raise ValueError('Empty or ragged elevation grid')
    if len(rows) * len(rows[0]) > MAX_CELLS:
        raise ValueError('Elevation cell budget exceeded')
    if any(not math.isfinite(value) or value < -500 or value > 9000 for row in rows for value in row):
        raise ValueError('Invalid elevation sample')
    west, south, east, north = map(float, bounds)
    return {'schema_version': 1, 'horizontal_crs': 'EPSG:4326',
            'vertical_reference': 'EGM2008_geoid_EPSG_3855', 'units': 'metres',
            'row_order': 'north_to_south', 'column_order': 'west_to_east',
            'rows': len(rows), 'columns': len(rows[0]),
            'sample_bounds_west_south_east_north': [west, south, east, north],
            'pixel_size_degrees': [abs(float(transform.a)), abs(float(transform.e))],
            'elevations_metres': rows}


def prepare(source, area, output, acquired_at):
    from rasterio import open as raster_open
    from rasterio.windows import from_bounds
    package_id, (south, west, north, east) = validate_area(area)
    source = Path(source); output = Path(output)
    if output.exists():
        raise FileExistsError('Refusing to overwrite elevation package')
    if not source.is_file() or source.stat().st_size <= 0 or source.stat().st_size > MAX_SOURCE_BYTES:
        raise ValueError('Invalid source size')
    with raster_open(source) as dataset:
        if dataset.crs is None or dataset.crs.to_epsg() != 4326 or dataset.count != 1:
            raise ValueError('Unexpected source raster layout')
        window = from_bounds(west, south, east, north, dataset.transform).round_offsets().round_lengths()
        values = dataset.read(1, window=window, masked=True)
        if values.size == 0 or bool(values.mask.any()):
            raise ValueError('Pilot contains missing elevation samples')
        grid = build_grid(values.tolist(), dataset.window_bounds(window), dataset.window_transform(window))
    grid_bytes = encode(grid)
    manifest = {'schema_version': 1, 'package_id': package_id,
        'kind': 'development_elevation_extract', 'generation_ready': False,
        'source': {'dataset': 'Copernicus DEM GLO-30 Public 2021', 'url': SOURCE_TEMPLATE,
                   'access': 'public AWS Open Data COG', 'acquired_at': acquired_at,
                   'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                   'license_url': 'https://dataspace.copernicus.eu/sites/default/files/media/files/2025-06/copernicus_contributing_mission_data_access_v2_cop_dem_licenses.pdf'},
        'files': {'elevation.json': {'bytes': len(grid_bytes), 'sha256': hashlib.sha256(grid_bytes).hexdigest()}},
        'limitations': ['30 metre DSM samples are not one-metre survey data.',
                        'DSM includes vegetation and structures; it is not a bare-earth DTM.',
                        'No horizontal or vertical compression is applied.',
                        'World generation and safe-spawn validation remain disabled.']}
    temporary = Path(tempfile.mkdtemp(prefix='.earthward-elevation-', dir=output.parent))
    try:
        (temporary / 'elevation.json').write_bytes(grid_bytes)
        (temporary / 'elevation-manifest.json').write_bytes(encode(manifest))
        (temporary / 'ELEVATION_ATTRIBUTION.txt').write_text(
            'Copernicus DEM GLO-30 Public 2021. Accessed from AWS Open Data.\n'
            'Copernicus DEM licence and disclaimer apply; see elevation-manifest.json.\n'
            'This is a DSM at approximately 30 m sample spacing, not 1 m measured terrain.\n', encoding='utf-8')
        temporary.rename(output)
    except BaseException:
        shutil.rmtree(temporary, ignore_errors=True); raise
    return grid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--area', type=Path, required=True)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    area = json.loads(args.area.read_text(encoding='utf-8'))
    grid = prepare(args.source, area, args.output, datetime.now(timezone.utc).isoformat())
    print(f"::notice title=Pilot elevation acquired::{grid['rows']}x{grid['columns']} Copernicus GLO-30 DSM grid; generation remains disabled")

if __name__ == '__main__':
    main()

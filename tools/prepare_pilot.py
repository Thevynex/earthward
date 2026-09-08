"""Bounded OSM development extract. Not a terrain/world generator.

Downloaded OSM-derived geometry remains ODbL-1.0, not Earthward's code license.
Only explicit --fetch performs network I/O. Existing outputs are never replaced.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import re
import shutil
import tempfile
import urllib.parse
import urllib.request

ENDPOINT = 'https://overpass-api.de/api/interpreter'
MAX_BYTES = 16 * 1024 * 1024
MAX_ELEMENTS = 20000
MAX_POINTS = 200000
TAG_ALLOWLIST = frozenset({
    'building', 'building:part', 'building:levels', 'building:min_level',
    'height', 'min_height', 'roof:shape', 'roof:height', 'roof:levels',
    'highway', 'surface', 'width', 'lanes', 'bridge', 'tunnel', 'layer', 'area',
})


def finite_number(value):
    return type(value) in (int, float) and math.isfinite(value)


def validate_area(area):
    if not isinstance(area, dict) or type(area.get('schema_version')) is not int or area['schema_version'] != 1:
        raise ValueError('Unsupported area schema')
    package_id = area.get('package_id', '')
    if not isinstance(package_id, str) or not re.fullmatch(r'[a-z][a-z0-9_-]{0,63}', package_id):
        raise ValueError('Invalid package identifier')
    bbox = area.get('bbox_south_west_north_east')
    if not isinstance(bbox, list) or len(bbox) != 4 or not all(map(finite_number, bbox)):
        raise ValueError('Invalid bounding box')
    south, west, north, east = bbox
    if not (-90 < south < north < 90 and -180 <= west < east < 180):
        raise ValueError('Pilot boxes cannot cross the dateline or include poles')
    if north - south > 0.02 or east - west > 0.02:
        raise ValueError('Development fetch limited to 0.02 degrees per axis')
    return package_id, bbox


def build_query(area):
    _, bbox = validate_area(area)
    bounds = ','.join(format(value, '.8f') for value in bbox)
    return ('[out:json][timeout:45][maxsize:67108864];\n('
            f'way["building"]({bounds});way["building:part"]({bounds});'
            f'way["highway"]({bounds});relation["building"]({bounds});'
            ');out body geom;\n')


def decode_source(raw):
    if len(raw) > MAX_BYTES:
        raise ValueError('Source exceeds 16 MiB limit')
    root = json.loads(raw)
    if not isinstance(root, dict) or root.get('remark'):
        raise ValueError('Overpass returned a partial result or invalid document')
    elements = root.get('elements')
    if not isinstance(elements, list) or len(elements) > MAX_ELEMENTS:
        raise ValueError('Invalid or oversized element list')
    snapshot = root.get('osm3s', {}).get('timestamp_osm_base')
    if not isinstance(snapshot, str) or not snapshot.endswith('Z'):
        raise ValueError('Missing source snapshot timestamp')
    datetime.fromisoformat(snapshot.replace('Z', '+00:00'))
    return elements, snapshot


def extract_geometry(elements):
    features = []
    skipped = Counter()
    seen = set()
    total_points = 0
    for element in elements:
        if not isinstance(element, dict):
            raise ValueError('Invalid element')
        if element.get('type') != 'way':
            skipped['unsupported_' + ('relation' if element.get('type') == 'relation' else 'element')] += 1
            continue
        source_id = element.get('id')
        if type(source_id) is not int or source_id <= 0:
            raise ValueError('Invalid OSM way identifier')
        if source_id in seen:
            raise ValueError('Duplicate OSM way identifier')
        seen.add(source_id)
        source_tags = element.get('tags', {})
        if not isinstance(source_tags, dict):
            raise ValueError('Invalid tags')
        tags = {key: value for key, value in source_tags.items()
                if key in TAG_ALLOWLIST and isinstance(value, str) and len(value) <= 128}
        building = any(tags.get(key) not in (None, '', 'no') for key in ('building', 'building:part'))
        road = tags.get('highway') not in (None, '', 'no')
        if not building and not road:
            skipped['unselected_way'] += 1
            continue
        geometry = element.get('geometry')
        if not isinstance(geometry, list) or len(geometry) < 2:
            skipped['missing_geometry'] += 1
            continue
        total_points += len(geometry)
        if total_points > MAX_POINTS:
            raise ValueError('Geometry point budget exceeded')
        points = []
        for point in geometry:
            if not isinstance(point, dict):
                break
            lat, lon = point.get('lat'), point.get('lon')
            if not (finite_number(lat) and finite_number(lon) and -90 <= lat <= 90 and -180 <= lon <= 180):
                break
            points.append([lon, lat])
        if len(points) != len(geometry):
            skipped['incomplete_or_invalid_geometry'] += 1
            continue
        layers = []
        if building:
            if len(points) >= 4 and points[0] == points[-1] and len({tuple(p) for p in points[:-1]}) >= 3:
                layers.append('building_outline')
            else:
                skipped['open_or_degenerate_building'] += 1
        if road:
            if tags.get('area') == 'yes':
                skipped['road_area_not_centerline'] += 1
            else:
                layers.append('road_centerline')
        if layers:
            features.append({'id': f'osm:way:{source_id}', 'layers': layers,
                             'coordinates_lon_lat': points, 'source_tags': tags})
    if not features:
        raise ValueError('No usable geometry: package was not created')
    features.sort(key=lambda feature: feature['id'])
    return features, dict(sorted(skipped.items()))


def encode_json(value):
    return (json.dumps(value, ensure_ascii=False, allow_nan=False, indent=2, sort_keys=True) + '\n').encode('utf-8')


def build_package(raw, area, acquired_at, acquisition):
    package_id, bbox = validate_area(area)
    elements, snapshot = decode_source(raw)
    features, skipped = extract_geometry(elements)
    geometry = encode_json({'schema_version': 1, 'crs': 'EPSG:4326', 'features': features})
    counts = Counter(layer for feature in features for layer in feature['layers'])
    manifest = {
        'schema_version': 1, 'package_id': package_id,
        'kind': 'development_geometry_extract', 'generation_ready': False,
        'horizontal_crs': 'EPSG:4326', 'coordinate_order': 'longitude_latitude',
        'coordinate_units': 'degrees', 'vertical_reference': None,
        'target_world_metres_per_block': 1, 'world_projection_implemented': False,
        'requested_bbox_south_west_north_east': bbox,
        'boundary_kind': 'approximate_pilot_rectangle_not_administrative_boundary',
        'features_clipped_to_rectangle': False,
        'source': {'provider': 'OpenStreetMap contributors', 'acquisition': acquisition,
                   'endpoint': ENDPOINT if acquisition == 'overpass' else None,
                   'snapshot_at': snapshot, 'acquired_at': acquired_at,
                   'license': 'ODbL-1.0', 'license_url': 'https://opendatacommons.org/licenses/odbl/1-0/',
                   'attribution_url': 'https://www.openstreetmap.org/copyright',
                   'raw_input_sha256': hashlib.sha256(raw).hexdigest()},
        'files': {'geometry.json': {'sha256': hashlib.sha256(geometry).hexdigest(), 'bytes': len(geometry)}},
        'feature_counts': dict(sorted(counts.items())), 'skipped': skipped,
        'missing': ['elevation_model', 'facade_textures', 'verified_spawn', 'interiors',
                    'building_multipolygon_relations', 'verified_building_height_coverage'],
        'inferences_added': [],
        'limitations': ['OSM geometry accuracy and completeness are not guaranteed.',
                       'Building outline closure is checked, but topology/self-intersections are not validated.',
                       'Heights and levels, when tagged, are raw unverified source strings.',
                       'A source snapshot date is not an individual feature survey/update date.',
                       'Names, contact, address, operator and contributor metadata are not retained.',
                       'Source tags are untrusted data, never instructions or executable content.'],
    }
    attribution = ('© OpenStreetMap contributors\nhttps://www.openstreetmap.org/copyright\n'
                   'This transformed geometry extract is licensed under ODbL-1.0:\n'
                   'https://opendatacommons.org/licenses/odbl/1-0/\n'
                   'Earthward restricted code license does not apply to this OSM-derived data.\n'
                   'Transformation: selected way geometry and allowlisted physical tags only; '
                   'unsupported/incomplete features counted in manifest. No inferred geometry added.\n')
    return {'manifest.json': encode_json(manifest), 'geometry.json': geometry,
            'ATTRIBUTION.txt': attribution.encode('utf-8'), 'source-query.overpass': build_query(area).encode('utf-8')}


def write_package(output, files):
    output = Path(output)
    if output.exists():
        raise FileExistsError('Refusing to overwrite an existing package')
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix='.earthward-', dir=output.parent))
    try:
        for name, content in files.items():
            (temporary / name).write_bytes(content)
        temporary.rename(output)
    except BaseException:
        shutil.rmtree(temporary, ignore_errors=True)
        raise


def fetch_source(query):
    request = urllib.request.Request(ENDPOINT,
        data=urllib.parse.urlencode({'data': query}).encode('utf-8'),
        headers={'User-Agent': 'Earthward-Development/0.1 (+https://github.com/Thevynex/earthward)',
                 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'})
    # Single bounded request. No automatic retries, tile fetching, or credentials.
    with urllib.request.urlopen(request, timeout=60) as response:
        raw = response.read(MAX_BYTES + 1)
    if len(raw) > MAX_BYTES:
        raise ValueError('Download exceeds 16 MiB limit')
    return raw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--area', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--fetch', action='store_true')
    source.add_argument('--input', type=Path)
    args = parser.parse_args()
    if args.output.exists():
        parser.error('Output already exists; refusing overwrite before reading or fetching')
    area_bytes = args.area.read_bytes()
    if len(area_bytes) > 4096:
        parser.error('Area definition exceeds 4 KiB')
    area = json.loads(area_bytes)
    query = build_query(area)
    if args.fetch:
        raw = fetch_source(query)
    else:
        with args.input.open('rb') as source_file:
            raw = source_file.read(MAX_BYTES + 1)
    acquired_at = datetime.now(timezone.utc).isoformat()
    files = build_package(raw, area, acquired_at, 'overpass' if args.fetch else 'local_input_unverified_origin')
    write_package(args.output, files)
    counts = json.loads(files['manifest.json'])['feature_counts']
    print('Geometry package written; world generation remains unavailable.')
    print('Counts: ' + json.dumps(counts, sort_keys=True))
    if args.fetch:
        print('::notice title=Pilot geometry acquired::' + json.dumps(counts, sort_keys=True)
              + '; elevation and world generation not implemented.')


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        detail = (type(error).__name__ + ': ' + str(error))[:500]
        detail = detail.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
        print('::error title=Pilot preparation failed::' + detail)
        raise SystemExit(1)

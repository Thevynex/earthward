import json, math, tempfile, unittest
from pathlib import Path
import prepare_elevation as elevation

AREA = {'schema_version': 1, 'package_id': 'tr_test',
        'bbox_south_west_north_east': [40.98, 29.02, 40.99, 29.03]}

class Transform:
    a = 1 / 3600
    e = -1 / 3600

class ElevationTests(unittest.TestCase):
    def test_pilot_area_is_pinned_to_expected_tile(self):
        package_id, bbox = elevation.validate_area(AREA)
        self.assertEqual('tr_test', package_id)
        self.assertEqual((40.98, 29.02, 40.99, 29.03), bbox)

    def test_nan_and_boolean_rejected(self):
        area = dict(AREA, bbox_south_west_north_east=[40.98, 29.02, math.nan, 29.03])
        with self.assertRaises(ValueError): elevation.validate_area(area)
        with self.assertRaises(ValueError): elevation.validate_area(dict(AREA, schema_version=True))

    def test_grid_keeps_uncompressed_metric_values(self):
        grid = elevation.build_grid([[10.25, 11.5], [12.75, 13.0]], (29, 40, 30, 41), Transform())
        self.assertEqual([[10.25, 11.5], [12.75, 13.0]], grid['elevations_metres'])
        self.assertEqual('metres', grid['units'])
        self.assertEqual('EGM2008_geoid_EPSG_3855', grid['vertical_reference'])

    def test_missing_and_extreme_samples_rejected(self):
        with self.assertRaises(ValueError): elevation.build_grid([[0, float('nan')]], (29, 40, 30, 41), Transform())
        with self.assertRaises(ValueError): elevation.build_grid([[10000]], (29, 40, 30, 41), Transform())

    def test_output_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'existing'
            output.mkdir()
            source = Path(directory) / 'source.tif'; source.write_bytes(b'x')
            with self.assertRaises(FileExistsError): elevation.prepare(source, AREA, output, '2026-09-09T00:00:00Z')

if __name__ == '__main__': unittest.main()

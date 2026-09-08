"""Synthetic input tests: never evidence of real-world geographic accuracy."""
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

import prepare_pilot as pipeline

AREA = {'schema_version': 1, 'package_id': 'test_pilot',
        'bbox_south_west_north_east': [40.9803, 29.0208, 40.9893, 29.0328]}
WAY = {'type': 'way', 'id': 1, 'tags': {'building': 'yes', 'name': 'DO NOT RETAIN',
       'phone': 'DO NOT RETAIN', 'operator': 'DO NOT RETAIN', 'height': '12'},
       'geometry': [{'lat': 40.982, 'lon': 29.022}, {'lat': 40.983, 'lon': 29.022},
                    {'lat': 40.983, 'lon': 29.023}, {'lat': 40.982, 'lon': 29.022}]}


def raw(elements=None, **extra):
    return json.dumps({'osm3s': {'timestamp_osm_base': '2026-09-08T00:00:00Z'},
                       'elements': [copy.deepcopy(WAY)] if elements is None else elements, **extra}).encode()


class PilotTests(unittest.TestCase):
    def test_query_is_bounded_and_not_meta(self):
        query = pipeline.build_query(AREA)
        self.assertIn('[timeout:45]', query)
        self.assertIn('40.98030000,29.02080000,40.98930000,29.03280000', query)
        self.assertNotIn('out meta', query)

    def test_oversized_area_rejected(self):
        area = copy.deepcopy(AREA)
        area['bbox_south_west_north_east'][2] = 42
        with self.assertRaises(ValueError): pipeline.validate_area(area)

    def test_dateline_area_rejected(self):
        area = copy.deepcopy(AREA)
        area['bbox_south_west_north_east'] = [0, 179.99, .01, -179.99]
        with self.assertRaises(ValueError): pipeline.validate_area(area)

    def test_nan_rejected(self):
        area = copy.deepcopy(AREA)
        area['bbox_south_west_north_east'][0] = float('nan')
        with self.assertRaises(ValueError): pipeline.validate_area(area)

    def test_identifier_traversal_rejected(self):
        area = dict(AREA, package_id='../save')
        with self.assertRaises(ValueError): pipeline.validate_area(area)

    def test_boolean_schema_rejected(self):
        with self.assertRaises(ValueError): pipeline.validate_area(dict(AREA, schema_version=True))

    def test_partial_overpass_result_rejected(self):
        with self.assertRaises(ValueError): pipeline.decode_source(raw(remark='runtime error: timeout'))

    def test_missing_timestamp_rejected(self):
        with self.assertRaises(ValueError): pipeline.decode_source(b'{"elements": []}')

    def test_oversized_source_rejected(self):
        with self.assertRaises(ValueError): pipeline.decode_source(b' ' * (pipeline.MAX_BYTES + 1))

    def test_empty_result_not_packaged(self):
        with self.assertRaises(ValueError): pipeline.extract_geometry([])

    def test_tags_minimized_and_coordinates_not_changed(self):
        features, skipped = pipeline.extract_geometry([copy.deepcopy(WAY)])
        self.assertEqual({}, skipped)
        self.assertEqual({'building': 'yes', 'height': '12'}, features[0]['source_tags'])
        self.assertEqual([29.022, 40.982], features[0]['coordinates_lon_lat'][0])
        self.assertEqual('osm:way:1', features[0]['id'])

    def test_duplicates_rejected(self):
        with self.assertRaises(ValueError): pipeline.extract_geometry([WAY, WAY])

    def test_open_building_and_relation_counted_not_invented(self):
        open_way = copy.deepcopy(WAY)
        open_way['id'] = 2
        open_way['geometry'].pop()
        _, skipped = pipeline.extract_geometry([WAY, open_way, {'type': 'relation', 'id': 3}])
        self.assertEqual(1, skipped['open_or_degenerate_building'])
        self.assertEqual(1, skipped['unsupported_relation'])

    def test_incomplete_geometry_counted(self):
        broken = copy.deepcopy(WAY)
        broken['id'] = 2
        broken['geometry'][1] = None
        _, skipped = pipeline.extract_geometry([WAY, broken])
        self.assertEqual(1, skipped['incomplete_or_invalid_geometry'])

    def test_package_hash_provenance_and_no_ready_flag(self):
        files = pipeline.build_package(raw(), AREA, '2026-09-08T00:00:01+00:00', 'local_input_unverified_origin')
        manifest = json.loads(files['manifest.json'])
        self.assertFalse(manifest['generation_ready'])
        self.assertFalse(manifest['world_projection_implemented'])
        self.assertIsNone(manifest['source']['endpoint'])
        self.assertEqual('ODbL-1.0', manifest['source']['license'])
        self.assertEqual('degrees', manifest['coordinate_units'])
        self.assertEqual(hashlib.sha256(files['geometry.json']).hexdigest(), manifest['files']['geometry.json']['sha256'])
        self.assertNotIn(b'DO NOT RETAIN', files['geometry.json'])

    def test_road_area_not_misrepresented_as_centerline(self):
        road = copy.deepcopy(WAY)
        road['id'] = 2
        road['tags'] = {'highway': 'pedestrian', 'area': 'yes'}
        features, skipped = pipeline.extract_geometry([WAY, road])
        self.assertEqual(1, len(features))
        self.assertEqual(1, skipped['road_area_not_centerline'])

    def test_existing_package_not_replaced(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'package'
            files = pipeline.build_package(raw(), AREA, '2026-09-08T00:00:01+00:00', 'local_input_unverified_origin')
            pipeline.write_package(output, files)
            before = (output / 'geometry.json').read_bytes()
            with self.assertRaises(FileExistsError): pipeline.write_package(output, {'geometry.json': b'changed'})
            self.assertEqual(before, (output / 'geometry.json').read_bytes())


if __name__ == '__main__':
    unittest.main()

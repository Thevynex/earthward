package io.github.thevynex.earthward.geo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class GeoGeometryTest {
    private static final String BUILDING = """
            {"schema_version":1,"crs":"EPSG:4326","features":[
              {"id":"osm:way:10","layers":["building_outline"],"coordinates_lon_lat":
                [[29.0,41.0],[29.001,41.0],[29.001,41.001],[29.0,41.0]],"source_tags":{"building":"yes"}}
            ]}
            """;

    @Test void parsesClosedBuildingAndBounds() {
        var geometry = GeoGeometry.parse(BUILDING);
        assertEquals(1, geometry.buildingCount());
        assertEquals(0, geometry.roadCount());
        assertEquals(4, geometry.pointCount());
        assertEquals(29.001, geometry.bounds().east());
    }

    @Test void parsesRoad() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:11","layers":["road_centerline"],"coordinates_lon_lat":[[29,41],[29.1,41.1]]}]}
                """);
        assertEquals(1, geometry.roadCount());
    }

    @Test void rejectsOpenBuilding() {
        assertThrows(IllegalArgumentException.class, () -> GeoGeometry.parse(BUILDING.replace("[29.0,41.0]]", "[29.0,41.1]]")));
    }

    @Test void rejectsUnknownLayer() {
        assertThrows(IllegalArgumentException.class, () -> GeoGeometry.parse(BUILDING.replace("building_outline", "instruction")));
    }

    @Test void rejectsDuplicateId() {
        String feature = "{\"id\":\"osm:way:1\",\"layers\":[\"road_centerline\"],\"coordinates_lon_lat\":[[29,41],[30,42]]}";
        assertThrows(IllegalArgumentException.class, () -> GeoGeometry.parse(
                "{\"schema_version\":1,\"crs\":\"EPSG:4326\",\"features\":[" + feature + "," + feature + "]}"));
    }

    @Test void rejectsNonFiniteCoordinate() {
        assertThrows(RuntimeException.class, () -> GeoGeometry.parse(BUILDING.replace("29.001", "1e999")));
    }

    @Test void dataIsImmutable() {
        var geometry = GeoGeometry.parse(BUILDING);
        assertThrows(UnsupportedOperationException.class, () -> geometry.features().clear());
        assertThrows(UnsupportedOperationException.class, () -> geometry.features().getFirst().points().clear());
    }
}

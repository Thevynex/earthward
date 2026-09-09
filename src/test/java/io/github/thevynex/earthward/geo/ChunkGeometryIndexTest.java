package io.github.thevynex.earthward.geo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkGeometryIndexTest {
    @Test void indexesFeaturesOnlyIntoIntersectedChunkBounds() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["road_centerline"],"coordinates_lon_lat":[[29.0,41.0],[29.0001,41.0]]},
                {"id":"osm:way:2","layers":["road_centerline"],"coordinates_lon_lat":[[29.001,41.001],[29.0011,41.001]]}]}
                """);
        var index = ChunkGeometryIndex.from(geometry);
        assertEquals(2, index.features().size());
        assertTrue(index.indexedChunkCount() >= 2);
        assertTrue(index.membershipCount() >= index.features().size());
        assertThrows(UnsupportedOperationException.class, () -> index.features().clear());
        assertTrue(index.featuresForChunk(1_000_000, 1_000_000).isEmpty());
    }

    @Test void floorChunkMappingHandlesNegativeMetres() {
        assertEquals(0, ChunkGeometryIndex.blockToChunk(0));
        assertEquals(0, ChunkGeometryIndex.blockToChunk(15.999));
        assertEquals(1, ChunkGeometryIndex.blockToChunk(16));
        assertEquals(-1, ChunkGeometryIndex.blockToChunk(-0.001));
        assertEquals(-1, ChunkGeometryIndex.blockToChunk(-16));
        assertEquals(-2, ChunkGeometryIndex.blockToChunk(-16.001));
    }

    @Test void aFeatureCanBeRetrievedFromItsIndexedChunk() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:9","layers":["road_centerline"],"coordinates_lon_lat":[[29,41],[29.0002,41]]}]}
                """);
        var index = ChunkGeometryIndex.from(geometry);
        var point = index.features().getFirst().points().getFirst();
        int x = ChunkGeometryIndex.blockToChunk(point.x());
        int z = ChunkGeometryIndex.blockToChunk(point.z());
        assertEquals("osm:way:9", index.featuresForChunk(x, z).getFirst().id());
    }
}

package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SafeSpawnValidatorTest {
    private static ChunkFeatureRasterizer rasterizer() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["road_centerline"],"coordinates_lon_lat":[[29,41],[29.0002,41]]}]}
                """);
        return new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry));
    }

    @Test void findsNearestClearLowSlopeCandidate() {
        var validator = new SafeSpawnValidator(rasterizer(), (x, z) -> 72);
        var spawn = validator.findNearest(0, 0, 32).orElseThrow();
        assertEquals(73, spawn.y());
        assertTrue(spawn.distanceMetres() <= 32);
        assertTrue(spawn.examinedCandidates() > 0);
    }

    @Test void rejectsPermanentlySteepSurface() {
        var validator = new SafeSpawnValidator(rasterizer(),
                (x, z) -> Math.floorMod(x + z, 2) == 0 ? 70 : 80);
        assertTrue(validator.findNearest(0, 0, 8).isEmpty());
    }

    @Test void enforcesBoundedSearch() {
        var validator = new SafeSpawnValidator(rasterizer(), (x, z) -> 70);
        assertThrows(IllegalArgumentException.class, () -> validator.findNearest(0, 0, 257));
    }
}

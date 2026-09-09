package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkFeatureRasterizerTest {
    private static ChunkFeatureRasterizer rasterizer() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["building_outline"],"coordinates_lon_lat":[
                  [29.0000,41.0000],[29.00012,41.0000],[29.00012,41.00010],[29.0000,41.00010],[29.0000,41.0000]]},
                {"id":"osm:way:2","layers":["road_centerline"],"coordinates_lon_lat":[
                  [28.9999,40.99995],[29.0002,40.99995]]}]}
                """);
        return new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry));
    }

    @Test void fillsBuildingFootprintsAndBuffersRoadCenterlines() {
        var rasterizer = rasterizer();
        int roads = 0, buildings = 0;
        for (int chunkX = -2; chunkX <= 1; chunkX++) {
            for (int chunkZ = -1; chunkZ <= 1; chunkZ++) {
                var mask = rasterizer.rasterize(chunkX, chunkZ);
                roads += mask.roadBlocks(); buildings += mask.buildingBlocks();
                assertTrue(mask.roadWidthInferred());
            }
        }
        assertTrue(roads > 0);
        assertTrue(buildings > 0);
    }

    @Test void returnedMasksAreDefensivelyCopied() {
        var mask = rasterizer().rasterize(0, 0);
        byte[] copy = mask.cells();
        copy[0] = 99;
        assertNotEquals(99, mask.cells()[0]);
    }

    @Test void distantChunksRemainEmpty() {
        var mask = rasterizer().rasterize(1000, 1000);
        assertEquals(0, mask.roadBlocks());
        assertEquals(0, mask.buildingBlocks());
    }
}

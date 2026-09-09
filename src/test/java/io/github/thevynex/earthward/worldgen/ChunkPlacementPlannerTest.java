package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkPlacementPlannerTest {
    @Test void createsRoadAndBuildingShellPlacementsAboveTerrain() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["building_outline"],"coordinates_lon_lat":[
                [29,41],[29.00012,41],[29.00012,41.00010],[29,41.00010],[29,41]]},
                {"id":"osm:way:2","layers":["road_centerline"],"coordinates_lon_lat":[
                [28.9999,40.99995],[29.0002,40.99995]]}]}
                """);
        var planner = new ChunkPlacementPlanner(new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry)));
        int[] heights = new int[256]; Arrays.fill(heights, 70);
        int road = 0, foundation = 0, wall = 0, roof = 0;
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) {
            for (var placement : planner.plan(x, z, heights).placements()) switch (placement.material()) {
                case ROAD -> road++; case FOUNDATION -> foundation++; case WALL -> wall++; case ROOF -> roof++;
            }
        }
        assertTrue(road > 0 && foundation > 0 && wall > 0 && roof > 0);
    }

    @Test void rejectsMissingTerrainAndReturnsImmutablePlan() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["road_centerline"],"coordinates_lon_lat":[[29,41],[29.0001,41]]}]}
                """);
        var planner = new ChunkPlacementPlanner(new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry)));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(0, 0, new int[1]));
        assertThrows(UnsupportedOperationException.class, () -> planner.plan(0, 0, new int[256]).placements().clear());
    }
}

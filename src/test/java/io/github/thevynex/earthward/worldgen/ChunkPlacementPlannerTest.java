package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkPlacementPlannerTest {
    @Test void createsRoadBuildingFacadeAndStreetPlacements() {
        var geometry = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["building_outline"],"coordinates_lon_lat":[
                [29,41],[29.00012,41],[29.00012,41.00010],[29,41.00010],[29,41]]},
                {"id":"osm:way:2","layers":["road_centerline"],"coordinates_lon_lat":[
                [28.9999,40.99995],[29.0002,40.99995]]}]}
                """);
        var planner = new ChunkPlacementPlanner(new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry)));
        int[] heights = new int[256]; Arrays.fill(heights, 70);
        var found = EnumSet.noneOf(ChunkPlacementPlanner.Material.class);
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) {
            var plan = planner.plan(x, z, heights);
            assertTrue(plan.facadePatternInferred());
            plan.placements().forEach(placement -> found.add(placement.material()));
        }
        assertTrue(found.contains(ChunkPlacementPlanner.Material.ROAD));
        assertTrue(found.contains(ChunkPlacementPlanner.Material.SIDEWALK));
        assertTrue(found.contains(ChunkPlacementPlanner.Material.FOUNDATION));
        assertTrue(found.contains(ChunkPlacementPlanner.Material.WALL));
        assertTrue(found.contains(ChunkPlacementPlanner.Material.WINDOW));
        assertTrue(found.contains(ChunkPlacementPlanner.Material.ROOF));
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

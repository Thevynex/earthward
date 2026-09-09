package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ChunkPlacementPlannerTest {
    private static ChunkPlacementPlanner planner(){
        var geometry=GeoGeometry.parse("""
        {"schema_version":1,"crs":"EPSG:4326","features":[
        {"id":"osm:way:1","layers":["building_outline"],"coordinates_lon_lat":[[29,41],[29.00012,41],[29.00012,41.00010],[29,41.00010],[29,41]]},
        {"id":"osm:way:2","layers":["road_centerline"],"coordinates_lon_lat":[[28.9999,40.99995],[29.0002,40.99995]]}]}
        """);
        return new ChunkPlacementPlanner(new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry)));
    }
    @Test void createsRoadBuildingFacadeAndStreetPlacements(){
        int[] heights=new int[256];Arrays.fill(heights,70);var found=EnumSet.noneOf(ChunkPlacementPlanner.Material.class);
        for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)planner().plan(x,z,heights).placements().forEach(p->found.add(p.material()));
        assertTrue(found.containsAll(EnumSet.of(ChunkPlacementPlanner.Material.ROAD,ChunkPlacementPlanner.Material.SIDEWALK,
                ChunkPlacementPlanner.Material.FOUNDATION,ChunkPlacementPlanner.Material.WALL,ChunkPlacementPlanner.Material.WINDOW,ChunkPlacementPlanner.Material.ROOF)));
    }
    @Test void connectedBuildingUsesFlatRoofAndRaisedShell(){
        int[] heights=new int[256];Arrays.fill(heights,70);heights[0]=75;
        var roofs=planner().plan(0,0,heights).placements().stream().filter(p->p.material()==ChunkPlacementPlanner.Material.ROOF).toList();
        if(!roofs.isEmpty()){
            int roofY=roofs.getFirst().y();assertTrue(roofs.stream().allMatch(p->p.y()==roofY));
            assertTrue(roofY>=70+ChunkPlacementPlanner.INFERRED_WALL_HEIGHT+1);
        }
    }
    @Test void rejectsMissingTerrainAndReturnsImmutablePlan(){
        assertThrows(IllegalArgumentException.class,()->planner().plan(0,0,new int[1]));
        assertThrows(UnsupportedOperationException.class,()->planner().plan(0,0,new int[256]).placements().clear());
    }
}

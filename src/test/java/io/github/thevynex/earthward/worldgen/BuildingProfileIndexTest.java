package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class BuildingProfileIndexTest {
    private static ChunkGeometryIndex geometry(){return ChunkGeometryIndex.from(GeoGeometry.parse("""
    {"schema_version":1,"crs":"EPSG:4326","features":[
    {"id":"osm:way:123","layers":["building_outline"],"coordinates_lon_lat":[[29,41],[29.0004,41],[29.0004,41.0002],[29,41.0002],[29,41]]}]}
    """));}
    @Test void createsStableCrossChunkProfile(){
        var profiles=new BuildingProfileIndex(geometry(),(x,z)->70+(int)Math.floor(Math.abs(x)/20));
        var profile=profiles.require("osm:way:123");assertEquals(1,profiles.size());assertTrue(profile.baseY()>=70);assertTrue(profile.wallHeight()>=6);assertEquals(profile.wallHeight(),profile.floors()*3);assertTrue(profile.heightInferred());
    }
    @Test void rasterCellsRetainOsmIdentity(){
        var rasterizer=new ChunkFeatureRasterizer(geometry());boolean found=false;
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(String id:rasterizer.rasterize(x,z).buildingIds())if("osm:way:123".equals(id))found=true;
        assertTrue(found);
    }
}

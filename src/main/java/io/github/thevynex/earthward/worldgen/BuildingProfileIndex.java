package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import java.util.HashMap;
import java.util.Map;

/** Immutable region-wide profiles keep one base and height for a building across every chunk it touches. */
public final class BuildingProfileIndex {
    private final Map<String,Profile> profiles;
    public BuildingProfileIndex(ChunkGeometryIndex geometry,SurfaceHeight height){
        if(geometry==null||height==null)throw new IllegalArgumentException("Geometry and height sampler required");
        var built=new HashMap<String,Profile>();
        for(var feature:geometry.features())if(feature.layers().contains(GeoGeometry.Layer.BUILDING_OUTLINE)){
            int base=Integer.MIN_VALUE;double sumX=0,sumZ=0;int count=0;
            for(var point:feature.points()){base=Math.max(base,height.surfaceY(point.x(),point.z()));sumX+=point.x();sumZ+=point.z();count++;}
            base=Math.max(base,height.surfaceY(sumX/count,sumZ/count));
            double area=Math.max(1,(feature.bounds().east()-feature.bounds().west())*(feature.bounds().south()-feature.bounds().north()));
            int floors=Math.max(2,Math.min(5,2+(int)Math.floor(Math.sqrt(area)/30.0)));
            built.put(feature.id(),new Profile(feature.id(),base,floors,floors*3,true));
        }
        profiles=Map.copyOf(built);
    }
    public Profile require(String id){var profile=profiles.get(id);if(profile==null)throw new IllegalArgumentException("Unknown building: "+id);return profile;}
    public int size(){return profiles.size();}
    @FunctionalInterface public interface SurfaceHeight{int surfaceY(double worldX,double worldZ);}
    public record Profile(String id,int baseY,int floors,int wallHeight,boolean heightInferred){
        public Profile{if(id==null||id.isBlank()||floors<1||wallHeight!=floors*3)throw new IllegalArgumentException("Invalid building profile");}
    }
}

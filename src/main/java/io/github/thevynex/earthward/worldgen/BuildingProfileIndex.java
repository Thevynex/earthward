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
            for(var point:feature.points()){
                base=sampleMaximum(height,point.x(),point.z(),base);
                sumX+=point.x();sumZ+=point.z();count++;
            }
            if(count>0)base=sampleMaximum(height,sumX/count,sumZ/count,base);
            // OSM outlines and the bounded DEM can differ by sub-metre projection/rounding at package edges.
            // A single edge vertex must not abort the whole world-creation flow.
            if(base==Integer.MIN_VALUE)base=PilotTerrainSampler.SEA_LEVEL_Y;
            double area=Math.max(1,(feature.bounds().east()-feature.bounds().west())*(feature.bounds().south()-feature.bounds().north()));
            int floors=Math.max(2,Math.min(5,2+(int)Math.floor(Math.sqrt(area)/30.0)));
            built.put(feature.id(),new Profile(feature.id(),base,floors,floors*3,true));
        }
        profiles=Map.copyOf(built);
    }
    private static int sampleMaximum(SurfaceHeight height,double x,double z,int current){
        try{return Math.max(current,height.surfaceY(x,z));}
        catch(IllegalArgumentException edgeSample){return current;}
    }
    public Profile require(String id){var profile=profiles.get(id);if(profile==null)throw new IllegalArgumentException("Unknown building: "+id);return profile;}
    public int size(){return profiles.size();}
    @FunctionalInterface public interface SurfaceHeight{int surfaceY(double worldX,double worldZ);}
    public record Profile(String id,int baseY,int floors,int wallHeight,boolean heightInferred){
        public Profile{if(id==null||id.isBlank()||floors<1||wallHeight!=floors*3)throw new IllegalArgumentException("Invalid building profile");}
    }
}

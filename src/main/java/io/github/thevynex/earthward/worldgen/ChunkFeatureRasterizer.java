package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import java.util.Arrays;

/** Deterministic 1 m/block rasterization preserving stable OSM building identity. */
public final class ChunkFeatureRasterizer {
    public static final double INFERRED_ROAD_HALF_WIDTH_METRES=1.5;
    private final ChunkGeometryIndex index;
    public ChunkFeatureRasterizer(ChunkGeometryIndex index){if(index==null)throw new IllegalArgumentException("Geometry index required");this.index=index;}
    public ChunkMask rasterize(int chunkX,int chunkZ){
        byte[] cells=new byte[256];String[] buildingIds=new String[256];
        int originX=Math.multiplyExact(chunkX,16),originZ=Math.multiplyExact(chunkZ,16);
        for(var feature:index.featuresForChunk(chunkX,chunkZ)){
            if(feature.layers().contains(GeoGeometry.Layer.ROAD_CENTERLINE))rasterizeRoad(feature,cells,originX,originZ);
            if(feature.layers().contains(GeoGeometry.Layer.BUILDING_OUTLINE))rasterizeBuilding(feature,cells,buildingIds,originX,originZ);
        }
        int roads=0,buildings=0;for(byte cell:cells){if(cell==Surface.ROAD.code)roads++;if(cell==Surface.BUILDING.code)buildings++;}
        return new ChunkMask(chunkX,chunkZ,cells,buildingIds,roads,buildings,true);
    }
    private static void rasterizeRoad(ChunkGeometryIndex.Feature feature,byte[] cells,int originX,int originZ){
        var points=feature.points();for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int offset=z*16+x;if(cells[offset]==Surface.BUILDING.code)continue;double wx=originX+x+0.5,wz=originZ+z+0.5;
            for(int p=1;p<points.size();p++)if(distanceSquared(wx,wz,points.get(p-1),points.get(p))<=INFERRED_ROAD_HALF_WIDTH_METRES*INFERRED_ROAD_HALF_WIDTH_METRES){cells[offset]=Surface.ROAD.code;break;}
        }
    }
    private static void rasterizeBuilding(ChunkGeometryIndex.Feature feature,byte[] cells,String[] ids,int originX,int originZ){
        for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(insidePolygon(originX+x+0.5,originZ+z+0.5,feature)){
            int offset=z*16+x;cells[offset]=Surface.BUILDING.code;ids[offset]=feature.id();
        }
    }
    static boolean insidePolygon(double x,double z,ChunkGeometryIndex.Feature feature){
        boolean inside=false;var points=feature.points();for(int i=0,j=points.size()-1;i<points.size();j=i++){
            var a=points.get(i);var b=points.get(j);boolean crosses=(a.z()>z)!=(b.z()>z)&&x<(b.x()-a.x())*(z-a.z())/(b.z()-a.z())+a.x();if(crosses)inside=!inside;
        }return inside;
    }
    private static double distanceSquared(double x,double z,ChunkGeometryIndex.Point a,ChunkGeometryIndex.Point b){
        double dx=b.x()-a.x(),dz=b.z()-a.z(),length=dx*dx+dz*dz;if(length==0)return(x-a.x())*(x-a.x())+(z-a.z())*(z-a.z());
        double t=Math.max(0,Math.min(1,((x-a.x())*dx+(z-a.z())*dz)/length));double cx=a.x()+t*dx,cz=a.z()+t*dz;return(x-cx)*(x-cx)+(z-cz)*(z-cz);
    }
    public enum Surface{NONE((byte)0),ROAD((byte)1),BUILDING((byte)2);private final byte code;Surface(byte code){this.code=code;}static Surface from(byte code){return switch(code){case 1->ROAD;case 2->BUILDING;default->NONE;};}}
    public record ChunkMask(int chunkX,int chunkZ,byte[] cells,String[] buildingIds,int roadBlocks,int buildingBlocks,boolean roadWidthInferred){
        public ChunkMask{if(cells.length!=256||buildingIds.length!=256||roadBlocks<0||buildingBlocks<0||roadBlocks+buildingBlocks>256)throw new IllegalArgumentException("Invalid raster mask");cells=Arrays.copyOf(cells,cells.length);buildingIds=Arrays.copyOf(buildingIds,buildingIds.length);}
        @Override public byte[] cells(){return Arrays.copyOf(cells,cells.length);}@Override public String[] buildingIds(){return Arrays.copyOf(buildingIds,buildingIds.length);}
        public Surface surfaceAt(int x,int z){if(x<0||x>=16||z<0||z>=16)throw new IndexOutOfBoundsException();return Surface.from(cells[z*16+x]);}
        public String buildingIdAt(int x,int z){if(x<0||x>=16||z<0||z>=16)throw new IndexOutOfBoundsException();return buildingIds[z*16+x];}
    }
}

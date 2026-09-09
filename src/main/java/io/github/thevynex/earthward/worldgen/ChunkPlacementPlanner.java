package io.github.thevynex.earthward.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure placement plan with stable cross-chunk profiles and a local fallback for tests. */
public final class ChunkPlacementPlanner {
    public static final int INFERRED_WALL_HEIGHT=9;
    private final ChunkFeatureRasterizer rasterizer;
    private final BuildingProfileIndex profiles;
    public ChunkPlacementPlanner(ChunkFeatureRasterizer rasterizer){this(rasterizer,null);}
    public ChunkPlacementPlanner(ChunkFeatureRasterizer rasterizer,BuildingProfileIndex profiles){
        if(rasterizer==null)throw new IllegalArgumentException("Rasterizer required");this.rasterizer=rasterizer;this.profiles=profiles;
    }
    public Plan plan(int chunkX,int chunkZ,int[] terrainSurfaceY){
        if(terrainSurfaceY==null||terrainSurfaceY.length!=256)throw new IllegalArgumentException("Terrain surface requires 256 columns");
        Map<Long,ChunkFeatureRasterizer.ChunkMask> masks=new HashMap<>();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){int x=chunkX+dx,z=chunkZ+dz;masks.put(key(x,z),rasterizer.rasterize(x,z));}
        var center=masks.get(key(chunkX,chunkZ));int[] localBases=buildingBases(center,terrainSurfaceY);var placements=new ArrayList<Placement>();
        int originX=Math.multiplyExact(chunkX,16),originZ=Math.multiplyExact(chunkZ,16);
        for(int localZ=0;localZ<16;localZ++)for(int localX=0;localX<16;localX++){
            int index=localZ*16+localX,worldX=originX+localX,worldZ=originZ+localZ,surface=terrainSurfaceY[index];
            var kind=center.surfaceAt(localX,localZ);
            if(kind==ChunkFeatureRasterizer.Surface.ROAD)placements.add(new Placement(worldX,surface,worldZ,Material.ROAD));
            else if(kind==ChunkFeatureRasterizer.Surface.BUILDING){
                String buildingId=center.buildingIdAt(localX,localZ);BuildingProfileIndex.Profile profile=profiles==null?null:profiles.require(buildingId);
                int base=profile==null?localBases[index]:profile.baseY(),wallHeight=profile==null?INFERRED_WALL_HEIGHT:profile.wallHeight();
                for(int y=Math.min(surface,base);y<=base;y++)placements.add(new Placement(worldX,y,worldZ,Material.FOUNDATION));
                if(isBuildingBoundary(worldX,worldZ,masks))for(int y=1;y<=wallHeight;y++){
                    boolean window=(y%3==2||y%3==0)&&windowCell(worldX,worldZ);
                    placements.add(new Placement(worldX,base+y,worldZ,window?Material.WINDOW:Material.WALL));
                }
                placements.add(new Placement(worldX,base+wallHeight+1,worldZ,Material.ROOF));
            }else if(adjacentToRoad(worldX,worldZ,masks)){
                placements.add(new Placement(worldX,surface,worldZ,Material.SIDEWALK));
                if(lampCell(worldX,worldZ)){for(int y=1;y<=3;y++)placements.add(new Placement(worldX,surface+y,worldZ,Material.LAMP_POST));placements.add(new Placement(worldX,surface+4,worldZ,Material.LAMP));}
            }
        }
        return new Plan(chunkX,chunkZ,placements,true,true,true);
    }
    private static int[] buildingBases(ChunkFeatureRasterizer.ChunkMask mask,int[] surfaces){
        int[] result=surfaces.clone();boolean[] visited=new boolean[256];
        for(int start=0;start<256;start++){
            int sx=start%16,sz=start/16;if(visited[start]||mask.surfaceAt(sx,sz)!=ChunkFeatureRasterizer.Surface.BUILDING)continue;
            var queue=new ArrayDeque<Integer>();var cells=new ArrayList<Integer>();queue.add(start);visited[start]=true;int base=surfaces[start];
            while(!queue.isEmpty()){
                int cell=queue.removeFirst(),x=cell%16,z=cell/16;cells.add(cell);base=Math.max(base,surfaces[cell]);
                int[] neighbours={cell-1,cell+1,cell-16,cell+16};for(int next:neighbours){
                    if(next<0||next>=256||visited[next])continue;int nx=next%16,nz=next/16;
                    if(Math.abs(nx-x)+Math.abs(nz-z)!=1||mask.surfaceAt(nx,nz)!=ChunkFeatureRasterizer.Surface.BUILDING)continue;
                    visited[next]=true;queue.add(next);
                }
            }for(int cell:cells)result[cell]=base;
        }return result;
    }
    private static boolean adjacentToRoad(int x,int z,Map<Long,ChunkFeatureRasterizer.ChunkMask> masks){return featureAt(x-1,z,masks)==ChunkFeatureRasterizer.Surface.ROAD||featureAt(x+1,z,masks)==ChunkFeatureRasterizer.Surface.ROAD||featureAt(x,z-1,masks)==ChunkFeatureRasterizer.Surface.ROAD||featureAt(x,z+1,masks)==ChunkFeatureRasterizer.Surface.ROAD;}
    private static boolean isBuildingBoundary(int x,int z,Map<Long,ChunkFeatureRasterizer.ChunkMask> masks){return featureAt(x-1,z,masks)!=ChunkFeatureRasterizer.Surface.BUILDING||featureAt(x+1,z,masks)!=ChunkFeatureRasterizer.Surface.BUILDING||featureAt(x,z-1,masks)!=ChunkFeatureRasterizer.Surface.BUILDING||featureAt(x,z+1,masks)!=ChunkFeatureRasterizer.Surface.BUILDING;}
    private static ChunkFeatureRasterizer.Surface featureAt(int worldX,int worldZ,Map<Long,ChunkFeatureRasterizer.ChunkMask> masks){var mask=masks.get(key(Math.floorDiv(worldX,16),Math.floorDiv(worldZ,16)));return mask==null?ChunkFeatureRasterizer.Surface.NONE:mask.surfaceAt(Math.floorMod(worldX,16),Math.floorMod(worldZ,16));}
    private static boolean windowCell(int x,int z){return Math.floorMod(x+z,3)==0;}
    private static boolean lampCell(int x,int z){return Math.floorMod(x*31+z*17,29)==0;}
    private static long key(int x,int z){return((long)x<<32)^(z&0xffffffffL);}
    public enum Material{ROAD,SIDEWALK,FOUNDATION,WALL,WINDOW,ROOF,LAMP_POST,LAMP}
    public record Placement(int x,int y,int z,Material material){}
    public record Plan(int chunkX,int chunkZ,List<Placement> placements,boolean roadWidthInferred,boolean buildingHeightInferred,boolean facadePatternInferred){public Plan{placements=List.copyOf(placements);}}
}

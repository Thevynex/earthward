package io.github.thevynex.earthward.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure placement plan: asphalt, foundations, inferred three-metre walls, and roofs. */
public final class ChunkPlacementPlanner {
    private final ChunkFeatureRasterizer rasterizer;

    public ChunkPlacementPlanner(ChunkFeatureRasterizer rasterizer) {
        if (rasterizer == null) throw new IllegalArgumentException("Rasterizer required");
        this.rasterizer = rasterizer;
    }

    public Plan plan(int chunkX, int chunkZ, int[] terrainSurfaceY) {
        if (terrainSurfaceY == null || terrainSurfaceY.length != 256) {
            throw new IllegalArgumentException("Terrain surface requires 256 columns");
        }
        Map<Long, ChunkFeatureRasterizer.ChunkMask> masks = new HashMap<>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = chunkX + dx, z = chunkZ + dz;
            masks.put(key(x, z), rasterizer.rasterize(x, z));
        }
        var center = masks.get(key(chunkX, chunkZ));
        var placements = new ArrayList<Placement>();
        int originX = Math.multiplyExact(chunkX, 16), originZ = Math.multiplyExact(chunkZ, 16);
        for (int localZ = 0; localZ < 16; localZ++) for (int localX = 0; localX < 16; localX++) {
            int index = localZ * 16 + localX;
            int worldX = originX + localX, worldZ = originZ + localZ, surface = terrainSurfaceY[index];
            var kind = center.surfaceAt(localX, localZ);
            if (kind == ChunkFeatureRasterizer.Surface.ROAD) {
                placements.add(new Placement(worldX, surface, worldZ, Material.ROAD));
            } else if (kind == ChunkFeatureRasterizer.Surface.BUILDING) {
                placements.add(new Placement(worldX, surface, worldZ, Material.FOUNDATION));
                if (isBoundary(worldX, worldZ, masks)) {
                    for (int y = 1; y <= 3; y++) placements.add(new Placement(worldX, surface + y, worldZ, Material.WALL));
                }
                placements.add(new Placement(worldX, surface + 4, worldZ, Material.ROOF));
            }
        }
        return new Plan(chunkX, chunkZ, placements, true, true);
    }

    private static boolean isBoundary(int worldX, int worldZ,
                                      Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        return !buildingAt(worldX - 1, worldZ, masks) || !buildingAt(worldX + 1, worldZ, masks)
                || !buildingAt(worldX, worldZ - 1, masks) || !buildingAt(worldX, worldZ + 1, masks);
    }

    private static boolean buildingAt(int worldX, int worldZ,
                                      Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        int chunkX = Math.floorDiv(worldX, 16), chunkZ = Math.floorDiv(worldZ, 16);
        var mask = masks.get(key(chunkX, chunkZ));
        return mask != null && mask.surfaceAt(Math.floorMod(worldX, 16), Math.floorMod(worldZ, 16))
                == ChunkFeatureRasterizer.Surface.BUILDING;
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    public enum Material { ROAD, FOUNDATION, WALL, ROOF }
    public record Placement(int x, int y, int z, Material material) {}
    public record Plan(int chunkX, int chunkZ, List<Placement> placements,
                       boolean roadWidthInferred, boolean buildingHeightInferred) {
        public Plan { placements = List.copyOf(placements); }
    }
}

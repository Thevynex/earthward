package io.github.thevynex.earthward.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure placement plan for roads, sidewalks, lamps and inferred functional building shells. */
public final class ChunkPlacementPlanner {
    private final ChunkFeatureRasterizer rasterizer;

    public ChunkPlacementPlanner(ChunkFeatureRasterizer rasterizer) {
        if (rasterizer == null) throw new IllegalArgumentException("Rasterizer required");
        this.rasterizer = rasterizer;
    }

    public Plan plan(int chunkX, int chunkZ, int[] terrainSurfaceY) {
        if (terrainSurfaceY == null || terrainSurfaceY.length != 256) throw new IllegalArgumentException("Terrain surface requires 256 columns");
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
                if (isBuildingBoundary(worldX, worldZ, masks)) {
                    for (int y = 1; y <= 3; y++) {
                        Material material = y == 2 && windowCell(worldX, worldZ) ? Material.WINDOW : Material.WALL;
                        placements.add(new Placement(worldX, surface + y, worldZ, material));
                    }
                }
                placements.add(new Placement(worldX, surface + 4, worldZ, Material.ROOF));
            } else if (adjacentToRoad(worldX, worldZ, masks)) {
                placements.add(new Placement(worldX, surface, worldZ, Material.SIDEWALK));
                if (lampCell(worldX, worldZ)) {
                    for (int y = 1; y <= 3; y++) placements.add(new Placement(worldX, surface + y, worldZ, Material.LAMP_POST));
                    placements.add(new Placement(worldX, surface + 4, worldZ, Material.LAMP));
                }
            }
        }
        return new Plan(chunkX, chunkZ, placements, true, true, true);
    }

    private static boolean adjacentToRoad(int x, int z, Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        return featureAt(x - 1, z, masks) == ChunkFeatureRasterizer.Surface.ROAD
                || featureAt(x + 1, z, masks) == ChunkFeatureRasterizer.Surface.ROAD
                || featureAt(x, z - 1, masks) == ChunkFeatureRasterizer.Surface.ROAD
                || featureAt(x, z + 1, masks) == ChunkFeatureRasterizer.Surface.ROAD;
    }

    private static boolean isBuildingBoundary(int x, int z, Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        return featureAt(x - 1, z, masks) != ChunkFeatureRasterizer.Surface.BUILDING
                || featureAt(x + 1, z, masks) != ChunkFeatureRasterizer.Surface.BUILDING
                || featureAt(x, z - 1, masks) != ChunkFeatureRasterizer.Surface.BUILDING
                || featureAt(x, z + 1, masks) != ChunkFeatureRasterizer.Surface.BUILDING;
    }

    private static ChunkFeatureRasterizer.Surface featureAt(int worldX, int worldZ,
                                                              Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        int chunkX = Math.floorDiv(worldX, 16), chunkZ = Math.floorDiv(worldZ, 16);
        var mask = masks.get(key(chunkX, chunkZ));
        return mask == null ? ChunkFeatureRasterizer.Surface.NONE
                : mask.surfaceAt(Math.floorMod(worldX, 16), Math.floorMod(worldZ, 16));
    }

    private static boolean windowCell(int x, int z) { return Math.floorMod(x + z, 3) == 0; }
    private static boolean lampCell(int x, int z) { return Math.floorMod(x * 31 + z * 17, 29) == 0; }
    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    public enum Material { ROAD, SIDEWALK, FOUNDATION, WALL, WINDOW, ROOF, LAMP_POST, LAMP }
    public record Placement(int x, int y, int z, Material material) {}
    public record Plan(int chunkX, int chunkZ, List<Placement> placements,
                       boolean roadWidthInferred, boolean buildingHeightInferred, boolean facadePatternInferred) {
        public Plan { placements = List.copyOf(placements); }
    }
}

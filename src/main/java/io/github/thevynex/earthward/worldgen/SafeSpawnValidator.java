package io.github.thevynex.earthward.worldgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Finds a bounded nearby spawn with low slope and a clear 3x3 feature footprint. */
public final class SafeSpawnValidator {
    public static final int MAX_RADIUS_METRES = 256;
    private final ChunkFeatureRasterizer rasterizer;
    private final SurfaceHeightProvider heights;

    public SafeSpawnValidator(ChunkFeatureRasterizer rasterizer, SurfaceHeightProvider heights) {
        if (rasterizer == null || heights == null) throw new IllegalArgumentException("Spawn inputs required");
        this.rasterizer = rasterizer;
        this.heights = heights;
    }

    public Optional<SafeSpawn> findNearest(int requestedX, int requestedZ, int maximumRadius) {
        if (maximumRadius < 0 || maximumRadius > MAX_RADIUS_METRES) {
            throw new IllegalArgumentException("Spawn search radius outside limit");
        }
        Map<Long, ChunkFeatureRasterizer.ChunkMask> masks = new HashMap<>();
        int examined = 0;
        for (int radius = 0; radius <= maximumRadius; radius++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = Math.addExact(requestedX, dx), z = Math.addExact(requestedZ, dz);
                    examined++;
                    Integer surface = safeSurface(x, z, masks);
                    if (surface != null) return Optional.of(new SafeSpawn(x, surface + 1, z, examined, radius));
                }
            }
        }
        return Optional.empty();
    }

    private Integer safeSurface(int x, int z, Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        int minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int candidateX = Math.addExact(x, dx), candidateZ = Math.addExact(z, dz);
            if (featureAt(candidateX, candidateZ, masks) != ChunkFeatureRasterizer.Surface.NONE) return null;
            int y;
            try { y = heights.surfaceY(candidateX, candidateZ); }
            catch (RuntimeException outsideCoverage) { return null; }
            minimum = Math.min(minimum, y); maximum = Math.max(maximum, y);
        }
        int center = heights.surfaceY(x, z);
        if (maximum - minimum > 1 || center < -62 || center > 316) return null;
        return center;
    }

    private ChunkFeatureRasterizer.Surface featureAt(int worldX, int worldZ,
                                                      Map<Long, ChunkFeatureRasterizer.ChunkMask> masks) {
        int chunkX = Math.floorDiv(worldX, 16), chunkZ = Math.floorDiv(worldZ, 16);
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        var mask = masks.computeIfAbsent(key, ignored -> rasterizer.rasterize(chunkX, chunkZ));
        return mask.surfaceAt(Math.floorMod(worldX, 16), Math.floorMod(worldZ, 16));
    }

    @FunctionalInterface public interface SurfaceHeightProvider { int surfaceY(int worldX, int worldZ); }
    public record SafeSpawn(int x, int y, int z, int examinedCandidates, int distanceMetres) {}
}

package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ElevationGrid;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import java.util.Arrays;

/** Pure 16x16 terrain-column preparation; safe to run off the Minecraft world thread. */
public final class PilotTerrainSampler {
    public static final int CHUNK_SIZE = 16;
    public static final int SEA_LEVEL_Y = 63;
    private final ElevationGrid elevation;
    private final LocalMetricProjection projection;

    public PilotTerrainSampler(ElevationGrid elevation, LocalMetricProjection projection) {
        if (elevation == null || projection == null) throw new IllegalArgumentException("Terrain inputs required");
        this.elevation = elevation;
        this.projection = projection;
    }

    public ChunkHeights sampleChunk(int chunkX, int chunkZ) {
        int[] surfaces = new int[CHUNK_SIZE * CHUNK_SIZE];
        double[] sourceMetres = new double[surfaces.length];
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int localZ = 0; localZ < CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int index = localZ * CHUNK_SIZE + localX;
                double worldX = Math.addExact(Math.multiplyExact(chunkX, CHUNK_SIZE), localX) + 0.5;
                double worldZ = Math.addExact(Math.multiplyExact(chunkZ, CHUNK_SIZE), localZ) + 0.5;
                var geographic = projection.toGeographic(worldX, worldZ);
                double metres = elevation.sampleMetres(geographic.longitude(), geographic.latitude());
                int surface = Math.addExact(SEA_LEVEL_Y, (int) Math.round(metres));
                sourceMetres[index] = metres;
                surfaces[index] = surface;
                minimum = Math.min(minimum, surface);
                maximum = Math.max(maximum, surface);
            }
        }
        return new ChunkHeights(chunkX, chunkZ, surfaces, sourceMetres, minimum, maximum);
    }

    public record ChunkHeights(int chunkX, int chunkZ, int[] surfaceY, double[] sourceElevationMetres,
                               int minimumSurfaceY, int maximumSurfaceY) {
        public ChunkHeights {
            if (surfaceY.length != 256 || sourceElevationMetres.length != 256) {
                throw new IllegalArgumentException("Chunk height arrays must contain 256 columns");
            }
            surfaceY = Arrays.copyOf(surfaceY, surfaceY.length);
            sourceElevationMetres = Arrays.copyOf(sourceElevationMetres, sourceElevationMetres.length);
        }
        @Override public int[] surfaceY() { return Arrays.copyOf(surfaceY, surfaceY.length); }
        @Override public double[] sourceElevationMetres() {
            return Arrays.copyOf(sourceElevationMetres, sourceElevationMetres.length);
        }
        public int surfaceY(int localX, int localZ) {
            if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16) throw new IndexOutOfBoundsException();
            return surfaceY[localZ * 16 + localX];
        }
    }
}

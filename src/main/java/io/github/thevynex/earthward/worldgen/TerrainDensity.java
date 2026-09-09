package io.github.thevynex.earthward.worldgen;

/** Pure density contract shared by the Minecraft adapter and plain JVM tests. */
public final class TerrainDensity {
    private TerrainDensity() {}

    public static double at(double sourceElevationMetres, int blockY) {
        if (!Double.isFinite(sourceElevationMetres) || sourceElevationMetres < -500 || sourceElevationMetres > 9000) {
            throw new IllegalArgumentException("Invalid source elevation");
        }
        double surfaceY = PilotTerrainSampler.SEA_LEVEL_Y + sourceElevationMetres;
        return Math.max(-200.0, Math.min(200.0, (surfaceY - blockY) * 0.35));
    }
}

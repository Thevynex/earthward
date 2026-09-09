package io.github.thevynex.earthward.worldgen;

import com.mojang.serialization.MapCodec;
import io.github.thevynex.earthward.geo.ElevationGrid;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Solid below verified translated DSM height, air above; no terrain noise or scale compression. */
final class PilotDensityFunction implements DensityFunction {
    private final ElevationGrid elevation;
    private final LocalMetricProjection projection;

    PilotDensityFunction(ElevationGrid elevation, LocalMetricProjection projection) {
        this.elevation = elevation;
        this.projection = projection;
    }

    double densityAt(int x, int y, int z) {
        try {
            var point = projection.toGeographic(x + 0.5, z + 0.5);
            double metres = elevation.sampleMetres(point.longitude(), point.latitude());
            double surfaceY = PilotTerrainSampler.SEA_LEVEL_Y + metres;
            return Math.max(-200.0, Math.min(200.0, (surfaceY - y) * 0.35));
        } catch (IllegalArgumentException outsidePackage) {
            return -200.0;
        }
    }

    @Override public double compute(FunctionContext context) {
        return densityAt(context.blockX(), context.blockY(), context.blockZ());
    }
    @Override public void fillArray(double[] array, ContextProvider provider) {
        for (int index = 0; index < array.length; index++) array[index] = compute(provider.forIndex(index));
    }
    @Override public DensityFunction mapAll(Visitor visitor) { return this; }
    @Override public double minValue() { return -200.0; }
    @Override public double maxValue() { return 200.0; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}

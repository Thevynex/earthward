package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ElevationGrid;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PilotTerrainSamplerTest {
    private static ElevationGrid flat(double metres) {
        return ElevationGrid.parse("""
                {"schema_version":1,"horizontal_crs":"EPSG:4326","vertical_reference":"EGM2008_geoid_EPSG_3855",
                 "units":"metres","row_order":"north_to_south","column_order":"west_to_east","rows":2,"columns":2,
                 "sample_bounds_west_south_east_north":[28.99,40.99,29.01,41.01],
                 "pixel_size_degrees":[0.01,0.01],"elevations_metres":[[%1$f,%1$f],[%1$f,%1$f]]}
                """.formatted(metres));
    }

    @Test void keepsOneVerticalBlockPerSourceMetreWithTranslationOnly() {
        var sampler = new PilotTerrainSampler(flat(27), new LocalMetricProjection(41, 29));
        var chunk = sampler.sampleChunk(0, 0);
        assertEquals(90, chunk.minimumSurfaceY());
        assertEquals(90, chunk.maximumSurfaceY());
        assertEquals(90, chunk.surfaceY(15, 15));
        assertEquals(27, chunk.sourceElevationMetres()[0]);
    }

    @Test void chunkArraysAreDefensivelyCopied() {
        var sampler = new PilotTerrainSampler(flat(1), new LocalMetricProjection(41, 29));
        var chunk = sampler.sampleChunk(0, 0);
        int[] copy = chunk.surfaceY();
        copy[0] = -999;
        assertEquals(64, chunk.surfaceY(0, 0));
    }

    @Test void rejectsChunksOutsideInstalledElevationCoverage() {
        var sampler = new PilotTerrainSampler(flat(0), new LocalMetricProjection(41, 29));
        assertThrows(IllegalArgumentException.class, () -> sampler.sampleChunk(10000, 10000));
    }
}

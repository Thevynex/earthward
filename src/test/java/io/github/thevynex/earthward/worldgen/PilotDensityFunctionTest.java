package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ElevationGrid;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PilotDensityFunctionTest {
    private static PilotDensityFunction density() {
        var grid = ElevationGrid.parse("""
                {"schema_version":1,"horizontal_crs":"EPSG:4326","vertical_reference":"EGM2008_geoid_EPSG_3855",
                 "units":"metres","row_order":"north_to_south","column_order":"west_to_east","rows":2,"columns":2,
                 "sample_bounds_west_south_east_north":[28.99,40.99,29.01,41.01],
                 "pixel_size_degrees":[0.01,0.01],"elevations_metres":[[10,10],[10,10]]}
                """);
        return new PilotDensityFunction(grid, new LocalMetricProjection(41, 29));
    }

    @Test void isSolidBelowTranslatedRealHeightAndAirAbove() {
        var density = density();
        assertTrue(density.densityAt(0, 70, 0) > 0);
        assertEquals(0, density.densityAt(0, 73, 0), 0.18);
        assertTrue(density.densityAt(0, 80, 0) < 0);
    }

    @Test void outsideInstalledPackageIsVoidNotInventedTerrain() {
        assertEquals(-200, density().densityAt(1_000_000, 64, 1_000_000));
    }
}

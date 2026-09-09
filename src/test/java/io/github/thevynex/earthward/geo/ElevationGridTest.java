package io.github.thevynex.earthward.geo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ElevationGridTest {
    private static final String GRID = """
            {"schema_version":1,"horizontal_crs":"EPSG:4326","vertical_reference":"EGM2008_geoid_EPSG_3855",
             "units":"metres","row_order":"north_to_south","column_order":"west_to_east",
             "rows":2,"columns":2,"sample_bounds_west_south_east_north":[29,40,30,41],
             "pixel_size_degrees":[0.5,0.5],"elevations_metres":[[10,20],[30,40]]}
            """;

    @Test void parsesAndBilinearlySamplesWithoutVerticalCompression() {
        var grid = ElevationGrid.parse(GRID);
        assertEquals(10, grid.sampleMetres(29, 41), 1e-9);
        assertEquals(40, grid.sampleMetres(30, 40), 1e-9);
        assertEquals(25, grid.sampleMetres(29.5, 40.5), 1e-9);
    }

    @Test void rejectsOutsideAndUnsupportedVerticalReference() {
        var grid = ElevationGrid.parse(GRID);
        assertThrows(IllegalArgumentException.class, () -> grid.sampleMetres(31, 40.5));
        assertThrows(IllegalArgumentException.class, () -> ElevationGrid.parse(GRID.replace("EGM2008_geoid_EPSG_3855", "unknown")));
    }

    @Test void parsedCollectionsAreImmutable() {
        var grid = ElevationGrid.parse(GRID);
        assertThrows(UnsupportedOperationException.class, () -> grid.elevationsMetres().clear());
        assertThrows(UnsupportedOperationException.class, () -> grid.elevationsMetres().getFirst().clear());
    }
}

package io.github.thevynex.earthward.geo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class LocalMetricProjectionTest {
    private static final LocalMetricProjection PILOT = new LocalMetricProjection(40.9848, 29.0268);

    @Test void originIsZero() {
        var point = PILOT.project(40.9848, 29.0268);
        assertEquals(0, point.eastMetres(), 1e-9);
        assertEquals(0, point.northMetres(), 1e-9);
    }
    @Test void approximatelyOneKilometreNorth() {
        assertEquals(1000, PILOT.project(40.9937932, 29.0268).northMetres(), 2);
    }
    @Test void worldZPointsSouth() {
        assertTrue(PILOT.toWorld(40.99, 29.0268).zMetres() < 0);
    }
    @Test void eastIsPositiveX() {
        assertTrue(PILOT.toWorld(40.9848, 29.03).xMetres() > 0);
    }
    @Test void inverseRoundTripsPilotCoordinates() {
        var world = PILOT.toWorld(40.9812, 29.0311);
        var geographic = PILOT.toGeographic(world.xMetres(), world.zMetres());
        assertEquals(40.9812, geographic.latitude(), 1e-10);
        assertEquals(29.0311, geographic.longitude(), 1e-10);
    }
    @Test void rejectsNonFiniteAndDistantCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> PILOT.project(Double.NaN, 29));
        assertThrows(IllegalArgumentException.class, () -> PILOT.project(50, 29));
        assertThrows(IllegalArgumentException.class, () -> PILOT.toGeographic(Double.POSITIVE_INFINITY, 0));
    }
    @Test void rejectsPolarOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new LocalMetricProjection(89, 0));
    }
}

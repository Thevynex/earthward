package io.github.thevynex.earthward.worldgen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PilotDensityFunctionTest {
    @Test void isSolidBelowTranslatedRealHeightAndAirAbove() {
        assertTrue(TerrainDensity.at(10, 70) > 0);
        assertEquals(0, TerrainDensity.at(10, 73), 1e-9);
        assertTrue(TerrainDensity.at(10, 80) < 0);
    }

    @Test void clampsDensityAndRejectsInvalidSourceValues() {
        assertEquals(200, TerrainDensity.at(9000, -64));
        assertEquals(-200, TerrainDensity.at(-500, 320));
        assertThrows(IllegalArgumentException.class, () -> TerrainDensity.at(Double.NaN, 64));
    }
}

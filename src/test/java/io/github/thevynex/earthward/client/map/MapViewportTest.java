package io.github.thevynex.earthward.client.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MapViewportTest {
    @Test void fitCentersBoundsAndKeepsThemVisible() {
        var view = new MapViewport();
        view.fit(-500, -400, 500, 400, 1000, 800, 50);
        assertEquals(0, view.centerX(), 1e-9);
        assertEquals(0, view.centerZ(), 1e-9);
        assertTrue(view.worldToScreenX(-500, 1000) >= 0);
        assertTrue(view.worldToScreenY(400, 800) <= 800);
    }

    @Test void panTracksHandDirection() {
        var view = new MapViewport();
        view.fit(-100, -100, 100, 100, 400, 400, 20);
        double before = view.centerX();
        view.panPixels(20, 0);
        assertTrue(view.centerX() < before);
    }

    @Test void cursorAnchoredZoomKeepsWorldPointStable() {
        var view = new MapViewport();
        view.fit(-100, -100, 100, 100, 400, 400, 20);
        double x = view.screenToWorldX(300, 400);
        double z = view.screenToWorldZ(250, 400);
        view.zoomAt(2, 300, 250, 400, 400);
        assertEquals(x, view.screenToWorldX(300, 400), 1e-9);
        assertEquals(z, view.screenToWorldZ(250, 400), 1e-9);
    }

    @Test void zoomIsBounded() {
        var view = new MapViewport();
        view.zoomAt(1e9, 100, 100, 200, 200);
        assertEquals(MapViewport.MAX_SCALE, view.pixelsPerMetre());
        view.zoomAt(1e-20, 100, 100, 200, 200);
        assertEquals(MapViewport.MIN_SCALE, view.pixelsPerMetre());
    }

    @Test void rejectsInvalidFit() {
        var view = new MapViewport();
        assertThrows(IllegalArgumentException.class, () -> view.fit(1, 0, -1, 2, 100, 100, 10));
    }
}

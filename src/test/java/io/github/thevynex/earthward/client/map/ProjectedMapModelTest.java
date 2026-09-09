package io.github.thevynex.earthward.client.map;

import io.github.thevynex.earthward.geo.GeoGeometry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ProjectedMapModelTest {
    @Test void projectsPilotGeometryToMetresWithoutChangingCounts() {
        var source = GeoGeometry.parse("""
                {"schema_version":1,"crs":"EPSG:4326","features":[
                {"id":"osm:way:1","layers":["road_centerline"],"coordinates_lon_lat":[[29,41],[29.001,41.001]]}]}
                """);
        var projected = ProjectedMapModel.from(source);
        assertEquals(1, projected.roadCount());
        assertEquals(2, projected.pointCount());
        assertTrue(projected.bounds().east() - projected.bounds().west() > 80);
        assertTrue(projected.bounds().south() - projected.bounds().north() > 100);
        assertThrows(UnsupportedOperationException.class, () -> projected.features().clear());
    }

    @Test void boundsIntersectionRejectsDistantViewport() {
        var bounds = new ProjectedMapModel.Bounds(-10, -10, 10, 10);
        assertTrue(bounds.intersects(-5, -5, 5, 5));
        assertFalse(bounds.intersects(20, 20, 30, 30));
    }
}

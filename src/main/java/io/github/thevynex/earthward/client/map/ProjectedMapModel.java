package io.github.thevynex.earthward.client.map;

import io.github.thevynex.earthward.geo.GeoGeometry;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Immutable pilot geometry projected to Minecraft horizontal metres: +X east, +Z south. */
public record ProjectedMapModel(List<Feature> features, Bounds bounds,
                                int buildingCount, int roadCount, int pointCount) {
    public ProjectedMapModel {
        features = List.copyOf(features);
        if (features.isEmpty() || pointCount <= 0) throw new IllegalArgumentException("Empty projected map");
    }

    public record Point(double x, double z) {}
    public record Feature(String id, Set<GeoGeometry.Layer> layers, List<Point> points, Bounds bounds) {
        public Feature {
            layers = Set.copyOf(layers);
            points = List.copyOf(points);
        }
    }
    public record Bounds(double west, double north, double east, double south) {
        public boolean intersects(double otherWest, double otherNorth, double otherEast, double otherSouth) {
            return east >= otherWest && west <= otherEast && south >= otherNorth && north <= otherSouth;
        }
    }

    public static ProjectedMapModel from(GeoGeometry geometry) {
        var geographic = geometry.bounds();
        double originLatitude = (geographic.south() + geographic.north()) / 2.0;
        double originLongitude = (geographic.west() + geographic.east()) / 2.0;
        var projection = new LocalMetricProjection(originLatitude, originLongitude);
        var output = new ArrayList<Feature>(geometry.features().size());
        double west = Double.POSITIVE_INFINITY, north = Double.POSITIVE_INFINITY;
        double east = Double.NEGATIVE_INFINITY, south = Double.NEGATIVE_INFINITY;
        for (var feature : geometry.features()) {
            var points = new ArrayList<Point>(feature.points().size());
            double featureWest = Double.POSITIVE_INFINITY, featureNorth = Double.POSITIVE_INFINITY;
            double featureEast = Double.NEGATIVE_INFINITY, featureSouth = Double.NEGATIVE_INFINITY;
            for (var point : feature.points()) {
                var world = projection.toWorld(point.latitude(), point.longitude());
                var projected = new Point(world.xMetres(), world.zMetres());
                points.add(projected);
                featureWest = Math.min(featureWest, projected.x());
                featureEast = Math.max(featureEast, projected.x());
                featureNorth = Math.min(featureNorth, projected.z());
                featureSouth = Math.max(featureSouth, projected.z());
            }
            var bounds = new Bounds(featureWest, featureNorth, featureEast, featureSouth);
            output.add(new Feature(feature.id(), feature.layers(), points, bounds));
            west = Math.min(west, featureWest); east = Math.max(east, featureEast);
            north = Math.min(north, featureNorth); south = Math.max(south, featureSouth);
        }
        return new ProjectedMapModel(output, new Bounds(west, north, east, south),
                geometry.buildingCount(), geometry.roadCount(), geometry.pointCount());
    }
}

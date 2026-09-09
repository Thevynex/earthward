package io.github.thevynex.earthward.geo;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable, validated geometry held only after package hash verification. */
public record GeoGeometry(List<Feature> features, int buildingCount, int roadCount,
                          int pointCount, Bounds bounds) {
    public static final int MAX_FEATURES = 20_000;
    public static final int MAX_POINTS = 200_000;
    private static final Pattern FEATURE_ID = Pattern.compile("osm:way:[1-9][0-9]*");

    public GeoGeometry {
        features = List.copyOf(features);
        if (features.isEmpty() || features.size() > MAX_FEATURES || pointCount <= 0 || pointCount > MAX_POINTS) {
            throw new IllegalArgumentException("Invalid geometry totals");
        }
    }

    public enum Layer { BUILDING_OUTLINE, ROAD_CENTERLINE }
    public record Point(double longitude, double latitude) {
        public Point {
            if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                    || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("Invalid geographic point");
            }
        }
    }
    public record Feature(String id, Set<Layer> layers, List<Point> points) {
        public Feature {
            if (id == null || !FEATURE_ID.matcher(id).matches()) throw new IllegalArgumentException("Invalid feature id");
            layers = Set.copyOf(layers);
            points = List.copyOf(points);
            if (layers.isEmpty()) throw new IllegalArgumentException("Feature has no supported layer");
            if (layers.contains(Layer.ROAD_CENTERLINE) && points.size() < 2) {
                throw new IllegalArgumentException("Road needs at least two points");
            }
            if (layers.contains(Layer.BUILDING_OUTLINE)) {
                if (points.size() < 4 || !points.getFirst().equals(points.getLast())
                        || new HashSet<>(points.subList(0, points.size() - 1)).size() < 3) {
                    throw new IllegalArgumentException("Building outline is not closed");
                }
            }
        }
    }
    public record Bounds(double west, double south, double east, double north) {
        public Bounds {
            if (!Double.isFinite(west) || !Double.isFinite(south)
                    || !Double.isFinite(east) || !Double.isFinite(north)
                    || west > east || south > north) {
                throw new IllegalArgumentException("Invalid geometry bounds");
            }
        }
    }

    public static GeoGeometry parse(String json) {
        var root = JsonParser.parseString(json).getAsJsonObject();
        if (root.get("schema_version").getAsInt() != 1 || !"EPSG:4326".equals(root.get("crs").getAsString())) {
            throw new IllegalArgumentException("Unsupported geometry schema or CRS");
        }
        var input = root.getAsJsonArray("features");
        if (input.isEmpty() || input.size() > MAX_FEATURES) throw new IllegalArgumentException("Invalid feature count");
        var features = new ArrayList<Feature>(input.size());
        var ids = new HashSet<String>();
        int buildings = 0;
        int roads = 0;
        int totalPoints = 0;
        double west = Double.POSITIVE_INFINITY, south = Double.POSITIVE_INFINITY;
        double east = Double.NEGATIVE_INFINITY, north = Double.NEGATIVE_INFINITY;
        for (var value : input) {
            var item = value.getAsJsonObject();
            String id = item.get("id").getAsString();
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate feature id");
            var layers = new HashSet<Layer>();
            for (var layerValue : item.getAsJsonArray("layers")) {
                Layer layer = switch (layerValue.getAsString()) {
                    case "building_outline" -> Layer.BUILDING_OUTLINE;
                    case "road_centerline" -> Layer.ROAD_CENTERLINE;
                    default -> throw new IllegalArgumentException("Unsupported geometry layer");
                };
                if (!layers.add(layer)) throw new IllegalArgumentException("Duplicate geometry layer");
            }
            var points = new ArrayList<Point>();
            for (var coordinateValue : item.getAsJsonArray("coordinates_lon_lat")) {
                var coordinate = coordinateValue.getAsJsonArray();
                if (coordinate.size() != 2) throw new IllegalArgumentException("Coordinate must contain longitude and latitude");
                var point = new Point(coordinate.get(0).getAsDouble(), coordinate.get(1).getAsDouble());
                points.add(point);
                west = Math.min(west, point.longitude()); east = Math.max(east, point.longitude());
                south = Math.min(south, point.latitude()); north = Math.max(north, point.latitude());
                if (++totalPoints > MAX_POINTS) throw new IllegalArgumentException("Point budget exceeded");
            }
            var feature = new Feature(id, layers, points);
            features.add(feature);
            if (layers.contains(Layer.BUILDING_OUTLINE)) buildings++;
            if (layers.contains(Layer.ROAD_CENTERLINE)) roads++;
        }
        return new GeoGeometry(features, buildings, roads, totalPoints, new Bounds(west, south, east, north));
    }
}

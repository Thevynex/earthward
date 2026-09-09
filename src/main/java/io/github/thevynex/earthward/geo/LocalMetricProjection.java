package io.github.thevynex.earthward.geo;

/** Pilot-only local tangent approximation. It is not the worldwide projection. */
public record LocalMetricProjection(double originLatitude, double originLongitude) {
    private static final double EARTH_RADIUS_METRES = 6_371_008.8;

    public LocalMetricProjection {
        if (!Double.isFinite(originLatitude) || !Double.isFinite(originLongitude)
                || originLatitude < -85 || originLatitude > 85
                || originLongitude < -180 || originLongitude >= 180) {
            throw new IllegalArgumentException("Invalid local projection origin");
        }
    }

    public MetricPoint project(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude >= 180) {
            throw new IllegalArgumentException("Invalid coordinate");
        }
        if (Math.abs(latitude - originLatitude) > 2 || Math.abs(longitude - originLongitude) > 2) {
            throw new IllegalArgumentException("Coordinate outside local projection limit");
        }
        double east = EARTH_RADIUS_METRES * Math.toRadians(longitude - originLongitude)
                * Math.cos(Math.toRadians(originLatitude));
        double north = EARTH_RADIUS_METRES * Math.toRadians(latitude - originLatitude);
        return new MetricPoint(east, north);
    }

    /** Minecraft mapping convention for the pilot: +X east, +Z south. */
    public WorldHorizontalPoint toWorld(double latitude, double longitude) {
        MetricPoint point = project(latitude, longitude);
        return new WorldHorizontalPoint(point.eastMetres(), -point.northMetres());
    }

    public record MetricPoint(double eastMetres, double northMetres) {}
    public record WorldHorizontalPoint(double xMetres, double zMetres) {}
}

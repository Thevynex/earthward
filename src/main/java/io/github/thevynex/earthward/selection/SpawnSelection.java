package io.github.thevynex.earthward.selection;

public record SpawnSelection(String regionPackageId, double latitude, double longitude) {
    public SpawnSelection {
        if (regionPackageId == null || !regionPackageId.matches("[a-z][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("regionPackageId must be a lowercase identifier (1-64 characters)");
        }
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude must be finite and within [-90, 90]");
        }
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude >= 180.0) {
            throw new IllegalArgumentException("longitude must be finite and within [-180, 180)");
        }
    }
}

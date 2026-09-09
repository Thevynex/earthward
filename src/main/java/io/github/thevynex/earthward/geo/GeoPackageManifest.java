package io.github.thevynex.earthward.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.regex.Pattern;

public record GeoPackageManifest(String packageId, String geometrySha256, long geometryBytes,
                                 int targetWorldMetresPerBlock, boolean generationReady) {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    public static final int MAX_MANIFEST_CHARS = 65_536;
    public static final long MAX_GEOMETRY_BYTES = 16L * 1024 * 1024;

    public GeoPackageManifest {
        if (packageId == null || !ID.matcher(packageId).matches()) {
            throw new IllegalArgumentException("Invalid package identifier");
        }
        if (geometrySha256 == null || !SHA256.matcher(geometrySha256).matches()) {
            throw new IllegalArgumentException("Invalid geometry digest");
        }
        if (geometryBytes <= 0 || geometryBytes > MAX_GEOMETRY_BYTES) {
            throw new IllegalArgumentException("Invalid geometry size");
        }
        if (targetWorldMetresPerBlock != 1) {
            throw new IllegalArgumentException("Package scale must be one metre per block");
        }
        if (generationReady) {
            throw new IllegalArgumentException("Schema v1 geometry packages cannot enable generation");
        }
    }

    public static GeoPackageManifest parse(String json, String expectedPackageId) {
        if (json == null || json.length() > MAX_MANIFEST_CHARS) {
            throw new IllegalArgumentException("Manifest missing or too large");
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        requireInt(root, "schema_version", 1);
        requireString(root, "kind", "development_geometry_extract");
        requireString(root, "horizontal_crs", "EPSG:4326");
        requireString(root, "coordinate_order", "longitude_latitude");
        requireString(root, "coordinate_units", "degrees");
        if (root.has("vertical_reference") && !root.get("vertical_reference").isJsonNull()) {
            throw new IllegalArgumentException("Unexpected vertical reference");
        }
        if (root.get("world_projection_implemented").getAsBoolean()) {
            throw new IllegalArgumentException("World projection must remain disabled in schema v1");
        }
        String packageId = root.get("package_id").getAsString();
        if (!packageId.equals(expectedPackageId)) {
            throw new IllegalArgumentException("Package identifier mismatch");
        }
        JsonObject geometry = root.getAsJsonObject("files").getAsJsonObject("geometry.json");
        return new GeoPackageManifest(packageId, geometry.get("sha256").getAsString(),
                geometry.get("bytes").getAsLong(), root.get("target_world_metres_per_block").getAsInt(),
                root.get("generation_ready").getAsBoolean());
    }

    private static void requireInt(JsonObject root, String key, int expected) {
        if (!root.has(key) || root.get(key).getAsInt() != expected) {
            throw new IllegalArgumentException("Unexpected " + key);
        }
    }

    private static void requireString(JsonObject root, String key, String expected) {
        if (!root.has(key) || !expected.equals(root.get(key).getAsString())) {
            throw new IllegalArgumentException("Unexpected " + key);
        }
    }
}

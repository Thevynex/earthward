package io.github.thevynex.earthward.geo;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

final class GeoPackageLoaderTest {
    private static final byte[] VALID_GEOMETRY = """
            {"schema_version":1,"crs":"EPSG:4326","features":[
            {"id":"osm:way:1","layers":["road_centerline"],"coordinates_lon_lat":[[29,41],[29.1,41.1]]}]}
            """.getBytes(StandardCharsets.UTF_8);

    @Test void missingPackageIsReported() throws Exception {
        Path root = Files.createTempDirectory("earthward-missing");
        assertEquals(GeoPackageLoader.Status.MISSING, GeoPackageLoader.inspect(root, "pilot").status());
    }

    @Test void verifiesMatchingGeometryWithoutEnablingGeneration() throws Exception {
        Path root = Files.createTempDirectory("earthward-valid");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        Files.write(packageDir.resolve("geometry.json"), VALID_GEOMETRY);
        Files.writeString(packageDir.resolve("manifest.json"), manifest("pilot", VALID_GEOMETRY, false));
        var result = GeoPackageLoader.inspect(root, "pilot");
        assertEquals(GeoPackageLoader.Status.VERIFIED_GEOMETRY_ONLY, result.status());
        assertFalse(result.manifest().generationReady());
        assertEquals(1, result.geometry().roadCount());
    }

    @Test void rejectsDigestMismatch() throws Exception {
        Path root = Files.createTempDirectory("earthward-digest");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        Files.write(packageDir.resolve("geometry.json"), "changed".getBytes(StandardCharsets.UTF_8));
        Files.writeString(packageDir.resolve("manifest.json"), manifest("pilot", VALID_GEOMETRY, false));
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "pilot").status());
    }

    @Test void rejectsIdentifierMismatchAndGenerationReady() throws Exception {
        Path root = Files.createTempDirectory("earthward-id");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        Files.write(packageDir.resolve("geometry.json"), VALID_GEOMETRY);
        Files.writeString(packageDir.resolve("manifest.json"), manifest("other", VALID_GEOMETRY, true));
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "pilot").status());
    }

    @Test void rejectsTraversalIdentifier() throws Exception {
        Path root = Files.createTempDirectory("earthward-traversal");
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "../saves").status());
    }

    @Test void rejectsHashValidButInvalidGeometrySchema() throws Exception {
        Path root = Files.createTempDirectory("earthward-schema");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        byte[] geometry = "{\"schema_version\":2}".getBytes(StandardCharsets.UTF_8);
        Files.write(packageDir.resolve("geometry.json"), geometry);
        Files.writeString(packageDir.resolve("manifest.json"), manifest("pilot", geometry, false));
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "pilot").status());
    }

    private static String manifest(String packageId, byte[] geometry, boolean generationReady) throws Exception {
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(geometry));
        return """
                {"schema_version":1,"package_id":"%s","kind":"development_geometry_extract",
                 "generation_ready":%s,"horizontal_crs":"EPSG:4326",
                 "coordinate_order":"longitude_latitude","coordinate_units":"degrees",
                 "vertical_reference":null,"target_world_metres_per_block":1,
                 "world_projection_implemented":false,
                 "files":{"geometry.json":{"sha256":"%s","bytes":%d}}}
                """.formatted(packageId, generationReady, digest, geometry.length);
    }
}

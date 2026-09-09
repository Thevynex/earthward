package io.github.thevynex.earthward.geo;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

final class GeoPackageLoaderTest {
    @Test void missingPackageIsReported() throws Exception {
        Path root = Files.createTempDirectory("earthward-missing");
        assertEquals(GeoPackageLoader.Status.MISSING, GeoPackageLoader.inspect(root, "pilot").status());
    }

    @Test void verifiesMatchingGeometryWithoutEnablingGeneration() throws Exception {
        Path root = Files.createTempDirectory("earthward-valid");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        byte[] geometry = "{\"schema_version\":1,\"features\":[]}".getBytes(StandardCharsets.UTF_8);
        Files.write(packageDir.resolve("geometry.json"), geometry);
        Files.writeString(packageDir.resolve("manifest.json"), manifest("pilot", geometry, false));
        var result = GeoPackageLoader.inspect(root, "pilot");
        assertEquals(GeoPackageLoader.Status.VERIFIED_GEOMETRY_ONLY, result.status());
        assertFalse(result.manifest().generationReady());
    }

    @Test void rejectsDigestMismatch() throws Exception {
        Path root = Files.createTempDirectory("earthward-digest");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        byte[] geometry = "original".getBytes(StandardCharsets.UTF_8);
        Files.write(packageDir.resolve("geometry.json"), "changed".getBytes(StandardCharsets.UTF_8));
        Files.writeString(packageDir.resolve("manifest.json"), manifest("pilot", geometry, false));
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "pilot").status());
    }

    @Test void rejectsIdentifierMismatchAndGenerationReady() throws Exception {
        Path root = Files.createTempDirectory("earthward-id");
        Path packageDir = root.resolve("pilot");
        Files.createDirectory(packageDir);
        byte[] geometry = "x".getBytes(StandardCharsets.UTF_8);
        Files.write(packageDir.resolve("geometry.json"), geometry);
        Files.writeString(packageDir.resolve("manifest.json"), manifest("other", geometry, true));
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "pilot").status());
    }

    @Test void rejectsTraversalIdentifier() throws Exception {
        Path root = Files.createTempDirectory("earthward-traversal");
        assertEquals(GeoPackageLoader.Status.INVALID, GeoPackageLoader.inspect(root, "../saves").status());
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

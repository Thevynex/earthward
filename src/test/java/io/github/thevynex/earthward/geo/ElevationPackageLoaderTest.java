package io.github.thevynex.earthward.geo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ElevationPackageLoaderTest {
    private static final byte[] GRID = """
            {"schema_version":1,"horizontal_crs":"EPSG:4326","vertical_reference":"EGM2008_geoid_EPSG_3855",
             "units":"metres","row_order":"north_to_south","column_order":"west_to_east","rows":1,"columns":1,
             "sample_bounds_west_south_east_north":[29,40,30,41],"pixel_size_degrees":[1,1],"elevations_metres":[[17.5]]}
            """.getBytes(StandardCharsets.UTF_8);

    @Test void verifiesHashBeforeParsingSamples() throws Exception {
        var root = Files.createTempDirectory("earthward-elevation");
        var directory = Files.createDirectory(root.resolve("pilot"));
        Files.write(directory.resolve("elevation.json"), GRID);
        Files.writeString(directory.resolve("elevation-manifest.json"), manifest(GRID, false));
        var result = ElevationPackageLoader.inspect(root, "pilot");
        assertEquals(ElevationPackageLoader.Status.VERIFIED, result.status());
        assertEquals(17.5, result.grid().sampleMetres(29.5, 40.5));
    }

    @Test void rejectsChangedGridAndReadyFlag() throws Exception {
        var root = Files.createTempDirectory("earthward-elevation-invalid");
        var directory = Files.createDirectory(root.resolve("pilot"));
        Files.writeString(directory.resolve("elevation.json"), "changed");
        Files.writeString(directory.resolve("elevation-manifest.json"), manifest(GRID, true));
        assertEquals(ElevationPackageLoader.Status.INVALID, ElevationPackageLoader.inspect(root, "pilot").status());
    }

    @Test void reportsMissingWithoutCreatingDirectories() throws Exception {
        var root = Files.createTempDirectory("earthward-elevation-missing");
        assertEquals(ElevationPackageLoader.Status.MISSING, ElevationPackageLoader.inspect(root, "pilot").status());
        assertFalse(Files.exists(root.resolve("pilot")));
    }

    private static String manifest(byte[] bytes, boolean ready) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return """
                {"schema_version":1,"package_id":"pilot","kind":"development_elevation_extract","generation_ready":%s,
                 "files":{"elevation.json":{"bytes":%d,"sha256":"%s"}}}
                """.formatted(ready, bytes.length, hash);
    }
}

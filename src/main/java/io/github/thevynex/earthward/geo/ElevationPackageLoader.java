package io.github.thevynex.earthward.geo;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Verifies a separate elevation package before exposing any sample. */
public final class ElevationPackageLoader {
    private ElevationPackageLoader() {}
    public enum Status { MISSING, INVALID, VERIFIED }
    public record Inspection(Status status, String detail, ElevationGrid grid) {}

    public static Inspection inspect(Path packagesRoot, String packageId) {
        try {
            if (packagesRoot == null || packageId == null || !packageId.matches("[a-z][a-z0-9_-]{0,63}")) return invalid("invalid_path");
            Path root = packagesRoot.toAbsolutePath().normalize();
            Path directory = root.resolve(packageId).normalize();
            if (!directory.getParent().equals(root)) return invalid("invalid_path");
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return new Inspection(Status.MISSING, "missing", null);
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) return invalid("invalid_directory");
            byte[] manifestBytes = read(regularChild(directory, "elevation-manifest.json"), 65_536);
            var manifest = JsonParser.parseString(new String(manifestBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (manifest.get("schema_version").getAsInt() != 1
                    || !packageId.equals(manifest.get("package_id").getAsString())
                    || !"development_elevation_extract".equals(manifest.get("kind").getAsString())
                    || manifest.get("generation_ready").getAsBoolean()) return invalid("manifest_contract");
            var file = manifest.getAsJsonObject("files").getAsJsonObject("elevation.json");
            long expectedBytes = file.get("bytes").getAsLong();
            String expectedHash = file.get("sha256").getAsString();
            if (expectedBytes <= 0 || expectedBytes > ElevationGrid.MAX_JSON_CHARS || !expectedHash.matches("[0-9a-f]{64}")) return invalid("file_contract");
            byte[] gridBytes = read(regularChild(directory, "elevation.json"), ElevationGrid.MAX_JSON_CHARS);
            if (gridBytes.length != expectedBytes || !MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.US_ASCII),
                    sha256(gridBytes).getBytes(StandardCharsets.US_ASCII))) return invalid("elevation_digest");
            return new Inspection(Status.VERIFIED, "verified", ElevationGrid.parse(new String(gridBytes, StandardCharsets.UTF_8)));
        } catch (Exception error) {
            return invalid("invalid_package");
        }
    }

    private static Path regularChild(Path directory, String name) throws IOException {
        Path path = directory.resolve(name).normalize();
        if (!path.getParent().equals(directory) || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe package file");
        return path;
    }

    private static byte[] read(Path path, long limit) throws IOException {
        long size = Files.size(path);
        if (size <= 0 || size > limit) throw new IOException("Invalid package file size");
        return Files.readAllBytes(path);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Inspection invalid(String detail) { return new Inspection(Status.INVALID, detail, null); }
}

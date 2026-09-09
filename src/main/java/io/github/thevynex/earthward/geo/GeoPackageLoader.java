package io.github.thevynex.earthward.geo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Validates an external package without mutating saves or loading geometry into RAM. */
public final class GeoPackageLoader {
    private GeoPackageLoader() {}

    public enum Status { MISSING, INVALID, VERIFIED_GEOMETRY_ONLY }
    public record Inspection(Status status, String detail, GeoPackageManifest manifest) {}

    public static Inspection inspect(Path packagesRoot, String packageId) {
        try {
            if (packagesRoot == null || packageId == null || !packageId.matches("[a-z][a-z0-9_-]{0,63}")) {
                return invalid("invalid_path");
            }
            Path normalizedRoot = packagesRoot.toAbsolutePath().normalize();
            Path directory = normalizedRoot.resolve(packageId).normalize();
            if (!directory.getParent().equals(normalizedRoot)) return invalid("invalid_path");
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return new Inspection(Status.MISSING, "missing", null);
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) {
                return invalid("invalid_directory");
            }
            Path manifestPath = regularChild(directory, "manifest.json");
            Path geometryPath = regularChild(directory, "geometry.json");
            long manifestBytes = Files.size(manifestPath);
            if (manifestBytes <= 0 || manifestBytes > GeoPackageManifest.MAX_MANIFEST_CHARS * 4L) {
                return invalid("manifest_size");
            }
            String manifestText = Files.readString(manifestPath, StandardCharsets.UTF_8);
            GeoPackageManifest manifest = GeoPackageManifest.parse(manifestText, packageId);
            if (Files.size(geometryPath) != manifest.geometryBytes()) return invalid("geometry_size");
            String digest = sha256(geometryPath, GeoPackageManifest.MAX_GEOMETRY_BYTES);
            if (!MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                    manifest.geometrySha256().getBytes(StandardCharsets.US_ASCII))) {
                return invalid("geometry_digest");
            }
            return new Inspection(Status.VERIFIED_GEOMETRY_ONLY, "verified_geometry_only", manifest);
        } catch (Exception error) {
            return invalid("invalid_package");
        }
    }

    private static Path regularChild(Path directory, String name) throws IOException {
        Path path = directory.resolve(name).normalize();
        if (!path.getParent().equals(directory) || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Missing or unsafe package file");
        }
        return path;
    }

    private static String sha256(Path path, long limit) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0;
        byte[] buffer = new byte[8192];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > limit) throw new IOException("Geometry exceeds size limit");
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static Inspection invalid(String detail) {
        return new Inspection(Status.INVALID, detail, null);
    }
}

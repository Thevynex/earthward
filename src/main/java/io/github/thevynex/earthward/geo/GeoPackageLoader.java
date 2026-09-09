package io.github.thevynex.earthward.geo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Validates an external package without mutating saves. Geometry is parsed only after hash verification. */
public final class GeoPackageLoader {
    private GeoPackageLoader() {}

    public enum Status { MISSING, INVALID, VERIFIED_GEOMETRY_ONLY }
    public record Inspection(Status status, String detail, GeoPackageManifest manifest, GeoGeometry geometry) {}

    public static Inspection inspect(Path packagesRoot, String packageId) {
        try {
            if (packagesRoot == null || packageId == null || !packageId.matches("[a-z][a-z0-9_-]{0,63}")) {
                return invalid("invalid_path");
            }
            Path normalizedRoot = packagesRoot.toAbsolutePath().normalize();
            Path directory = normalizedRoot.resolve(packageId).normalize();
            if (!directory.getParent().equals(normalizedRoot)) return invalid("invalid_path");
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
                return new Inspection(Status.MISSING, "missing", null, null);
            }
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) {
                return invalid("invalid_directory");
            }
            Path manifestPath = regularChild(directory, "manifest.json");
            Path geometryPath = regularChild(directory, "geometry.json");
            byte[] manifestBytes = readBounded(manifestPath, GeoPackageManifest.MAX_MANIFEST_CHARS * 4L);
            String manifestText = new String(manifestBytes, StandardCharsets.UTF_8);
            GeoPackageManifest manifest = GeoPackageManifest.parse(manifestText, packageId);
            byte[] geometryBytes = readBounded(geometryPath, GeoPackageManifest.MAX_GEOMETRY_BYTES);
            if (geometryBytes.length != manifest.geometryBytes()) return invalid("geometry_size");
            String digest = sha256(geometryBytes);
            if (!MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                    manifest.geometrySha256().getBytes(StandardCharsets.US_ASCII))) {
                return invalid("geometry_digest");
            }
            GeoGeometry geometry = GeoGeometry.parse(new String(geometryBytes, StandardCharsets.UTF_8));
            return new Inspection(Status.VERIFIED_GEOMETRY_ONLY, "verified_geometry_only", manifest, geometry);
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

    private static byte[] readBounded(Path path, long limit) throws IOException {
        try (InputStream input = Files.newInputStream(path); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > limit) throw new IOException("Package file exceeds size limit");
                output.write(buffer, 0, read);
            }
            if (total == 0) throw new IOException("Package file is empty");
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] data) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static Inspection invalid(String detail) {
        return new Inspection(Status.INVALID, detail, null, null);
    }
}

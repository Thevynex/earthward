package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.GeoGeometry;
import java.util.Arrays;

/** Deterministic 1 m/block road and building-footprint rasterization for one chunk. */
public final class ChunkFeatureRasterizer {
    public static final double INFERRED_ROAD_HALF_WIDTH_METRES = 1.5;
    private final ChunkGeometryIndex index;

    public ChunkFeatureRasterizer(ChunkGeometryIndex index) {
        if (index == null) throw new IllegalArgumentException("Geometry index required");
        this.index = index;
    }

    public ChunkMask rasterize(int chunkX, int chunkZ) {
        byte[] cells = new byte[256];
        int originX = Math.multiplyExact(chunkX, 16);
        int originZ = Math.multiplyExact(chunkZ, 16);
        for (var feature : index.featuresForChunk(chunkX, chunkZ)) {
            if (feature.layers().contains(GeoGeometry.Layer.ROAD_CENTERLINE)) {
                rasterizeRoad(feature, cells, originX, originZ);
            }
            if (feature.layers().contains(GeoGeometry.Layer.BUILDING_OUTLINE)) {
                rasterizeBuilding(feature, cells, originX, originZ);
            }
        }
        int roads = 0, buildings = 0;
        for (byte cell : cells) {
            if (cell == Surface.ROAD.code) roads++;
            if (cell == Surface.BUILDING.code) buildings++;
        }
        return new ChunkMask(chunkX, chunkZ, cells, roads, buildings, true);
    }

    private static void rasterizeRoad(ChunkGeometryIndex.Feature feature, byte[] cells, int originX, int originZ) {
        var points = feature.points();
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int offset = localZ * 16 + localX;
                if (cells[offset] == Surface.BUILDING.code) continue;
                double x = originX + localX + 0.5;
                double z = originZ + localZ + 0.5;
                for (int point = 1; point < points.size(); point++) {
                    if (distanceSquared(x, z, points.get(point - 1), points.get(point))
                            <= INFERRED_ROAD_HALF_WIDTH_METRES * INFERRED_ROAD_HALF_WIDTH_METRES) {
                        cells[offset] = Surface.ROAD.code;
                        break;
                    }
                }
            }
        }
    }

    private static void rasterizeBuilding(ChunkGeometryIndex.Feature feature, byte[] cells, int originX, int originZ) {
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                double x = originX + localX + 0.5;
                double z = originZ + localZ + 0.5;
                if (insidePolygon(x, z, feature)) cells[localZ * 16 + localX] = Surface.BUILDING.code;
            }
        }
    }

    static boolean insidePolygon(double x, double z, ChunkGeometryIndex.Feature feature) {
        boolean inside = false;
        var points = feature.points();
        for (int i = 0, j = points.size() - 1; i < points.size(); j = i++) {
            var a = points.get(i); var b = points.get(j);
            boolean crosses = (a.z() > z) != (b.z() > z)
                    && x < (b.x() - a.x()) * (z - a.z()) / (b.z() - a.z()) + a.x();
            if (crosses) inside = !inside;
        }
        return inside;
    }

    private static double distanceSquared(double x, double z, ChunkGeometryIndex.Point a, ChunkGeometryIndex.Point b) {
        double dx = b.x() - a.x(), dz = b.z() - a.z();
        double length = dx * dx + dz * dz;
        if (length == 0) return (x - a.x()) * (x - a.x()) + (z - a.z()) * (z - a.z());
        double t = Math.max(0, Math.min(1, ((x - a.x()) * dx + (z - a.z()) * dz) / length));
        double closestX = a.x() + t * dx, closestZ = a.z() + t * dz;
        return (x - closestX) * (x - closestX) + (z - closestZ) * (z - closestZ);
    }

    public enum Surface {
        NONE((byte) 0), ROAD((byte) 1), BUILDING((byte) 2);
        private final byte code;
        Surface(byte code) { this.code = code; }
        static Surface from(byte code) { return switch (code) { case 1 -> ROAD; case 2 -> BUILDING; default -> NONE; }; }
    }

    public record ChunkMask(int chunkX, int chunkZ, byte[] cells, int roadBlocks, int buildingBlocks,
                            boolean roadWidthInferred) {
        public ChunkMask {
            if (cells.length != 256 || roadBlocks < 0 || buildingBlocks < 0 || roadBlocks + buildingBlocks > 256) {
                throw new IllegalArgumentException("Invalid raster mask");
            }
            cells = Arrays.copyOf(cells, cells.length);
        }
        @Override public byte[] cells() { return Arrays.copyOf(cells, cells.length); }
        public Surface surfaceAt(int localX, int localZ) {
            if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16) throw new IndexOutOfBoundsException();
            return Surface.from(cells[localZ * 16 + localX]);
        }
    }
}

package io.github.thevynex.earthward.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable chunk-local index. Projection is performed once; chunk queries never scan the full region. */
public final class ChunkGeometryIndex {
    public static final int CHUNK_SIZE_METRES = 16;
    public static final int MAX_MEMBERSHIPS = 2_000_000;

    public record Point(double x, double z) {}
    public record Bounds(double west, double north, double east, double south) {
        public Bounds {
            if (!Double.isFinite(west) || !Double.isFinite(north) || !Double.isFinite(east)
                    || !Double.isFinite(south) || west > east || north > south) {
                throw new IllegalArgumentException("Invalid local bounds");
            }
        }
    }
    public record Feature(String id, Set<GeoGeometry.Layer> layers, List<Point> points, Bounds bounds) {
        public Feature {
            layers = Set.copyOf(layers);
            points = List.copyOf(points);
        }
    }

    private final Map<Long, List<Feature>> chunks;
    private final List<Feature> features;
    private final Bounds bounds;
    private final int membershipCount;

    private ChunkGeometryIndex(Map<Long, List<Feature>> chunks, List<Feature> features,
                               Bounds bounds, int membershipCount) {
        var immutable = new HashMap<Long, List<Feature>>(chunks.size());
        chunks.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        this.chunks = Map.copyOf(immutable);
        this.features = List.copyOf(features);
        this.bounds = bounds;
        this.membershipCount = membershipCount;
    }

    public static ChunkGeometryIndex from(GeoGeometry geometry) {
        var geographic = geometry.bounds();
        var projection = new LocalMetricProjection(
                (geographic.south() + geographic.north()) / 2.0,
                (geographic.west() + geographic.east()) / 2.0);
        var features = new ArrayList<Feature>(geometry.features().size());
        var chunks = new HashMap<Long, List<Feature>>();
        double west = Double.POSITIVE_INFINITY, north = Double.POSITIVE_INFINITY;
        double east = Double.NEGATIVE_INFINITY, south = Double.NEGATIVE_INFINITY;
        int memberships = 0;
        for (var source : geometry.features()) {
            var points = new ArrayList<Point>(source.points().size());
            double featureWest = Double.POSITIVE_INFINITY, featureNorth = Double.POSITIVE_INFINITY;
            double featureEast = Double.NEGATIVE_INFINITY, featureSouth = Double.NEGATIVE_INFINITY;
            for (var point : source.points()) {
                var projected = projection.toWorld(point.latitude(), point.longitude());
                var local = new Point(projected.xMetres(), projected.zMetres());
                points.add(local);
                featureWest = Math.min(featureWest, local.x()); featureEast = Math.max(featureEast, local.x());
                featureNorth = Math.min(featureNorth, local.z()); featureSouth = Math.max(featureSouth, local.z());
            }
            var localBounds = new Bounds(featureWest, featureNorth, featureEast, featureSouth);
            var feature = new Feature(source.id(), source.layers(), points, localBounds);
            features.add(feature);
            int minChunkX = blockToChunk(featureWest);
            int maxChunkX = blockToChunk(featureEast);
            int minChunkZ = blockToChunk(featureNorth);
            int maxChunkZ = blockToChunk(featureSouth);
            long additions = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
            if (additions > MAX_MEMBERSHIPS - memberships) {
                throw new IllegalArgumentException("Chunk index membership budget exceeded");
            }
            memberships += (int) additions;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    chunks.computeIfAbsent(key(chunkX, chunkZ), ignored -> new ArrayList<>()).add(feature);
                }
            }
            west = Math.min(west, featureWest); east = Math.max(east, featureEast);
            north = Math.min(north, featureNorth); south = Math.max(south, featureSouth);
        }
        return new ChunkGeometryIndex(chunks, features, new Bounds(west, north, east, south), memberships);
    }

    public List<Feature> featuresForChunk(int chunkX, int chunkZ) {
        return chunks.getOrDefault(key(chunkX, chunkZ), List.of());
    }

    public List<Feature> features() { return features; }
    public Bounds bounds() { return bounds; }
    public int indexedChunkCount() { return chunks.size(); }
    public int membershipCount() { return membershipCount; }

    static int blockToChunk(double metres) {
        if (!Double.isFinite(metres) || metres < Integer.MIN_VALUE || metres > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Coordinate outside supported block range");
        }
        return Math.floorDiv((int) Math.floor(metres), CHUNK_SIZE_METRES);
    }

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}

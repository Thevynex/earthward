package io.github.thevynex.earthward.geo;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/** Immutable validated Copernicus DSM grid with bilinear geographic sampling. */
public record ElevationGrid(int rows, int columns, Bounds bounds,
                            double longitudeStep, double latitudeStep,
                            List<List<Double>> elevationsMetres) {
    public static final int MAX_CELLS = 100_000;
    public static final int MAX_JSON_CHARS = 4 * 1024 * 1024;

    public ElevationGrid {
        if (rows <= 0 || columns <= 0 || (long) rows * columns > MAX_CELLS
                || !Double.isFinite(longitudeStep) || !Double.isFinite(latitudeStep)
                || longitudeStep <= 0 || latitudeStep <= 0 || elevationsMetres.size() != rows) {
            throw new IllegalArgumentException("Invalid elevation grid dimensions");
        }
        var copy = new ArrayList<List<Double>>(rows);
        for (var row : elevationsMetres) {
            if (row.size() != columns) throw new IllegalArgumentException("Ragged elevation grid");
            var rowCopy = List.copyOf(row);
            for (double value : rowCopy) {
                if (!Double.isFinite(value) || value < -500 || value > 9000) {
                    throw new IllegalArgumentException("Invalid elevation sample");
                }
            }
            copy.add(rowCopy);
        }
        elevationsMetres = List.copyOf(copy);
    }

    public record Bounds(double west, double south, double east, double north) {
        public Bounds {
            if (!Double.isFinite(west) || !Double.isFinite(south) || !Double.isFinite(east)
                    || !Double.isFinite(north) || west >= east || south >= north) {
                throw new IllegalArgumentException("Invalid elevation bounds");
            }
        }
        public boolean contains(double longitude, double latitude) {
            return longitude >= west && longitude <= east && latitude >= south && latitude <= north;
        }
    }

    public static ElevationGrid parse(String json) {
        if (json == null || json.length() > MAX_JSON_CHARS) throw new IllegalArgumentException("Elevation JSON too large");
        var root = JsonParser.parseString(json).getAsJsonObject();
        if (root.get("schema_version").getAsInt() != 1
                || !"EPSG:4326".equals(root.get("horizontal_crs").getAsString())
                || !"EGM2008_geoid_EPSG_3855".equals(root.get("vertical_reference").getAsString())
                || !"metres".equals(root.get("units").getAsString())
                || !"north_to_south".equals(root.get("row_order").getAsString())
                || !"west_to_east".equals(root.get("column_order").getAsString())) {
            throw new IllegalArgumentException("Unsupported elevation contract");
        }
        int rows = root.get("rows").getAsInt();
        int columns = root.get("columns").getAsInt();
        var bbox = root.getAsJsonArray("sample_bounds_west_south_east_north");
        var pixel = root.getAsJsonArray("pixel_size_degrees");
        if (bbox.size() != 4 || pixel.size() != 2) throw new IllegalArgumentException("Invalid elevation metadata");
        var values = root.getAsJsonArray("elevations_metres");
        if (values.size() != rows) throw new IllegalArgumentException("Elevation row mismatch");
        var output = new ArrayList<List<Double>>(rows);
        for (var rowValue : values) {
            var row = rowValue.getAsJsonArray();
            if (row.size() != columns) throw new IllegalArgumentException("Elevation column mismatch");
            var parsed = new ArrayList<Double>(columns);
            for (var value : row) parsed.add(value.getAsDouble());
            output.add(parsed);
        }
        return new ElevationGrid(rows, columns,
                new Bounds(bbox.get(0).getAsDouble(), bbox.get(1).getAsDouble(),
                        bbox.get(2).getAsDouble(), bbox.get(3).getAsDouble()),
                pixel.get(0).getAsDouble(), pixel.get(1).getAsDouble(), output);
    }

    public double sampleMetres(double longitude, double latitude) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude) || !bounds.contains(longitude, latitude)) {
            throw new IllegalArgumentException("Coordinate outside elevation grid");
        }
        if (rows == 1 || columns == 1) return elevationsMetres.getFirst().getFirst();
        double column = (longitude - bounds.west()) / (bounds.east() - bounds.west()) * (columns - 1);
        double row = (bounds.north() - latitude) / (bounds.north() - bounds.south()) * (rows - 1);
        int x0 = Math.max(0, Math.min(columns - 1, (int) Math.floor(column)));
        int z0 = Math.max(0, Math.min(rows - 1, (int) Math.floor(row)));
        int x1 = Math.min(columns - 1, x0 + 1);
        int z1 = Math.min(rows - 1, z0 + 1);
        double tx = column - x0;
        double tz = row - z0;
        double north = lerp(elevationsMetres.get(z0).get(x0), elevationsMetres.get(z0).get(x1), tx);
        double south = lerp(elevationsMetres.get(z1).get(x0), elevationsMetres.get(z1).get(x1), tx);
        return lerp(north, south, tz);
    }

    private static double lerp(double a, double b, double amount) { return a + (b - a) * amount; }
}

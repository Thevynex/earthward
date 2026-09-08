package io.github.thevynex.earthward.selection;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public record SelectionCatalog(String regionId, List<Level> levels) {
    public record Level(String label, String name) {
        public Level {
            if (label == null || name == null || label.isBlank() || name.isBlank()
                    || label.length() > 32 || name.length() > 64) {
                throw new IllegalArgumentException("Invalid administrative label");
            }
        }
    }

    public SelectionCatalog {
        if (regionId == null || !regionId.matches("[a-z][a-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("Invalid region identifier");
        }
        levels = List.copyOf(levels);
        if (levels.isEmpty() || levels.size() > 8) {
            throw new IllegalArgumentException("Expected 1-8 administrative levels");
        }
    }

    public static SelectionCatalog parse(String json) {
        if (json == null || json.length() > 16384) throw new IllegalArgumentException("Catalog too large or missing");
        var root = JsonParser.parseString(json).getAsJsonObject();
        if (root.get("schema_version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported catalog version");
        var levels = new ArrayList<Level>();
        for (var value : root.getAsJsonArray("levels")) {
            var level = value.getAsJsonObject();
            levels.add(new Level(level.get("label").getAsString(), level.get("name").getAsString()));
        }
        return new SelectionCatalog(root.get("region_id").getAsString(), levels);
    }

    public static SelectionCatalog loadBundled() throws IOException {
        try (var stream = SelectionCatalog.class.getResourceAsStream("/data/earthward/pilot_selection.json")) {
            if (stream == null) throw new IOException("Missing pilot selection catalog");
            byte[] data = stream.readNBytes(16385);
            if (data.length > 16384) throw new IOException("Catalog too large");
            return parse(new String(data, StandardCharsets.UTF_8));
        }
    }
}

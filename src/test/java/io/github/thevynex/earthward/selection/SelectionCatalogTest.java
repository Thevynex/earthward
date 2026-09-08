package io.github.thevynex.earthward.selection;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class SelectionCatalogTest {
    @Test void loadsBundledPlanningCatalog() throws Exception {
        var catalog = SelectionCatalog.loadBundled();
        assertEquals("tr_istanbul_kadikoy_caferaga", catalog.regionId());
        assertEquals(4, catalog.levels().size());
    }
    @Test void supportsDifferentAdministrativeDepths() {
        assertEquals(1, new SelectionCatalog("test", List.of(new SelectionCatalog.Level("Country", "Test"))).levels().size());
    }
    @Test void copiesCallerList() {
        var levels = new ArrayList<>(List.of(new SelectionCatalog.Level("Country", "Test")));
        var catalog = new SelectionCatalog("test", levels);
        levels.clear();
        assertEquals(1, catalog.levels().size());
        assertThrows(UnsupportedOperationException.class, () -> catalog.levels().clear());
    }
    @Test void rejectsUnsafeIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new SelectionCatalog("../test", List.of()));
    }
    @Test void rejectsEmptyLevels() {
        assertThrows(IllegalArgumentException.class, () -> new SelectionCatalog("test", List.of()));
    }
    @Test void rejectsMalformedJson() {
        assertThrows(RuntimeException.class, () -> SelectionCatalog.parse("{"));
    }
    @Test void rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException.class, () -> SelectionCatalog.parse("{\"schema_version\":2}"));
    }
    @Test void rejectsOversizedInput() {
        assertThrows(IllegalArgumentException.class, () -> SelectionCatalog.parse(" ".repeat(16385)));
    }
}

package io.github.thevynex.earthward.selection;

public final class SpawnSelectionContractTest {
    private static int passed;

    public static void main(String[] args) {
        accept("synthetic_zero", 0, 0);
        accept("synthetic_north", 90, -180);
        accept("synthetic_south", -90, 179.999);
        reject(null, 0, 0);
        reject("", 0, 0);
        reject("../data", 0, 0);
        reject("a".repeat(65), 0, 0);
        reject("synthetic", Double.NaN, 0);
        reject("synthetic", Double.POSITIVE_INFINITY, 0);
        reject("synthetic", 90.001, 0);
        reject("synthetic", -90.001, 0);
        reject("synthetic", 0, Double.NaN);
        reject("synthetic", 0, Double.NEGATIVE_INFINITY);
        reject("synthetic", 0, 180);
        reject("synthetic", 0, -180.001);
        System.out.println("PASS: " + passed + " synthetic input contracts; not geographic accuracy tests.");
    }

    private static void accept(String id, double lat, double lon) {
        var selection = new SpawnSelection(id, lat, lon);
        if (!selection.regionPackageId().equals(id)
                || selection.latitude() != lat || selection.longitude() != lon) {
            throw new AssertionError("Selection changed input values");
        }
        passed++;
    }

    private static void reject(String id, double lat, double lon) {
        try {
            new SpawnSelection(id, lat, lon);
        } catch (IllegalArgumentException expected) {
            passed++;
            return;
        }
        throw new AssertionError("Invalid selection was accepted");
    }
}

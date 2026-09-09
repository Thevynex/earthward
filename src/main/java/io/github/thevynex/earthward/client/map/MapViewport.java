package io.github.thevynex.earthward.client.map;

/** Pure camera math for a pannable, cursor-anchored local metric map. */
public final class MapViewport {
    public static final double MIN_SCALE = 0.02;
    public static final double MAX_SCALE = 16.0;

    private double centerX;
    private double centerZ;
    private double pixelsPerMetre = 1.0;

    public double centerX() { return centerX; }
    public double centerZ() { return centerZ; }
    public double pixelsPerMetre() { return pixelsPerMetre; }

    public void fit(double west, double north, double east, double south, int width, int height, int padding) {
        if (!finite(west, north, east, south) || west > east || north > south || width <= padding * 2 || height <= padding * 2) {
            throw new IllegalArgumentException("Invalid map fit bounds");
        }
        centerX = (west + east) / 2.0;
        centerZ = (north + south) / 2.0;
        double spanX = Math.max(1.0, east - west);
        double spanZ = Math.max(1.0, south - north);
        pixelsPerMetre = clamp(Math.min((width - padding * 2.0) / spanX, (height - padding * 2.0) / spanZ));
    }

    public void panPixels(double dragX, double dragY) {
        if (!Double.isFinite(dragX) || !Double.isFinite(dragY)) throw new IllegalArgumentException("Invalid pan");
        centerX -= dragX / pixelsPerMetre;
        centerZ -= dragY / pixelsPerMetre;
    }

    public void zoomAt(double factor, double mouseX, double mouseY, int width, int height) {
        if (!Double.isFinite(factor) || factor <= 0 || !finite(mouseX, mouseY) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid zoom");
        }
        double worldX = screenToWorldX(mouseX, width);
        double worldZ = screenToWorldZ(mouseY, height);
        double next = clamp(pixelsPerMetre * factor);
        pixelsPerMetre = next;
        centerX = worldX - (mouseX - width / 2.0) / next;
        centerZ = worldZ - (mouseY - height / 2.0) / next;
    }

    public int worldToScreenX(double x, int width) {
        return (int) Math.round((x - centerX) * pixelsPerMetre + width / 2.0);
    }

    public int worldToScreenY(double z, int height) {
        return (int) Math.round((z - centerZ) * pixelsPerMetre + height / 2.0);
    }

    public double screenToWorldX(double screenX, int width) {
        return centerX + (screenX - width / 2.0) / pixelsPerMetre;
    }

    public double screenToWorldZ(double screenY, int height) {
        return centerZ + (screenY - height / 2.0) / pixelsPerMetre;
    }

    private static double clamp(double value) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }
}

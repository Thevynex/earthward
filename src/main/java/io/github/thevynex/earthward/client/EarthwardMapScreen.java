package io.github.thevynex.earthward.client;

import io.github.thevynex.earthward.client.map.MapViewport;
import io.github.thevynex.earthward.client.map.ProjectedMapModel;
import io.github.thevynex.earthward.client.worldgen.EarthwardWorldCreationLauncher;
import io.github.thevynex.earthward.geo.GeoGeometry;
import io.github.thevynex.earthward.geo.GeoPackageLoader;
import io.github.thevynex.earthward.selection.SelectionCatalog;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

/** Full-screen, pannable local pilot map opened in place of fresh vanilla world creation. */
public final class EarthwardMapScreen extends Screen {
    private static final int ROAD_COLOR = 0xFFE6E5E3;
    private static final int BUILDING_COLOR = 0xFFD5803B;
    private static final int MAP_BACKGROUND = 0xFF202A2F;
    private static final int MAX_SEGMENTS_PER_FRAME = 50_000;

    private final Screen parent;
    private final MapViewport viewport = new MapViewport();
    private final String packageId;
    private volatile GeoPackageLoader.Inspection inspection;
    private volatile ProjectedMapModel model;
    private volatile boolean loadFinished;
    private boolean fitted;
    private boolean dragged;
    private double pressX;
    private double pressY;
    private Double selectedX;
    private Double selectedZ;
    private Button earthwardCreateButton;
    private boolean spawnSelected;

    public EarthwardMapScreen(Screen parent) {
        super(Component.translatable("earthward.map.title"));
        this.parent = parent;
        String id;
        try {
            id = SelectionCatalog.loadBundled().regionId();
        } catch (Exception error) {
            id = "invalid_catalog";
            loadFinished = true;
            inspection = new GeoPackageLoader.Inspection(GeoPackageLoader.Status.INVALID, "invalid_catalog", null, null);
        }
        packageId = id;
        if (!loadFinished) {
            CompletableFuture.supplyAsync(() -> GeoPackageLoader.inspect(
                    FMLPaths.GAMEDIR.get().resolve("earthward").resolve("packages"), packageId))
                    .thenAccept(result -> {
                        inspection = result;
                        if (result.status() == GeoPackageLoader.Status.VERIFIED_GEOMETRY_ONLY) {
                            model = ProjectedMapModel.from(result.geometry());
                        }
                        loadFinished = true;
                    });
        }
    }

    @Override
    protected void init() {
        int pad = 12;
        addRenderableWidget(Button.builder(Component.literal("+"), button -> zoom(1.5))
                .bounds(width - 48, pad, 36, 28).build());
        addRenderableWidget(Button.builder(Component.literal("−"), button -> zoom(1.0 / 1.5))
                .bounds(width - 48, pad + 34, 36, 28).build());
        addRenderableWidget(Button.builder(Component.translatable("earthward.map.fit"), button -> fitMap())
                .bounds(width - 96, pad + 68, 84, 20).build());

        earthwardCreateButton = addRenderableWidget(Button.builder(
                Component.literal("Güvenli konum seç"), button -> {
                    if (spawnSelected) EarthwardWorldCreationLauncher.start(packageId,
                            (int)Math.round(selectedX), (int)Math.round(selectedZ));
                })
                .bounds(pad, height - 84, 210, 20).build());
        earthwardCreateButton.active = false;
        addRenderableWidget(Button.builder(Component.translatable("earthward.map.classic"),
                button -> EarthwardClient.openVanillaCreateWorld(this))
                .bounds(pad, height - 58, 210, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(pad, height - 32, 210, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, MAP_BACKGROUND);
        ProjectedMapModel current = model;
        if (current != null) {
            if (!fitted) fitMap();
            drawGeometry(graphics, current);
            drawSelection(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (earthwardCreateButton != null) {
            earthwardCreateButton.active = spawnSelected;
            earthwardCreateButton.setMessage(Component.literal(spawnSelected ? "Earthward dünyasını oluştur" : "Güvenli konum seç"));
        }
        drawHud(graphics);
    }

    private void drawGeometry(GuiGraphics graphics, ProjectedMapModel current) {
        double west = viewport.screenToWorldX(0, width);
        double east = viewport.screenToWorldX(width, width);
        double north = viewport.screenToWorldZ(0, height);
        double south = viewport.screenToWorldZ(height, height);
        int segments = 0;
        for (GeoGeometry.Layer pass : new GeoGeometry.Layer[]{GeoGeometry.Layer.ROAD_CENTERLINE,
                GeoGeometry.Layer.BUILDING_OUTLINE}) {
            int color = pass == GeoGeometry.Layer.ROAD_CENTERLINE ? ROAD_COLOR : BUILDING_COLOR;
            for (var feature : current.features()) {
                if (segments >= MAX_SEGMENTS_PER_FRAME) return;
                if (!feature.layers().contains(pass) || !feature.bounds().intersects(west, north, east, south)) continue;
                var points = feature.points();
                for (int index = 1; index < points.size() && segments < MAX_SEGMENTS_PER_FRAME; index++, segments++) {
                    drawSegment(graphics, points.get(index - 1), points.get(index), color,
                            pass == GeoGeometry.Layer.ROAD_CENTERLINE ? 2 : 1);
                }
            }
        }
    }

    private void drawSegment(GuiGraphics graphics, ProjectedMapModel.Point a, ProjectedMapModel.Point b,
                             int color, int size) {
        int x1 = viewport.worldToScreenX(a.x(), width);
        int y1 = viewport.worldToScreenY(a.z(), height);
        int x2 = viewport.worldToScreenX(b.x(), width);
        int y2 = viewport.worldToScreenY(b.z(), height);
        int steps = Math.min(256, Math.max(1, (int) Math.ceil(Math.hypot(x2 - x1, y2 - y1))));
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            if (x >= 0 && x < width && y >= 0 && y < height) {
                graphics.fill(x, y, x + size, y + size, color);
            }
        }
    }

    private void drawSelection(GuiGraphics graphics) {
        if (selectedX == null || selectedZ == null) return;
        int x = viewport.worldToScreenX(selectedX, width);
        int y = viewport.worldToScreenY(selectedZ, height);
        graphics.fill(x - 5, y - 5, x + 6, y + 6, 0xFFFFFFFF);
        graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFFE56458);
    }

    private void drawHud(GuiGraphics graphics) {
        int x = 12;
        int y = 12;
        int boxWidth = Math.min(360, width - 80);
        graphics.fill(x, y, x + boxWidth, y + 106, 0xD8191919);
        graphics.drawString(font, title, x + 8, y + 8, 0xFFFFFFFF, true);
        graphics.drawString(font, Component.translatable("earthward.map.pilot"), x + 8, y + 24, 0xFF72BC8F, false);
        Component status;
        if (!loadFinished) {
            status = Component.translatable("earthward.map.loading");
        } else if (inspection == null || inspection.status() == GeoPackageLoader.Status.INVALID) {
            status = Component.translatable("earthward.map.invalid", packageId);
        } else if (inspection.status() == GeoPackageLoader.Status.MISSING) {
            status = Component.translatable("earthward.map.missing", packageId);
        } else {
            status = Component.translatable("earthward.map.ready", model.buildingCount(), model.roadCount(), model.pointCount());
        }
        graphics.drawWordWrap(font, status, x + 8, y + 42, boxWidth - 16, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("earthward.map.controls"), x + 8, y + 82, 0xFFB8B5B0, false);
        if (selectedX != null) {
            graphics.drawString(font, Component.translatable("earthward.map.requested_spawn",
                    Math.round(selectedX), Math.round(selectedZ)), x + 8, y + 94, 0xFFEAC26B, false);
        }
    }

    private void fitMap() {
        ProjectedMapModel current = model;
        if (current == null || width <= 0 || height <= 0) return;
        var bounds = current.bounds();
        viewport.fit(bounds.west(), bounds.north(), bounds.east(), bounds.south(), width, height, 56);
        fitted = true;
    }

    private void zoom(double factor) {
        viewport.zoomAt(factor, width / 2.0, height / 2.0, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && model != null) {
            pressX = mouseX;
            pressY = mouseY;
            dragged = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        if (button == 0 && model != null) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) > 3) dragged = true;
            viewport.panPixels(dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        if (button == 0 && model != null && !dragged && !handled) {
            selectedX = viewport.screenToWorldX(mouseX, width);
            selectedZ = viewport.screenToWorldZ(mouseY, height);
            spawnSelected = true;
            return true;
        }
        return handled || (button == 0 && model != null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        if (model != null && verticalAmount != 0) {
            viewport.zoomAt(verticalAmount > 0 ? 1.35 : 1.0 / 1.35, mouseX, mouseY, width, height);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        minecraft.setScreen(parent != null ? parent : new TitleScreen());
    }
}

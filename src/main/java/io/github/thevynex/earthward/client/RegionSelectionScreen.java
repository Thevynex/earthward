package io.github.thevynex.earthward.client;

import io.github.thevynex.earthward.selection.SelectionCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RegionSelectionScreen extends Screen {
    private final Screen parent;
    private SelectionCatalog catalog;
    private int stage;
    private String problem;

    public RegionSelectionScreen(Screen parent) {
        super(Component.translatable("earthward.selection.title"));
        this.parent = parent;
        try {
            catalog = SelectionCatalog.loadBundled();
        } catch (Exception error) {
            problem = "earthward.selection.invalid_catalog";
        }
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(280, width - 32);
        int left = (width - panelWidth) / 2;
        if (catalog != null && stage < catalog.levels().size()) {
            var level = catalog.levels().get(stage);
            addRenderableWidget(Button.builder(Component.literal(level.label() + ": " + level.name()), button -> {
                stage++;
                rebuildWidgets();
            }).bounds(left, 64, panelWidth, 20).build());
        } else if (catalog != null) {
            var create = Button.builder(Component.translatable("earthward.selection.create_unavailable"), button -> {})
                    .bounds(left, 64, panelWidth, 20).build();
            create.active = false;
            addRenderableWidget(create);
        }
        addRenderableWidget(Button.builder(Component.translatable(stage > 0 ? "gui.back" : "gui.cancel"), button -> {
            if (stage > 0) {
                stage--;
                rebuildWidgets();
            } else {
                onClose();
            }
        }).bounds(left, height - 28, panelWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF191919);
        int panelWidth = Math.min(280, width - 32);
        int left = (width - panelWidth) / 2;
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("earthward.selection.preview"), width / 2, 34, 0xFFDE9255);
        Component detail;
        if (problem != null) {
            detail = Component.translatable(problem);
        } else if (stage < catalog.levels().size()) {
            detail = Component.translatable("earthward.selection.step", stage + 1, catalog.levels().size());
        } else {
            detail = Component.translatable("earthward.selection.no_data", catalog.regionId());
        }
        graphics.drawWordWrap(font, detail, left, 100, panelWidth, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}

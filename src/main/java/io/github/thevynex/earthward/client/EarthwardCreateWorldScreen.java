package io.github.thevynex.earthward.client;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.time.Instant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;

/** Minimal final confirmation screen using Minecraft's normal save/world loading infrastructure. */
public final class EarthwardCreateWorldScreen extends Screen {
    private final WorldCreationContext context;
    private final String packageId;
    private long seed;
    private EditBox nameField;
    private EditBox seedField;
    private Button createButton;
    private boolean creating;

    public EarthwardCreateWorldScreen(WorldCreationContext context, String packageId, long seed) {
        super(Component.literal("Earthward — Dünya Oluştur"));
        this.context = context;
        this.packageId = packageId;
        this.seed = seed;
    }

    @Override protected void init() {
        int x = width / 2 - 110, y = Math.max(54, height / 2 - 70);
        nameField = addRenderableWidget(new EditBox(font, x, y, 220, 20, Component.literal("Dünya adı")));
        nameField.setValue("Earthward Caferağa Pilot");
        nameField.setMaxLength(50);
        seedField = addRenderableWidget(new EditBox(font, x, y + 34, 220, 20, Component.literal("Tohum")));
        seedField.setValue(Long.toString(seed));
        createButton = addRenderableWidget(Button.builder(Component.literal("Dünyayı Oluştur"), button -> create())
                .bounds(x, y + 72, 220, 20).build());
        addRenderableWidget(Button.builder(Component.literal("İptal"), button -> Minecraft.getInstance().setScreen(new TitleScreen()))
                .bounds(x, y + 98, 220, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 24, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Paket: " + packageId), width / 2, 38, 0xFF72BC8F);
    }

    private void create() {
        if (creating) return;
        creating = true;
        createButton.active = false;
        String seedText = seedField.getValue().trim();
        if (!seedText.isEmpty()) {
            try { seed = Long.parseLong(seedText); } catch (NumberFormatException ignored) { seed = seedText.hashCode(); }
        }
        WorldDimensions.Complete complete = context.selectedDimensions().bake(context.datapackDimensions());
        LayeredRegistryAccess<RegistryLayer> layered = context.worldgenRegistries()
                .replaceFrom(RegistryLayer.DIMENSIONS, complete.dimensionsRegistryAccess());
        Lifecycle featureLifecycle = FeatureFlags.isExperimental(context.dataConfiguration().enabledFeatures())
                ? Lifecycle.experimental() : Lifecycle.stable();
        Lifecycle registryLifecycle = layered.compositeAccess().allRegistriesLifecycle();
        Lifecycle lifecycle = registryLifecycle.add(featureLifecycle);
        confirm(lifecycle, () -> createWorld(complete.specialWorldProperty(), layered, lifecycle),
                registryLifecycle == Lifecycle.stable());
    }

    private void confirm(Lifecycle lifecycle, Runnable action, boolean skip) {
        Minecraft minecraft = Minecraft.getInstance();
        BooleanConsumer response = accepted -> {
            if (accepted) action.run(); else { creating = false; createButton.active = true; minecraft.setScreen(this); }
        };
        if (skip || lifecycle == Lifecycle.stable()) action.run();
        else minecraft.setScreen(new ConfirmScreen(response,
                Component.translatable(lifecycle == Lifecycle.experimental()
                        ? "selectWorld.warning.experimental.title" : "selectWorld.warning.deprecated.title"),
                Component.translatable(lifecycle == Lifecycle.experimental()
                        ? "selectWorld.warning.experimental.question" : "selectWorld.warning.deprecated.question")));
    }

    private void createWorld(PrimaryLevelData.SpecialWorldProperty special,
                             LayeredRegistryAccess<RegistryLayer> registries, Lifecycle lifecycle) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.forceSetScreen(new GenericMessageScreen(Component.translatable("createWorld.preparing")));
        try {
            String displayName = nameField.getValue().trim();
            if (displayName.isEmpty()) displayName = "Earthward Pilot";
            String directory = sanitize(displayName) + "_" + Instant.now().toEpochMilli();
            LevelStorageAccess access = minecraft.getLevelSource().createAccess(directory);
            LevelSettings settings = new LevelSettings(displayName, GameType.SURVIVAL, false, Difficulty.NORMAL,
                    false, new GameRules(), context.dataConfiguration());
            WorldData data = new PrimaryLevelData(settings,
                    new net.minecraft.world.level.levelgen.WorldOptions(seed, true, false), special, lifecycle);
            if (data.worldGenSettingsLifecycle() != Lifecycle.stable()) ((PrimaryLevelData) data).withConfirmedWarning(true);
            minecraft.createWorldOpenFlows().createLevelFromExistingSettings(access,
                    context.dataPackResources(), registries, data);
        } catch (Exception error) {
            error.printStackTrace();
            creating = false;
            minecraft.setScreen(this);
        }
    }

    private static String sanitize(String value) {
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_").trim();
        return sanitized.isEmpty() || sanitized.startsWith(".") ? "Earthward_Pilot" : sanitized;
    }
}

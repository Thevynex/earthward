package io.github.thevynex.earthward.client;

import io.github.thevynex.earthward.Earthward;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Earthward.MOD_ID, value = Dist.CLIENT)
public final class EarthwardClient {
    private static boolean openingVanilla;

    private EarthwardClient() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof CreateWorldScreen createWorld)) return;
        if (openingVanilla) return;
        // Preserve recreate settings. If the mapping changes and the flag cannot be read,
        // fail closed and leave vanilla untouched rather than risking a wrong recreation.
        if (isRecreateOrUnknown(createWorld)) return;
        event.setNewScreen(new EarthwardMapScreen(event.getCurrentScreen()));
    }

    public static void openVanillaCreateWorld(Screen parent) {
        openingVanilla = true;
        try {
            CreateWorldScreen.openFresh(Minecraft.getInstance(), parent);
        } finally {
            openingVanilla = false;
        }
    }

    private static boolean isRecreateOrUnknown(CreateWorldScreen screen) {
        try {
            Field field = CreateWorldScreen.class.getDeclaredField("recreated");
            field.setAccessible(true);
            return field.getBoolean(screen);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return true;
        }
    }
}

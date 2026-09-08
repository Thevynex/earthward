package io.github.thevynex.earthward.client;

import io.github.thevynex.earthward.Earthward;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Earthward.MOD_ID, value = Dist.CLIENT)
public final class EarthwardClient {
    private EarthwardClient() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof SelectWorldScreen parent) {
            event.addListener(Button.builder(Component.literal("Earthward"),
                    button -> Minecraft.getInstance().setScreen(new RegionSelectionScreen(parent)))
                    .bounds(8, 8, 92, 20).build());
        }
    }
}

package io.github.thevynex.earthward;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Earthward.MOD_ID)
public final class Earthward {
    public static final String MOD_ID = "earthward";

    public Earthward(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LogUtils.getLogger().info("Earthward bootstrap loaded; geographic generation is not implemented.");
    }
}

package io.github.thevynex.earthward;

import com.mojang.logging.LogUtils;
import io.github.thevynex.earthward.worldgen.WorldgenRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Earthward.MOD_ID)
public final class Earthward {
    public static final String MOD_ID = "earthward";

    public Earthward(IEventBus modEventBus) {
        WorldgenRegistration.CHUNK_GENERATORS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LogUtils.getLogger().info("Earthward loaded; bounded DEM generator registered, world creation remains guarded.");
    }
}

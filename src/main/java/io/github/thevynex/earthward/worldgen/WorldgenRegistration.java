package io.github.thevynex.earthward.worldgen;

import com.mojang.serialization.MapCodec;
import io.github.thevynex.earthward.Earthward;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WorldgenRegistration {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Earthward.MOD_ID);
    static { CHUNK_GENERATORS.register("pilot", () -> EarthwardChunkGenerator.CODEC); }
    private WorldgenRegistration() {}
}

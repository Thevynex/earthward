package io.github.thevynex.earthward.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/** Deliberately simple pilot biome source; real land-cover mapping is a later data layer. */
final class PilotBiomeSource extends BiomeSource {
    private final Holder<Biome> plains;

    PilotBiomeSource(HolderGetter<Biome> biomes) {
        plains = biomes.getOrThrow(Biomes.PLAINS);
    }

    @Override protected MapCodec<? extends BiomeSource> codec() { return MapCodec.unit(this); }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return Stream.of(plains); }
    @Override public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) { return plains; }
}

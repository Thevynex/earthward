package io.github.thevynex.earthward.client.worldgen;

import io.github.thevynex.earthward.client.EarthwardCreateWorldScreen;
import io.github.thevynex.earthward.worldgen.EarthwardChunkGenerator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.resource.ResourcePackLoader;

public final class EarthwardWorldCreationLauncher {
    private EarthwardWorldCreationLauncher() {}
    public static void start(String packageId, int requestedSpawnX, int requestedSpawnZ) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.forceSetScreen(new GenericMessageScreen(Component.translatable("createWorld.preparing")));
        try {
            WorldCreationContext context = loadContext(minecraft);
            long seed = new Random().nextLong();
            RegistryAccess.Frozen registries = context.worldgenRegistries().compositeAccess();
            HolderGetter<Biome> biomes = registries.registryOrThrow(Registries.BIOME).asLookup();
            HolderGetter<NoiseGeneratorSettings> settings = registries.registryOrThrow(Registries.NOISE_SETTINGS).asLookup();
            ChunkGenerator generator = EarthwardChunkGenerator.create(biomes, settings, packageId,
                    requestedSpawnX, requestedSpawnZ);
            WorldDimensions dimensions = context.selectedDimensions().replaceOverworldGenerator(registries, generator);
            context = context.withSettings(new WorldOptions(seed, true, false), dimensions);
            minecraft.setScreen(new EarthwardCreateWorldScreen(context, packageId, seed));
        } catch (Exception error) {
            error.printStackTrace();
            minecraft.setScreen(new TitleScreen());
        }
    }
    private static WorldCreationContext loadContext(Minecraft minecraft) {
        PackRepository repository = new PackRepository(new ServerPacksSource(minecraft.directoryValidator()));
        ResourcePackLoader.populatePackRepository(repository, PackType.SERVER_DATA, false);
        WorldLoader.InitConfig configuration = new WorldLoader.InitConfig(
                new WorldLoader.PackConfig(repository, WorldDataConfiguration.DEFAULT, false, true),
                Commands.CommandSelection.INTEGRATED, 2);
        CompletableFuture<WorldCreationContext> future = WorldLoader.load(configuration, context -> {
            if (context.datapackWorldgen().registryOrThrow(Registries.WORLD_PRESET).size() == 0
                    || context.datapackWorldgen().registryOrThrow(Registries.BIOME).size() == 0)
                throw new IllegalStateException("Vanilla world registries unavailable");
            WorldGenSettings settings = new WorldGenSettings(WorldOptions.defaultWithRandomSeed(),
                    WorldPresets.createNormalWorldDimensions(context.datapackWorldgen()));
            return new WorldLoader.DataLoadOutput<>(new LoadCookie(settings, context.dataConfiguration()),
                    context.datapackDimensions());
        }, (manager, resources, registryAccess, cookie) -> {
            manager.close();
            return new WorldCreationContext(cookie.settings(), registryAccess, resources, cookie.configuration());
        }, Util.backgroundExecutor(), minecraft);
        minecraft.managedBlock(future::isDone);
        return future.join();
    }
    private record LoadCookie(WorldGenSettings settings, WorldDataConfiguration configuration) {}
}

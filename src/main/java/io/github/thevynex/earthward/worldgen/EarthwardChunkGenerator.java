package io.github.thevynex.earthward.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.thevynex.earthward.geo.ElevationPackageLoader;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.fml.loading.FMLPaths;

/** Serializable bounded pilot generator backed by an installed, hash-verified elevation package. */
public final class EarthwardChunkGenerator extends NoiseBasedChunkGenerator {
    public static final double PILOT_ORIGIN_LATITUDE = 40.9848;
    public static final double PILOT_ORIGIN_LONGITUDE = 29.0268;

    public static final MapCodec<EarthwardChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(EarthwardChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(EarthwardChunkGenerator::generatorSettings),
            Codec.STRING.fieldOf("package_id").forGetter(EarthwardChunkGenerator::packageId)
    ).apply(instance, EarthwardChunkGenerator::new));

    private final String packageId;

    public EarthwardChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> baseSettings,
                                   String packageId) {
        super(biomeSource, Holder.direct(settings(baseSettings.value(), load(packageId))));
        this.packageId = packageId;
    }

    public static EarthwardChunkGenerator create(HolderGetter<Biome> biomes,
                                                  HolderGetter<NoiseGeneratorSettings> noiseSettings,
                                                  String packageId) {
        return new EarthwardChunkGenerator(new FixedBiomeSource(biomes.getOrThrow(Biomes.PLAINS)),
                noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD), packageId);
    }

    private static io.github.thevynex.earthward.geo.ElevationGrid load(String packageId) {
        var inspection = ElevationPackageLoader.inspect(
                FMLPaths.GAMEDIR.get().resolve("earthward").resolve("packages"), packageId);
        if (inspection.status() != ElevationPackageLoader.Status.VERIFIED) {
            throw new IllegalArgumentException("Elevation package is not verified: " + inspection.detail());
        }
        return inspection.grid();
    }

    private static NoiseGeneratorSettings settings(NoiseGeneratorSettings vanilla,
                                                    io.github.thevynex.earthward.geo.ElevationGrid elevation) {
        NoiseRouter router = vanilla.noiseRouter();
        var density = new PilotDensityFunction(elevation,
                new LocalMetricProjection(PILOT_ORIGIN_LATITUDE, PILOT_ORIGIN_LONGITUDE));
        NoiseRouter replacement = new NoiseRouter(router.barrierNoise(), router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(), router.lavaNoise(), router.temperature(), router.vegetation(),
                router.continents(), router.erosion(), router.depth(), router.ridges(),
                router.initialDensityWithoutJaggedness(), density, router.veinToggle(), router.veinRidged(), router.veinGap());
        return new NoiseGeneratorSettings(vanilla.noiseSettings(), vanilla.defaultBlock(), vanilla.defaultFluid(),
                replacement, vanilla.surfaceRule(), vanilla.spawnTarget(), PilotTerrainSampler.SEA_LEVEL_Y,
                vanilla.disableMobGeneration(), false, vanilla.oreVeinsEnabled(), vanilla.useLegacyRandomSource());
    }

    @Override public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState,
                                           StructureManager structureManager, ChunkAccess chunk,
                                           StructureTemplateManager structureTemplateManager) {
        // Vanilla structures are deliberately disabled in the bounded real-world pilot.
    }

    public String packageId() { return packageId; }
    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
}

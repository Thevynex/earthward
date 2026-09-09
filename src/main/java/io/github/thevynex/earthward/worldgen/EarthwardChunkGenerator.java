package io.github.thevynex.earthward.worldgen;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import io.github.thevynex.earthward.geo.ElevationPackageLoader;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.fml.loading.FMLPaths;

/** Serializable bounded pilot generator backed by an installed, hash-verified elevation package. */
public final class EarthwardChunkGenerator extends NoiseBasedChunkGenerator {
    public static final double PILOT_ORIGIN_LATITUDE = 40.9848;
    public static final double PILOT_ORIGIN_LONGITUDE = 29.0268;

    public static final MapCodec<EarthwardChunkGenerator> CODEC = new MapCodec<>() {
        @Override public <T> DataResult<EarthwardChunkGenerator> decode(DynamicOps<T> ops, MapLike<T> input) {
            T value = input.get("package_id");
            if (value == null) return DataResult.error(() -> "Earthward package_id is required");
            Optional<String> packageId = ops.getStringValue(value).result();
            if (packageId.isEmpty() || !(ops instanceof RegistryOps<?> registries)) {
                return DataResult.error(() -> "Earthward generator requires package_id and RegistryOps");
            }
            var biomes = registries.getter(Registries.BIOME);
            var noise = registries.getter(Registries.NOISE_SETTINGS);
            if (biomes.isEmpty() || noise.isEmpty()) return DataResult.error(() -> "Missing worldgen registries");
            try {
                return DataResult.success(new EarthwardChunkGenerator(biomes.get(), noise.get(), packageId.get()));
            } catch (RuntimeException error) {
                return DataResult.error(() -> "Cannot load Earthward elevation package: " + error.getMessage());
            }
        }
        @Override public <T> Stream<T> keys(DynamicOps<T> ops) { return Stream.of(ops.createString("package_id")); }
        @Override public <T> RecordBuilder<T> encode(EarthwardChunkGenerator generator, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return prefix.add("package_id", ops.createString(generator.packageId));
        }
    };

    private final String packageId;

    public EarthwardChunkGenerator(HolderGetter<Biome> biomes,
                                   HolderGetter<NoiseGeneratorSettings> noiseSettings,
                                   String packageId) {
        super(new PilotBiomeSource(biomes), Holder.direct(settings(noiseSettings, load(packageId))));
        this.packageId = packageId;
    }

    private static io.github.thevynex.earthward.geo.ElevationGrid load(String packageId) {
        var inspection = ElevationPackageLoader.inspect(
                FMLPaths.GAMEDIR.get().resolve("earthward").resolve("packages"), packageId);
        if (inspection.status() != ElevationPackageLoader.Status.VERIFIED) {
            throw new IllegalArgumentException("Elevation package is not verified: " + inspection.detail());
        }
        return inspection.grid();
    }

    private static NoiseGeneratorSettings settings(HolderGetter<NoiseGeneratorSettings> noiseSettings,
                                                    io.github.thevynex.earthward.geo.ElevationGrid elevation) {
        NoiseGeneratorSettings vanilla = noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
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

package io.github.thevynex.earthward.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.ElevationGrid;
import io.github.thevynex.earthward.geo.ElevationPackageLoader;
import io.github.thevynex.earthward.geo.GeoPackageLoader;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.fml.loading.FMLPaths;

public final class EarthwardChunkGenerator extends NoiseBasedChunkGenerator {
    public static final double PILOT_ORIGIN_LATITUDE = 40.9848;
    public static final double PILOT_ORIGIN_LONGITUDE = 29.0268;
    public static final MapCodec<EarthwardChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(EarthwardChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(EarthwardChunkGenerator::generatorSettings),
            Codec.STRING.fieldOf("package_id").forGetter(EarthwardChunkGenerator::packageId),
            Codec.INT.fieldOf("requested_spawn_x").forGetter(EarthwardChunkGenerator::requestedSpawnX),
            Codec.INT.fieldOf("requested_spawn_z").forGetter(EarthwardChunkGenerator::requestedSpawnZ)
    ).apply(instance, EarthwardChunkGenerator::new));

    private final String packageId;
    private final int requestedSpawnX;
    private final int requestedSpawnZ;
    private final ChunkPlacementPlanner placementPlanner;

    public EarthwardChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> baseSettings,
                                   String packageId, int requestedSpawnX, int requestedSpawnZ) {
        this(biomeSource, baseSettings, packageId, requestedSpawnX, requestedSpawnZ, prepare(packageId));
    }
    private EarthwardChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> baseSettings,
                                    String packageId, int requestedSpawnX, int requestedSpawnZ, PreparedPackage prepared) {
        super(biomeSource, Holder.direct(settings(baseSettings.value(), prepared.elevation())));
        this.packageId = packageId;
        this.requestedSpawnX = requestedSpawnX;
        this.requestedSpawnZ = requestedSpawnZ;
        this.placementPlanner = prepared.placementPlanner();
    }
    public static EarthwardChunkGenerator create(HolderGetter<Biome> biomes,
            HolderGetter<NoiseGeneratorSettings> noiseSettings, String packageId, int requestedSpawnX, int requestedSpawnZ) {
        return new EarthwardChunkGenerator(new FixedBiomeSource(biomes.getOrThrow(Biomes.PLAINS)),
                noiseSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD), packageId, requestedSpawnX, requestedSpawnZ);
    }
    private static PreparedPackage prepare(String packageId) {
        var root = FMLPaths.GAMEDIR.get().resolve("earthward").resolve("packages");
        var elevation = ElevationPackageLoader.inspect(root, packageId);
        if (elevation.status() != ElevationPackageLoader.Status.VERIFIED) throw new IllegalArgumentException("Elevation package is not verified: " + elevation.detail());
        var geometry = GeoPackageLoader.inspect(root, packageId);
        if (geometry.status() != GeoPackageLoader.Status.VERIFIED_GEOMETRY_ONLY) throw new IllegalArgumentException("Geometry package is not verified: " + geometry.detail());
        return new PreparedPackage(elevation.grid(), new ChunkPlacementPlanner(
                new ChunkFeatureRasterizer(ChunkGeometryIndex.from(geometry.geometry()))));
    }
    private static NoiseGeneratorSettings settings(NoiseGeneratorSettings vanilla, ElevationGrid elevation) {
        NoiseRouter router = vanilla.noiseRouter();
        var density = new PilotDensityFunction(elevation,
                new LocalMetricProjection(PILOT_ORIGIN_LATITUDE, PILOT_ORIGIN_LONGITUDE));
        NoiseRouter replacement = new NoiseRouter(router.barrierNoise(), router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(), router.lavaNoise(), router.temperature(), router.vegetation(),
                router.continents(), router.erosion(), router.depth(), router.ridges(),
                router.initialDensityWithoutJaggedness(), density, router.veinToggle(), router.veinRidged(), router.veinGap());
        return new NoiseGeneratorSettings(vanilla.noiseSettings(), vanilla.defaultBlock(), Blocks.AIR.defaultBlockState(),
                replacement, vanilla.surfaceRule(), vanilla.spawnTarget(), PilotTerrainSampler.SEA_LEVEL_Y,
                vanilla.disableMobGeneration(), false, vanilla.oreVeinsEnabled(), vanilla.useLegacyRandomSource());
    }
    @Override public void createStructures(RegistryAccess access, ChunkGeneratorStructureState state,
            StructureManager manager, ChunkAccess chunk, StructureTemplateManager templates) {}
    @Override public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager manager) {
        ChunkPos pos = chunk.getPos();
        int[] surfaces = new int[256];
        for (int z=0; z<16; z++) for (int x=0; x<16; x++)
            surfaces[z*16+x] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z)-1;
        var plan = placementPlanner.plan(pos.x, pos.z, surfaces);
        int min = chunk.getMinBuildHeight(), max = min + chunk.getHeight()-1;
        for (var placement : plan.placements()) if (placement.y() >= min && placement.y() <= max)
            chunk.setBlockState(new BlockPos(placement.x(), placement.y(), placement.z()), blockFor(placement.material()), false);
    }
    private static BlockState blockFor(ChunkPlacementPlanner.Material material) {
        return switch (material) {
            case ROAD -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case SIDEWALK, FOUNDATION -> Blocks.SMOOTH_STONE.defaultBlockState();
            case WALL -> Blocks.BRICKS.defaultBlockState();
            case WINDOW -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
            case ROOF -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            case LAMP_POST -> Blocks.IRON_BARS.defaultBlockState();
            case LAMP -> Blocks.SEA_LANTERN.defaultBlockState();
        };
    }
    public String packageId() { return packageId; }
    public int requestedSpawnX() { return requestedSpawnX; }
    public int requestedSpawnZ() { return requestedSpawnZ; }
    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
    private record PreparedPackage(ElevationGrid elevation, ChunkPlacementPlanner placementPlanner) {}
}

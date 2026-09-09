package io.github.thevynex.earthward.worldgen;

import io.github.thevynex.earthward.Earthward;
import io.github.thevynex.earthward.geo.ChunkGeometryIndex;
import io.github.thevynex.earthward.geo.ElevationPackageLoader;
import io.github.thevynex.earthward.geo.GeoPackageLoader;
import io.github.thevynex.earthward.geo.LocalMetricProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = Earthward.MOD_ID)
public final class WorldSpawnEvents {
    private WorldSpawnEvents() {}
    @SubscribeEvent public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator() instanceof EarthwardChunkGenerator generator)) return;
        var root = FMLPaths.GAMEDIR.get().resolve("earthward").resolve("packages");
        var elevation = ElevationPackageLoader.inspect(root, generator.packageId());
        var geometry = GeoPackageLoader.inspect(root, generator.packageId());
        if (elevation.status() != ElevationPackageLoader.Status.VERIFIED
                || geometry.status() != GeoPackageLoader.Status.VERIFIED_GEOMETRY_ONLY) return;
        var projection = new LocalMetricProjection(EarthwardChunkGenerator.PILOT_ORIGIN_LATITUDE,
                EarthwardChunkGenerator.PILOT_ORIGIN_LONGITUDE);
        var validator = new SafeSpawnValidator(new ChunkFeatureRasterizer(
                ChunkGeometryIndex.from(geometry.geometry())), (x,z) -> {
            var coordinate = projection.toGeographic(x + 0.5, z + 0.5);
            return PilotTerrainSampler.SEA_LEVEL_Y + (int)Math.round(
                    elevation.grid().sampleMetres(coordinate.longitude(), coordinate.latitude()));
        });
        var spawn = validator.findNearest(generator.requestedSpawnX(), generator.requestedSpawnZ(),
                SafeSpawnValidator.MAX_RADIUS_METRES);
        if (spawn.isEmpty()) return;
        var point = spawn.get();
        event.getSettings().setSpawn(new BlockPos(point.x(), point.y(), point.z()), 0.0F);
        event.setCanceled(true);
    }
}

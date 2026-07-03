package com.skrra.blockreskinner.render;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockRenderOverrideHooks {
    // Touched from chunk-build worker threads.
    private static final Set<Long> LOGGED_RENDER_OVERRIDES = ConcurrentHashMap.newKeySet();
    private static final Set<Long> LOGGED_SODIUM_RENDER_OVERRIDES = ConcurrentHashMap.newKeySet();

    private BlockRenderOverrideHooks() {
    }

    public static BlockState resolve(BlockState state, BlockPos pos, BlockRenderView world) {
        return VisualStateResolver.resolve(state, pos, world);
    }

    public static void logRenderOverride(BlockPos pos, BlockState realState, BlockState resolvedState) {
        if (realState == resolvedState || realState.equals(resolvedState)) {
            return;
        }
        long key = pos.asLong();
        if (LOGGED_RENDER_OVERRIDES.add(key)) {
            BlockReskinnerMod.LOGGER.info(
                    "Render override resolved: pos={} real={} visual={}",
                    pos.toShortString(),
                    realState,
                    resolvedState
            );
        }
    }

    public static void logSodiumRenderOverride(BlockPos pos, BlockState realState, BlockState resolvedState) {
        if (realState == resolvedState || realState.equals(resolvedState)) {
            return;
        }
        long key = pos.asLong();
        if (LOGGED_SODIUM_RENDER_OVERRIDES.add(key)) {
            BlockReskinnerMod.LOGGER.info(
                    "Sodium render override resolved: pos={} real={} visual={}",
                    pos.toShortString(),
                    realState,
                    resolvedState
            );
        }
    }

    public static void clearRenderLog(BlockPos pos) {
        LOGGED_RENDER_OVERRIDES.remove(pos.asLong());
        LOGGED_SODIUM_RENDER_OVERRIDES.remove(pos.asLong());
    }

    /**
     * Lightmap coordinates for a skinned position. The world's light engine
     * stores 0 light inside the real (usually opaque) block, so cross-model
     * visuals like plants and amethyst clusters — whose quads sample light at
     * the block's own position instead of a face-offset neighbor — rendered
     * black. When the stored light at the position is fully dark, approximate
     * the light the visual would receive if the block were transparent by
     * taking the brightest neighbor. Light storage itself is never touched;
     * this only changes what the chunk mesher samples for this position.
     */
    public static int skinnedLightmapCoordinates(BlockRenderView world, BlockState visualState, BlockPos pos) {
        if (visualState.hasEmissiveLighting(world, pos)) {
            return LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
        int sky = world.getLightLevel(LightType.SKY, pos);
        int block = world.getLightLevel(LightType.BLOCK, pos);
        if (sky == 0 && block == 0) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.offset(direction);
                sky = Math.max(sky, world.getLightLevel(LightType.SKY, neighbor));
                block = Math.max(block, world.getLightLevel(LightType.BLOCK, neighbor));
            }
        }
        block = Math.max(block, visualState.getLuminance());
        return LightmapTextureManager.pack(block, sky);
    }
}

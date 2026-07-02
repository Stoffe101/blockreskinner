package com.skrra.blockreskinner.render;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

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
}

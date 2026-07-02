package com.skrra.blockreskinner.render;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public final class BlockRenderOverrideHooks {
    private BlockRenderOverrideHooks() {
    }

    public static BlockState resolve(BlockState state, BlockPos pos, BlockRenderView world) {
        return VisualStateResolver.resolve(state, pos, world);
    }
}

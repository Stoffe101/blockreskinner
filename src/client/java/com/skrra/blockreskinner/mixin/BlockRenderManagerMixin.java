package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {
    /*
     * Vanilla chunk rendering calls BlockRenderManager with the real world state.
     * This hook swaps only that render argument. The world/server state remains
     * unchanged, so mining, drops, portals, explosions, pathing, and block logic
     * continue to use the original block.
     */
    @ModifyVariable(
            method = "renderBlock",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private BlockState blockreskinner$useVisualState(BlockState state, BlockState original, BlockPos pos, BlockRenderView world, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, List<BlockModelPart> parts) {
        return BlockRenderOverrideHooks.resolve(state, pos, world);
    }
}

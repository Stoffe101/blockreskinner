package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRendererRegion.class)
public abstract class ChunkRendererRegionMixin {
    /*
     * ChunkRendererRegion is a render-only snapshot used while building chunk
     * meshes. Overriding getBlockState here swaps the visual state for every
     * query made during section building: the block's own model and render
     * layer, neighbor face culling (Block.shouldDrawSide), and ambient
     * occlusion all see the same visual state. The real world state is never
     * touched, so block behavior (mining, drops, collision, portals) stays
     * driven by the original block.
     */
    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void blockreskinner$resolveVisualState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState real = cir.getReturnValue();
        BlockState visual = BlockRenderOverrideHooks.resolve(real, pos, (ChunkRendererRegion) (Object) this);
        if (visual != real) {
            BlockRenderOverrideHooks.logRenderOverride(pos, real, visual);
            cir.setReturnValue(visual);
        }
    }
}

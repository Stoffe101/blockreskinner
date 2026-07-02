package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionBuilder.class)
public abstract class SectionBuilderMixin {
    /*
     * SectionBuilder is where vanilla chunk rendering extracts the block state,
     * render layer, model, and model parts. Replacing the state here changes the
     * rendered model without mutating the actual client or server world state.
     */
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
            )
    )
    private BlockState blockreskinner$useVisualStateForChunkRender(ChunkRendererRegion region, BlockPos pos) {
        BlockState realState = region.getBlockState(pos);
        BlockState visualState = BlockRenderOverrideHooks.resolve(realState, pos, region);
        BlockRenderOverrideHooks.logRenderOverride(pos, realState, visualState);
        return visualState;
    }
}

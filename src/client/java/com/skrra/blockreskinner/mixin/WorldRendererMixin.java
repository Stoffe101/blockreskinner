package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    /*
     * Chunk meshing samples light through this static helper (via
     * BlockModelRenderer's brightness cache). Quads without a cull face —
     * cross models such as plants, saplings and amethyst clusters — sample at
     * the block's own position, where the real (opaque) block stores 0 light,
     * turning the visual black. For skinned positions we substitute a light
     * value appropriate for the visual state; see
     * BlockRenderOverrideHooks.skinnedLightmapCoordinates.
     */
    @Inject(
            method = "getLightmapCoordinates(Lnet/minecraft/client/render/WorldRenderer$BrightnessGetter;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void blockreskinner$skinnedBlockLight(WorldRenderer.BrightnessGetter brightnessGetter, BlockRenderView world,
                                                         BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (ClientSkinCache.get(pos) != null) {
            cir.setReturnValue(BlockRenderOverrideHooks.skinnedLightmapCoordinates(world, state, pos));
        }
    }
}

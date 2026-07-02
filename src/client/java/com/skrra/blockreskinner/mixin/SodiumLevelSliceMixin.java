package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class SodiumLevelSliceMixin {
    private static final ThreadLocal<BlockPos.Mutable> BLOCKRESKINNER_POS =
            ThreadLocal.withInitial(() -> new BlockPos.Mutable(0, 0, 0));

    /*
     * Sodium builds chunk meshes from LevelSlice instead of vanilla's
     * ChunkRendererRegion. Returning the visual state here makes Sodium's model
     * lookup, face culling, AO, and render pass selection see the same
     * render-only block state as the vanilla/Fabric path.
     */
    @Inject(method = "getBlockState(III)Lnet/minecraft/class_2680;", at = @At("RETURN"), cancellable = true, remap = false)
    private void blockreskinner$resolveSodiumVisualState(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        if (ClientSkinCache.isEmpty()) {
            return;
        }
        BlockPos.Mutable pos = BLOCKRESKINNER_POS.get();
        pos.set(x, y, z);
        BlockState real = cir.getReturnValue();
        BlockState visual = BlockRenderOverrideHooks.resolve(real, pos, null);
        if (visual != real) {
            BlockRenderOverrideHooks.logSodiumRenderOverride(pos, real, visual);
            cir.setReturnValue(visual);
        }
    }
}

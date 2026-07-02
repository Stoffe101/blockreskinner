package com.skrra.blockreskinner.mixin;

import com.skrra.blockreskinner.networking.ModNetworking;
import com.skrra.blockreskinner.skin.ServerSkinStorage;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @Inject(method = "onBlockStateChanged", at = @At("TAIL"))
    private void blockreskinner$clearSkinWhenRealBlockChanges(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (oldState == newState) {
            return;
        }
        ServerWorld world = (ServerWorld) (Object) this;
        if (ServerSkinStorage.get(world).remove(pos)) {
            ModNetworking.syncRemove(world, pos);
        }
    }
}

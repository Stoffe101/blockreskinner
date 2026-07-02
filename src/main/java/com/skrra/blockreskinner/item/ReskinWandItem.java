package com.skrra.blockreskinner.item;

import com.skrra.blockreskinner.networking.ModNetworking;
import com.skrra.blockreskinner.networking.payload.OpenSkinScreenPayload;
import com.skrra.blockreskinner.networking.payload.RemoveSkinPayload;
import com.skrra.blockreskinner.registry.ModItems;
import com.skrra.blockreskinner.skin.ServerSkinStorage;
import com.skrra.blockreskinner.skin.SkinQueries;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class ReskinWandItem extends Item {
    public ReskinWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld world) || !(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos();
        if (!player.getStackInHand(context.getHand()).isOf(ModItems.RESKIN_WAND)) {
            return ActionResult.FAIL;
        }

        if (player.isSneaking()) {
            boolean removed = ServerSkinStorage.get(world).remove(pos);
            if (removed) {
                ModNetworking.syncRemove(world, pos);
                player.sendMessage(Text.translatable("message.blockreskinner.cleared"), true);
            }
            return ActionResult.SUCCESS_SERVER;
        }

        if (!SkinQueries.isSupportedTarget(world.getBlockState(pos))) {
            player.sendMessage(Text.translatable("message.blockreskinner.unsupported"), true);
            return ActionResult.SUCCESS_SERVER;
        }

        ServerPlayNetworking.send(player, new OpenSkinScreenPayload(pos, SkinQueries.isConnectedBlock(world.getBlockState(pos))));
        return ActionResult.SUCCESS_SERVER;
    }
}

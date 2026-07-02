package com.skrra.blockreskinner.item;

import com.skrra.blockreskinner.networking.ModNetworking;
import com.skrra.blockreskinner.networking.payload.OpenSkinScreenPayload;
import com.skrra.blockreskinner.networking.payload.RemoveSkinPayload;
import com.skrra.blockreskinner.registry.ModItems;
import com.skrra.blockreskinner.skin.ServerSkinStorage;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.util.ConnectedBlockUtil;
import net.minecraft.block.BlockState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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

        BlockState state = world.getBlockState(pos);
        if (!SkinQueries.isSupportedTarget(state)) {
            player.sendMessage(Text.translatable("message.blockreskinner.unsupported"), true);
            return ActionResult.SUCCESS_SERVER;
        }

        boolean connected = SkinQueries.isConnectedBlock(state);
        ServerPlayNetworking.send(player, new OpenSkinScreenPayload(
                pos,
                connected,
                connected && ConnectedBlockUtil.isConnected(state, Direction.NORTH),
                connected && ConnectedBlockUtil.isConnected(state, Direction.EAST),
                connected && ConnectedBlockUtil.isConnected(state, Direction.SOUTH),
                connected && ConnectedBlockUtil.isConnected(state, Direction.WEST)
        ));
        return ActionResult.SUCCESS_SERVER;
    }
}

package com.skrra.blockreskinner.skin;

import com.skrra.blockreskinner.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class SkinValidation {
    private static final double MAX_DISTANCE_SQUARED = 8.0D * 8.0D;

    private SkinValidation() {
    }

    public static boolean canEdit(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!player.getMainHandStack().isOf(ModItems.RESKIN_WAND) && !player.getOffHandStack().isOf(ModItems.RESKIN_WAND)) {
            player.sendMessage(Text.translatable("message.blockreskinner.need_wand"), true);
            return false;
        }
        if (player.squaredDistanceTo(pos.toCenterPos()) > MAX_DISTANCE_SQUARED) {
            player.sendMessage(Text.translatable("message.blockreskinner.too_far"), true);
            return false;
        }
        if (!world.isChunkLoaded(pos)) {
            return false;
        }
        BlockState target = world.getBlockState(pos);
        if (!SkinQueries.isSupportedTarget(target)) {
            player.sendMessage(Text.translatable("message.blockreskinner.unsupported"), true);
            return false;
        }
        return true;
    }

    public static boolean canApplySimple(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState visual) {
        if (!canEdit(player, world, pos)) {
            return false;
        }
        if (SkinQueries.isConnectedBlock(world.getBlockState(pos)) || !SkinQueries.isAllowedSimpleVisual(visual)) {
            player.sendMessage(Text.translatable("message.blockreskinner.invalid_skin"), true);
            return false;
        }
        return true;
    }

    public static boolean canApplyConnected(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState visual) {
        if (!canEdit(player, world, pos)) {
            return false;
        }
        if (!SkinQueries.isAllowedConnectedVisual(world.getBlockState(pos), visual)) {
            player.sendMessage(Text.translatable("message.blockreskinner.invalid_skin"), true);
            return false;
        }
        return true;
    }
}

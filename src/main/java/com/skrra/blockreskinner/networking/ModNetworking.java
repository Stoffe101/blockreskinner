package com.skrra.blockreskinner.networking;

import com.skrra.blockreskinner.networking.payload.ApplyConnectedSkinPayload;
import com.skrra.blockreskinner.networking.payload.ApplySimpleSkinPayload;
import com.skrra.blockreskinner.networking.payload.ClearSkinPayload;
import com.skrra.blockreskinner.networking.payload.OpenSkinScreenPayload;
import com.skrra.blockreskinner.networking.payload.RemoveSkinPayload;
import com.skrra.blockreskinner.networking.payload.RequestInitialSyncPayload;
import com.skrra.blockreskinner.networking.payload.SyncSkinPayload;
import com.skrra.blockreskinner.BlockReskinnerMod;
import com.skrra.blockreskinner.skin.ConnectedSkinData;
import com.skrra.blockreskinner.skin.ServerSkinStorage;
import com.skrra.blockreskinner.skin.SimpleSkinData;
import com.skrra.blockreskinner.skin.SkinData;
import com.skrra.blockreskinner.skin.SkinQueries;
import com.skrra.blockreskinner.skin.SkinValidation;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(ApplySimpleSkinPayload.ID, ApplySimpleSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyConnectedSkinPayload.ID, ApplyConnectedSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClearSkinPayload.ID, ClearSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestInitialSyncPayload.ID, RequestInitialSyncPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(OpenSkinScreenPayload.ID, OpenSkinScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSkinPayload.ID, SyncSkinPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveSkinPayload.ID, RemoveSkinPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ApplySimpleSkinPayload.ID, (payload, context) ->
                context.server().execute(() -> handleApplySimple(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ApplyConnectedSkinPayload.ID, (payload, context) ->
                context.server().execute(() -> handleApplyConnected(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ClearSkinPayload.ID, (payload, context) ->
                context.server().execute(() -> clear(context.player(), payload.pos(), true)));
        ServerPlayNetworking.registerGlobalReceiver(RequestInitialSyncPayload.ID, (payload, context) ->
                context.server().execute(() -> syncInitial(context.player())));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> syncInitial(handler.player)));
    }

    private static void handleApplySimple(ServerPlayerEntity player, ApplySimpleSkinPayload payload) {
        ServerWorld world = player.getEntityWorld();
        BlockState visual = BlockStateUtil.parse(payload.visualState());
        if (visual == null || !SkinValidation.canApplySimple(player, world, payload.pos(), visual)) {
            return;
        }
        BlockReskinnerMod.LOGGER.info(
                "Server applying simple skin: dimension={} pos={} real={} visual={}",
                world.getRegistryKey().getValue(),
                payload.pos().toShortString(),
                world.getBlockState(payload.pos()),
                visual
        );
        SimpleSkinData data = new SimpleSkinData(payload.pos(), visual);
        ServerSkinStorage.get(world).put(data);
        syncSkin(world, data);
        player.sendMessage(Text.translatable("message.blockreskinner.applied", visual.getBlock().getName()), true);
    }

    private static void handleApplyConnected(ServerPlayerEntity player, ApplyConnectedSkinPayload payload) {
        ServerWorld world = player.getEntityWorld();
        BlockState visual = BlockStateUtil.parse(payload.visualState());
        if (visual == null || !SkinValidation.canApplyConnected(player, world, payload.pos(), visual)) {
            return;
        }
        BlockReskinnerMod.LOGGER.info(
                "Server applying connected skin: dimension={} pos={} real={} visual={} north={} east={} south={} west={}",
                world.getRegistryKey().getValue(),
                payload.pos().toShortString(),
                world.getBlockState(payload.pos()),
                visual,
                payload.north(),
                payload.east(),
                payload.south(),
                payload.west()
        );
        ConnectedSkinData data = new ConnectedSkinData(payload.pos(), visual, payload.north(), payload.east(), payload.south(), payload.west());
        ServerSkinStorage.get(world).put(data);
        syncSkin(world, data);
        player.sendMessage(Text.translatable("message.blockreskinner.applied", visual.getBlock().getName()), true);
    }

    public static void clear(ServerPlayerEntity player, BlockPos pos, boolean feedback) {
        ServerWorld world = player.getEntityWorld();
        if (!SkinValidation.canEdit(player, world, pos)) {
            return;
        }
        if (ServerSkinStorage.get(world).remove(pos)) {
            syncRemove(world, pos);
            if (feedback) {
                player.sendMessage(Text.translatable("message.blockreskinner.cleared"), true);
            }
        }
    }

    public static void syncSkin(ServerWorld world, SkinData data) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (ServerPlayNetworking.canSend(player, SyncSkinPayload.ID)) {
                ServerPlayNetworking.send(player, new SyncSkinPayload(data));
            }
        }
    }

    public static void syncRemove(ServerWorld world, BlockPos pos) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (ServerPlayNetworking.canSend(player, RemoveSkinPayload.ID)) {
                ServerPlayNetworking.send(player, new RemoveSkinPayload(pos));
            }
        }
    }

    private static void syncInitial(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        ServerSkinStorage storage = ServerSkinStorage.get(world);
        List<BlockPos> invalidEntries = new ArrayList<>();
        for (SkinData data : storage.all()) {
            if (!isStoredSkinStillApplicable(world, data)) {
                invalidEntries.add(data.pos());
                continue;
            }
            if (ServerPlayNetworking.canSend(player, SyncSkinPayload.ID)) {
                ServerPlayNetworking.send(player, new SyncSkinPayload(data));
            }
        }
        for (BlockPos pos : invalidEntries) {
            if (storage.remove(pos)) {
                BlockReskinnerMod.LOGGER.info(
                        "Removed stale visual skin during initial sync: dimension={} pos={}",
                        world.getRegistryKey().getValue(),
                        pos.toShortString()
                );
            }
        }
    }

    private static boolean isStoredSkinStillApplicable(ServerWorld world, SkinData data) {
        if (!world.isChunkLoaded(data.pos())) {
            return true;
        }
        BlockState real = world.getBlockState(data.pos());
        if (!SkinQueries.isSupportedTarget(real)) {
            return false;
        }
        if (data instanceof SimpleSkinData simple) {
            return !SkinQueries.isConnectedBlock(real) && SkinQueries.isAllowedSimpleVisual(simple.visualState());
        }
        if (data instanceof ConnectedSkinData connected) {
            return SkinQueries.isAllowedConnectedVisual(real, connected.visualMaterialState());
        }
        return false;
    }
}

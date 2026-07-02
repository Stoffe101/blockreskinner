package com.skrra.blockreskinner;

import com.skrra.blockreskinner.networking.payload.OpenSkinScreenPayload;
import com.skrra.blockreskinner.networking.payload.RemoveSkinPayload;
import com.skrra.blockreskinner.networking.payload.RequestInitialSyncPayload;
import com.skrra.blockreskinner.networking.payload.SyncSkinPayload;
import com.skrra.blockreskinner.screen.BlockSkinScreen;
import com.skrra.blockreskinner.screen.ConnectedBlockSkinScreen;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class BlockReskinnerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenSkinScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.connected()) {
                        context.client().setScreen(new ConnectedBlockSkinScreen(payload.pos()));
                    } else {
                        context.client().setScreen(new BlockSkinScreen(payload.pos()));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SyncSkinPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientSkinCache.put(payload.data());
                    rerender(payload.data().pos());
                }));

        ClientPlayNetworking.registerGlobalReceiver(RemoveSkinPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientSkinCache.remove(payload.pos());
                    rerender(payload.pos());
                }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientSkinCache.clear();
            if (ClientPlayNetworking.canSend(RequestInitialSyncPayload.ID)) {
                ClientPlayNetworking.send(new RequestInitialSyncPayload());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientSkinCache.clear());
    }

    private static void rerender(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.worldRenderer != null) {
            BlockState state = client.world.getBlockState(pos);
            client.worldRenderer.scheduleBlockRerenderIfNeeded(pos, state, state);
        }
    }
}

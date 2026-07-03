package com.skrra.blockreskinner;

import com.skrra.blockreskinner.networking.payload.OpenSkinScreenPayload;
import com.skrra.blockreskinner.networking.payload.RemoveSkinPayload;
import com.skrra.blockreskinner.networking.payload.RequestInitialSyncPayload;
import com.skrra.blockreskinner.networking.payload.SyncSkinPayload;
import com.skrra.blockreskinner.screen.BlockSkinScreen;
import com.skrra.blockreskinner.screen.ConnectedBlockSkinScreen;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import com.skrra.blockreskinner.skin.ConnectedSkinData;
import com.skrra.blockreskinner.skin.SimpleSkinData;
import com.skrra.blockreskinner.render.BlockPreviewGuiElementRenderer;
import com.skrra.blockreskinner.render.BlockRenderOverrideHooks;
import com.skrra.blockreskinner.render.ClientRenderRefresh;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class BlockReskinnerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialGuiElementRegistry.register(ctx -> new BlockPreviewGuiElementRenderer(ctx.vertexConsumers()));

        ClientPlayNetworking.registerGlobalReceiver(OpenSkinScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.connected()) {
                        context.client().setScreen(new ConnectedBlockSkinScreen(
                                payload.pos(),
                                payload.northConnected(),
                                payload.eastConnected(),
                                payload.southConnected(),
                                payload.westConnected()
                        ));
                    } else {
                        context.client().setScreen(new BlockSkinScreen(payload.pos()));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SyncSkinPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientSkinCache.put(payload.data());
                    BlockReskinnerMod.LOGGER.info(
                            "Client received skin sync: dimension={} pos={} visual={}",
                            context.client().world == null ? "unknown" : context.client().world.getRegistryKey().getValue(),
                            payload.data().pos().toShortString(),
                            visualStateForLog(payload.data())
                    );
                    rerender(payload.data().pos());
                }));

        ClientPlayNetworking.registerGlobalReceiver(RemoveSkinPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ClientSkinCache.remove(payload.pos());
                    BlockRenderOverrideHooks.clearRenderLog(payload.pos());
                    BlockReskinnerMod.LOGGER.info(
                            "Client received skin removal: dimension={} pos={}",
                            context.client().world == null ? "unknown" : context.client().world.getRegistryKey().getValue(),
                            payload.pos().toShortString()
                    );
                    rerender(payload.pos());
                }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientSkinCache.clear();
            if (ClientPlayNetworking.canSend(RequestInitialSyncPayload.ID)) {
                ClientPlayNetworking.send(new RequestInitialSyncPayload());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientSkinCache.clear();
            com.skrra.blockreskinner.render.head.PlayerHeadProfiles.clear();
        });
    }

    private static void rerender(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientRenderRefresh.requestBlockRenderUpdate(client, pos);
    }

    private static BlockState visualStateForLog(com.skrra.blockreskinner.skin.SkinData data) {
        if (data instanceof SimpleSkinData simple) {
            return simple.visualState();
        }
        if (data instanceof ConnectedSkinData connected) {
            return connected.visualMaterialState();
        }
        return null;
    }
}

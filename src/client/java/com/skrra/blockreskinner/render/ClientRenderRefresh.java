package com.skrra.blockreskinner.render;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public final class ClientRenderRefresh {
    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");
    private static boolean loggedSodiumStatus;
    private static boolean loggedSodiumFailure;

    private ClientRenderRefresh() {
    }

    public static void requestBlockRenderUpdate(MinecraftClient client, BlockPos pos) {
        if (client.world == null || client.worldRenderer == null) {
            return;
        }

        client.worldRenderer.scheduleBlockRenders(
                pos.getX() - 1,
                pos.getY() - 1,
                pos.getZ() - 1,
                pos.getX() + 1,
                pos.getY() + 1,
                pos.getZ() + 1
        );
        logSodiumStatus();
        if (SODIUM_LOADED) {
            requestSodiumRenderUpdate(pos);
        }
        BlockReskinnerMod.LOGGER.info("Requested block render update: pos={}", pos.toShortString());
    }

    public static boolean isSodiumLoaded() {
        return SODIUM_LOADED;
    }

    private static void logSodiumStatus() {
        if (!loggedSodiumStatus) {
            loggedSodiumStatus = true;
            BlockReskinnerMod.LOGGER.info("Sodium detected for Block Reskinner rendering: {}", SODIUM_LOADED);
        }
    }

    private static void requestSodiumRenderUpdate(BlockPos pos) {
        try {
            Class<?> rendererClass = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
            Method instanceNullable = rendererClass.getMethod("instanceNullable");
            Object renderer = instanceNullable.invoke(null);
            if (renderer == null) {
                return;
            }
            Method rebuildArea = rendererClass.getMethod(
                    "scheduleRebuildForBlockArea",
                    int.class, int.class, int.class,
                    int.class, int.class, int.class,
                    boolean.class
            );
            rebuildArea.invoke(
                    renderer,
                    pos.getX() - 1,
                    pos.getY() - 1,
                    pos.getZ() - 1,
                    pos.getX() + 1,
                    pos.getY() + 1,
                    pos.getZ() + 1,
                    true
            );
            BlockReskinnerMod.LOGGER.info("Requested Sodium chunk rebuild: pos={}", pos.toShortString());
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!loggedSodiumFailure) {
                loggedSodiumFailure = true;
                BlockReskinnerMod.LOGGER.warn("Sodium was detected, but Block Reskinner could not request a Sodium chunk rebuild", e);
            }
        }
    }
}

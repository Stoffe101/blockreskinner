package com.skrra.blockreskinner.render.head;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.component.type.ProfileComponent;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side player head skin lookups, built entirely on the vanilla
 * PlayerSkinCache: name-based ProfileComponents are resolved and their skin
 * textures fetched asynchronously by vanilla, while {@code get()} always
 * returns immediately (default Steve skin until resolved). This class only
 * memoizes the suppliers per lowercase name and tracks a resolve status for
 * the GUI/Jade. Nothing here ever blocks the render thread.
 */
public final class PlayerHeadProfiles {
    public enum Status {
        RESOLVING,
        RESOLVED,
        FAILED
    }

    private static final Map<String, java.util.function.Supplier<PlayerSkinCache.Entry>> SUPPLIERS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Optional<PlayerSkinCache.Entry>>> FUTURES = new ConcurrentHashMap<>();

    private PlayerHeadProfiles() {
    }

    /** Render layer for the given player's head; default player skin while unresolved. */
    public static RenderLayer renderLayer(String playerName) {
        String key = key(playerName);
        if (key.isEmpty()) {
            return PlayerSkinCache.DEFAULT_RENDER_LAYER;
        }
        PlayerSkinCache.Entry entry = SUPPLIERS
                .computeIfAbsent(key, name -> MinecraftClient.getInstance().getPlayerSkinCache()
                        .getSupplier(ProfileComponent.ofDynamic(name)))
                .get();
        return entry != null ? entry.getRenderLayer() : PlayerSkinCache.DEFAULT_RENDER_LAYER;
    }

    /** Non-blocking resolve status for GUI/Jade display. */
    public static Status status(String playerName) {
        String key = key(playerName);
        if (key.isEmpty()) {
            return Status.FAILED;
        }
        CompletableFuture<Optional<PlayerSkinCache.Entry>> future = FUTURES.computeIfAbsent(key,
                name -> MinecraftClient.getInstance().getPlayerSkinCache().getFuture(ProfileComponent.ofDynamic(name)));
        if (!future.isDone()) {
            return Status.RESOLVING;
        }
        if (future.isCompletedExceptionally()) {
            return Status.FAILED;
        }
        return future.join().isPresent() ? Status.RESOLVED : Status.FAILED;
    }

    /** Drop memoized lookups (e.g. on disconnect) so stale failures can retry. */
    public static void clear() {
        SUPPLIERS.clear();
        FUTURES.clear();
    }

    private static String key(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }
}

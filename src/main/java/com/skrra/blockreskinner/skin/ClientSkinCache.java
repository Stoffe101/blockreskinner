package com.skrra.blockreskinner.skin;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSkinCache {
    // Read from chunk-build worker threads while the network thread mutates it.
    private static final Map<Long, SkinData> SKINS = new ConcurrentHashMap<>();

    private ClientSkinCache() {
    }

    public static SkinData get(BlockPos pos) {
        return SKINS.get(pos.asLong());
    }

    /** Live view of all cached skins; safe to iterate from the render thread. */
    public static Collection<SkinData> all() {
        return SKINS.values();
    }

    public static boolean isEmpty() {
        return SKINS.isEmpty();
    }

    public static void put(SkinData data) {
        SKINS.put(data.pos().asLong(), data);
    }

    public static void putAll(Collection<SkinData> data) {
        SKINS.clear();
        for (SkinData skin : data) {
            put(skin);
        }
    }

    public static void remove(BlockPos pos) {
        SKINS.remove(pos.asLong());
    }

    public static void clear() {
        SKINS.clear();
    }
}

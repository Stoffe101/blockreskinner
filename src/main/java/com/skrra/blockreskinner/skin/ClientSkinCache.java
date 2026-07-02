package com.skrra.blockreskinner.skin;

import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ClientSkinCache {
    private static final Map<Long, SkinData> SKINS = new HashMap<>();

    private ClientSkinCache() {
    }

    public static SkinData get(BlockPos pos) {
        return SKINS.get(pos.asLong());
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

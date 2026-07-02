package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import com.skrra.blockreskinner.skin.SkinCodecs;
import com.skrra.blockreskinner.skin.SkinData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SyncSkinPayload(SkinData data) implements CustomPayload {
    public static final Id<SyncSkinPayload> ID = new Id<>(BlockReskinnerMod.id("sync_skin"));
    public static final PacketCodec<RegistryByteBuf, SyncSkinPayload> CODEC = PacketCodec.ofStatic(SyncSkinPayload::write, SyncSkinPayload::read);

    private static void write(RegistryByteBuf buf, SyncSkinPayload payload) {
        SkinCodecs.writeSkin(buf, payload.data);
    }

    private static SyncSkinPayload read(RegistryByteBuf buf) {
        return new SyncSkinPayload(SkinCodecs.readSkin(buf));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

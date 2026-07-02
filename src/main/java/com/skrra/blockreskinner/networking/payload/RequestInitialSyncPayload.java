package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RequestInitialSyncPayload() implements CustomPayload {
    public static final Id<RequestInitialSyncPayload> ID = new Id<>(BlockReskinnerMod.id("request_initial_sync"));
    public static final PacketCodec<RegistryByteBuf, RequestInitialSyncPayload> CODEC = PacketCodec.unit(new RequestInitialSyncPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

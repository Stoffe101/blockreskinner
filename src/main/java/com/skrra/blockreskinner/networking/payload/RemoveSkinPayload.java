package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record RemoveSkinPayload(BlockPos pos) implements CustomPayload {
    public static final Id<RemoveSkinPayload> ID = new Id<>(BlockReskinnerMod.id("remove_skin"));
    public static final PacketCodec<RegistryByteBuf, RemoveSkinPayload> CODEC = PacketCodec.ofStatic(RemoveSkinPayload::write, RemoveSkinPayload::read);

    private static void write(RegistryByteBuf buf, RemoveSkinPayload payload) {
        buf.writeBlockPos(payload.pos);
    }

    private static RemoveSkinPayload read(RegistryByteBuf buf) {
        return new RemoveSkinPayload(buf.readBlockPos());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

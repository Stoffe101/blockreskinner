package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record ClearSkinPayload(BlockPos pos) implements CustomPayload {
    public static final Id<ClearSkinPayload> ID = new Id<>(BlockReskinnerMod.id("clear_skin"));
    public static final PacketCodec<RegistryByteBuf, ClearSkinPayload> CODEC = PacketCodec.ofStatic(ClearSkinPayload::write, ClearSkinPayload::read);

    private static void write(RegistryByteBuf buf, ClearSkinPayload payload) {
        buf.writeBlockPos(payload.pos);
    }

    private static ClearSkinPayload read(RegistryByteBuf buf) {
        return new ClearSkinPayload(buf.readBlockPos());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

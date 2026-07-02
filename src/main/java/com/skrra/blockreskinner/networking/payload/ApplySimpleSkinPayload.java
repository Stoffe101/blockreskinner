package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record ApplySimpleSkinPayload(BlockPos pos, String visualState) implements CustomPayload {
    public static final Id<ApplySimpleSkinPayload> ID = new Id<>(BlockReskinnerMod.id("apply_simple_skin"));
    public static final PacketCodec<RegistryByteBuf, ApplySimpleSkinPayload> CODEC = PacketCodec.ofStatic(ApplySimpleSkinPayload::write, ApplySimpleSkinPayload::read);

    private static void write(RegistryByteBuf buf, ApplySimpleSkinPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeString(payload.visualState);
    }

    private static ApplySimpleSkinPayload read(RegistryByteBuf buf) {
        return new ApplySimpleSkinPayload(buf.readBlockPos(), buf.readString(256));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

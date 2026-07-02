package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record OpenSkinScreenPayload(BlockPos pos, boolean connected) implements CustomPayload {
    public static final Id<OpenSkinScreenPayload> ID = new Id<>(BlockReskinnerMod.id("open_skin_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenSkinScreenPayload> CODEC = PacketCodec.ofStatic(OpenSkinScreenPayload::write, OpenSkinScreenPayload::read);

    private static void write(RegistryByteBuf buf, OpenSkinScreenPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeBoolean(payload.connected);
    }

    private static OpenSkinScreenPayload read(RegistryByteBuf buf) {
        return new OpenSkinScreenPayload(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

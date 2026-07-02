package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record OpenSkinScreenPayload(
        BlockPos pos,
        boolean connected,
        boolean northConnected,
        boolean eastConnected,
        boolean southConnected,
        boolean westConnected
) implements CustomPayload {
    public static final Id<OpenSkinScreenPayload> ID = new Id<>(BlockReskinnerMod.id("open_skin_screen"));
    public static final PacketCodec<RegistryByteBuf, OpenSkinScreenPayload> CODEC = PacketCodec.ofStatic(OpenSkinScreenPayload::write, OpenSkinScreenPayload::read);

    private static void write(RegistryByteBuf buf, OpenSkinScreenPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeBoolean(payload.connected);
        buf.writeBoolean(payload.northConnected);
        buf.writeBoolean(payload.eastConnected);
        buf.writeBoolean(payload.southConnected);
        buf.writeBoolean(payload.westConnected);
    }

    private static OpenSkinScreenPayload read(RegistryByteBuf buf) {
        return new OpenSkinScreenPayload(
                buf.readBlockPos(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

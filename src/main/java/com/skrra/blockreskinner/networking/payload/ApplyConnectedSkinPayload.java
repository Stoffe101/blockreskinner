package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import com.skrra.blockreskinner.skin.ConnectionOverride;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record ApplyConnectedSkinPayload(
        BlockPos pos,
        String visualState,
        ConnectionOverride north,
        ConnectionOverride east,
        ConnectionOverride south,
        ConnectionOverride west
) implements CustomPayload {
    public static final Id<ApplyConnectedSkinPayload> ID = new Id<>(BlockReskinnerMod.id("apply_connected_skin"));
    public static final PacketCodec<RegistryByteBuf, ApplyConnectedSkinPayload> CODEC = PacketCodec.ofStatic(ApplyConnectedSkinPayload::write, ApplyConnectedSkinPayload::read);

    private static void write(RegistryByteBuf buf, ApplyConnectedSkinPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeString(payload.visualState);
        buf.writeEnumConstant(payload.north);
        buf.writeEnumConstant(payload.east);
        buf.writeEnumConstant(payload.south);
        buf.writeEnumConstant(payload.west);
    }

    private static ApplyConnectedSkinPayload read(RegistryByteBuf buf) {
        return new ApplyConnectedSkinPayload(
                buf.readBlockPos(),
                buf.readString(256),
                buf.readEnumConstant(ConnectionOverride.class),
                buf.readEnumConstant(ConnectionOverride.class),
                buf.readEnumConstant(ConnectionOverride.class),
                buf.readEnumConstant(ConnectionOverride.class)
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

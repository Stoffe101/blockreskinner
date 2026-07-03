package com.skrra.blockreskinner.networking.payload;

import com.skrra.blockreskinner.BlockReskinnerMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record ApplyPlayerHeadSkinPayload(BlockPos pos, String playerName, int rotation) implements CustomPayload {
    public static final Id<ApplyPlayerHeadSkinPayload> ID = new Id<>(BlockReskinnerMod.id("apply_player_head_skin"));
    public static final PacketCodec<RegistryByteBuf, ApplyPlayerHeadSkinPayload> CODEC = PacketCodec.ofStatic(ApplyPlayerHeadSkinPayload::write, ApplyPlayerHeadSkinPayload::read);

    private static void write(RegistryByteBuf buf, ApplyPlayerHeadSkinPayload payload) {
        buf.writeBlockPos(payload.pos);
        buf.writeString(payload.playerName);
        buf.writeVarInt(payload.rotation);
    }

    private static ApplyPlayerHeadSkinPayload read(RegistryByteBuf buf) {
        return new ApplyPlayerHeadSkinPayload(buf.readBlockPos(), buf.readString(64), buf.readVarInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

package com.skrra.blockreskinner.skin;

import com.skrra.blockreskinner.util.BlockStateUtil;
import net.minecraft.block.BlockState;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public final class SkinCodecs {
    public static final PacketCodec<RegistryByteBuf, SkinData> SKIN_DATA = PacketCodec.ofStatic(SkinCodecs::writeSkin, SkinCodecs::readSkin);

    private SkinCodecs() {
    }

    public static void writeSkin(RegistryByteBuf buf, SkinData data) {
        buf.writeEnumConstant(data.type());
        buf.writeBlockPos(data.pos());
        if (data instanceof ConnectedSkinData connected) {
            buf.writeString(BlockStateUtil.toString(connected.visualMaterialState()));
            buf.writeEnumConstant(connected.north());
            buf.writeEnumConstant(connected.east());
            buf.writeEnumConstant(connected.south());
            buf.writeEnumConstant(connected.west());
        } else if (data instanceof SimpleSkinData simple) {
            buf.writeString(BlockStateUtil.toString(simple.visualState()));
        } else if (data instanceof PlayerHeadSkinData playerHead) {
            buf.writeString(playerHead.playerName());
            buf.writeVarInt(playerHead.rotation());
        }
    }

    public static SkinData readSkin(RegistryByteBuf buf) {
        SkinType type = buf.readEnumConstant(SkinType.class);
        BlockPos pos = buf.readBlockPos();
        if (type == SkinType.PLAYER_HEAD) {
            String name = buf.readString(64);
            int rotation = Math.floorMod(buf.readVarInt(), 16);
            return new PlayerHeadSkinData(pos, name, rotation);
        }
        BlockState state = BlockStateUtil.parse(buf.readString(256));
        if (state == null) {
            state = net.minecraft.block.Blocks.STONE.getDefaultState();
        }
        if (type == SkinType.CONNECTED) {
            return new ConnectedSkinData(
                    pos,
                    state,
                    buf.readEnumConstant(ConnectionOverride.class),
                    buf.readEnumConstant(ConnectionOverride.class),
                    buf.readEnumConstant(ConnectionOverride.class),
                    buf.readEnumConstant(ConnectionOverride.class)
            );
        }
        return new SimpleSkinData(pos, state);
    }
}

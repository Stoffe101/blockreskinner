package com.skrra.blockreskinner.skin;

import com.mojang.serialization.Codec;
import com.skrra.blockreskinner.util.BlockStateUtil;
import net.minecraft.block.BlockState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServerSkinStorage extends PersistentState {
    private static final Codec<ServerSkinStorage> CODEC = NbtCompound.CODEC.xmap(ServerSkinStorage::fromNbt, ServerSkinStorage::toNbt);
    private static final PersistentStateType<ServerSkinStorage> TYPE = new PersistentStateType<>(
            "blockreskinner_skins",
            ServerSkinStorage::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<Long, SkinData> skins = new HashMap<>();

    public static ServerSkinStorage get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public Collection<SkinData> all() {
        return skins.values();
    }

    public SkinData get(BlockPos pos) {
        return skins.get(pos.asLong());
    }

    public void put(SkinData data) {
        skins.put(data.pos().asLong(), data);
        markDirty();
    }

    public boolean remove(BlockPos pos) {
        boolean removed = skins.remove(pos.asLong()) != null;
        if (removed) {
            markDirty();
        }
        return removed;
    }

    private static ServerSkinStorage fromNbt(NbtCompound root) {
        ServerSkinStorage storage = new ServerSkinStorage();
        for (NbtCompound tag : root.getListOrEmpty("skins").streamCompounds().toList()) {
            SkinData data = readSkin(tag);
            if (data != null) {
                storage.skins.put(data.pos().asLong(), data);
            }
        }
        return storage;
    }

    private NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();
        for (SkinData data : skins.values()) {
            list.add(writeSkin(data));
        }
        root.put("skins", list);
        return root;
    }

    private static SkinData readSkin(NbtCompound tag) {
        BlockPos pos = BlockPos.fromLong(tag.getLong("pos", 0L));
        SkinType type;
        try {
            type = SkinType.valueOf(tag.getString("type", SkinType.SIMPLE.name()));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (type == SkinType.PLAYER_HEAD) {
            String player = tag.getString("player", "");
            if (!SkinQueries.isValidPlayerName(player)) {
                return null;
            }
            return new PlayerHeadSkinData(pos, player, Math.floorMod(tag.getInt("rotation", 0), 16));
        }
        BlockState visual = BlockStateUtil.parse(tag.getString("visual", ""));
        if (visual == null) {
            return null;
        }
        if (type == SkinType.CONNECTED) {
            return new ConnectedSkinData(
                    pos,
                    visual,
                    ConnectionOverride.byOrdinal(tag.getInt("north", 0)),
                    ConnectionOverride.byOrdinal(tag.getInt("east", 0)),
                    ConnectionOverride.byOrdinal(tag.getInt("south", 0)),
                    ConnectionOverride.byOrdinal(tag.getInt("west", 0))
            );
        }
        return new SimpleSkinData(pos, visual);
    }

    private static NbtCompound writeSkin(SkinData data) {
        NbtCompound tag = new NbtCompound();
        tag.putLong("pos", data.pos().asLong());
        tag.putString("type", data.type().name());
        if (data instanceof ConnectedSkinData connected) {
            tag.putString("visual", BlockStateUtil.toString(connected.visualMaterialState()));
            tag.putInt("north", connected.north().ordinal());
            tag.putInt("east", connected.east().ordinal());
            tag.putInt("south", connected.south().ordinal());
            tag.putInt("west", connected.west().ordinal());
        } else if (data instanceof SimpleSkinData simple) {
            tag.putString("visual", BlockStateUtil.toString(simple.visualState()));
        } else if (data instanceof PlayerHeadSkinData playerHead) {
            tag.putString("player", playerHead.playerName());
            tag.putInt("rotation", playerHead.rotation());
        }
        return tag;
    }
}

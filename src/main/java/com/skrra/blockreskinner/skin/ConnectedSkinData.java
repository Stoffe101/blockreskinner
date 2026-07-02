package com.skrra.blockreskinner.skin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public record ConnectedSkinData(
        BlockPos pos,
        BlockState visualMaterialState,
        ConnectionOverride north,
        ConnectionOverride east,
        ConnectionOverride south,
        ConnectionOverride west
) implements SkinData {
    @Override
    public SkinType type() {
        return SkinType.CONNECTED;
    }

    public ConnectionOverride overrideFor(String directionName) {
        return switch (directionName) {
            case "north" -> north;
            case "east" -> east;
            case "south" -> south;
            case "west" -> west;
            default -> ConnectionOverride.AUTO;
        };
    }
}

package com.skrra.blockreskinner.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.WallShape;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public final class ConnectedBlockUtil {
    private ConnectedBlockUtil() {
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        if (state.getBlock() instanceof WallBlock) {
            EnumProperty<WallShape> property = wallProperty(direction);
            return property != null && state.contains(property) && state.get(property) != WallShape.NONE;
        }

        BooleanProperty property = booleanProperty(direction);
        return property != null && state.contains(property) && state.get(property);
    }

    private static BooleanProperty booleanProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> Properties.NORTH;
            case EAST -> Properties.EAST;
            case SOUTH -> Properties.SOUTH;
            case WEST -> Properties.WEST;
            default -> null;
        };
    }

    private static EnumProperty<WallShape> wallProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> Properties.NORTH_WALL_SHAPE;
            case EAST -> Properties.EAST_WALL_SHAPE;
            case SOUTH -> Properties.SOUTH_WALL_SHAPE;
            case WEST -> Properties.WEST_WALL_SHAPE;
            default -> null;
        };
    }
}

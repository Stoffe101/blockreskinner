package com.skrra.blockreskinner.render;

import com.skrra.blockreskinner.skin.ClientSkinCache;
import com.skrra.blockreskinner.skin.ConnectedSkinData;
import com.skrra.blockreskinner.skin.ConnectionOverride;
import com.skrra.blockreskinner.skin.SimpleSkinData;
import com.skrra.blockreskinner.skin.SkinData;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.WallShape;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public final class VisualStateResolver {
    private VisualStateResolver() {
    }

    public static BlockState resolve(BlockState realState, BlockPos pos, BlockRenderView world) {
        SkinData data = ClientSkinCache.get(pos);
        if (data instanceof SimpleSkinData simple) {
            return simple.visualState();
        }
        if (data instanceof ConnectedSkinData connected) {
            return resolveConnected(realState, connected);
        }
        return realState;
    }

    private static BlockState resolveConnected(BlockState realState, ConnectedSkinData data) {
        BlockState visual = data.visualMaterialState();
        visual = copyIfPresent(realState, visual, Properties.WATERLOGGED);

        if (visual.getBlock() instanceof WallBlock) {
            visual = applyWall(realState, visual, Properties.NORTH_WALL_SHAPE, data.north());
            visual = applyWall(realState, visual, Properties.EAST_WALL_SHAPE, data.east());
            visual = applyWall(realState, visual, Properties.SOUTH_WALL_SHAPE, data.south());
            visual = applyWall(realState, visual, Properties.WEST_WALL_SHAPE, data.west());
            visual = copyIfPresent(realState, visual, Properties.UP);
            return visual;
        }

        visual = applyBoolean(realState, visual, Properties.NORTH, data.north());
        visual = applyBoolean(realState, visual, Properties.EAST, data.east());
        visual = applyBoolean(realState, visual, Properties.SOUTH, data.south());
        visual = applyBoolean(realState, visual, Properties.WEST, data.west());
        return visual;
    }

    private static BlockState applyBoolean(BlockState real, BlockState visual, BooleanProperty property, ConnectionOverride override) {
        if (!visual.contains(property)) {
            return visual;
        }
        return switch (override) {
            case AUTO -> real.contains(property) ? visual.with(property, real.get(property)) : visual;
            case FORCE_ON -> visual.with(property, true);
            case FORCE_OFF -> visual.with(property, false);
        };
    }

    private static BlockState applyWall(BlockState real, BlockState visual, EnumProperty<WallShape> property, ConnectionOverride override) {
        if (!visual.contains(property)) {
            return visual;
        }
        return switch (override) {
            case AUTO -> real.contains(property) ? visual.with(property, real.get(property)) : visual;
            case FORCE_ON -> visual.with(property, WallShape.LOW);
            case FORCE_OFF -> visual.with(property, WallShape.NONE);
        };
    }

    private static <T extends Comparable<T>> BlockState copyIfPresent(BlockState real, BlockState visual, net.minecraft.state.property.Property<T> property) {
        if (real.contains(property) && visual.contains(property)) {
            return visual.with(property, real.get(property));
        }
        return visual;
    }
}

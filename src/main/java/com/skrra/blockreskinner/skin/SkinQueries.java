package com.skrra.blockreskinner.skin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SkinQueries {
    /**
     * Blocks that pass the shape checks but never make sense as a visual skin.
     * Exact registry paths only; substring rules removed good blocks before
     * (for example "kelp" also removed dried_kelp_block).
     */
    private static final Set<String> DENIED_PATHS = Set.of(
            "barrier",
            "light",
            "structure_void",
            "moving_piston",
            "frosted_ice",
            "cobweb"
    );

    /** Substring denials, kept deliberately narrow. */
    private static final String[] DENIED_PATH_PARTS = {
            "infested_"
    };

    private SkinQueries() {
    }

    public static boolean isConnectedBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FenceBlock || block instanceof WallBlock || block instanceof PaneBlock;
    }

    public static boolean isSupportedTarget(BlockState state) {
        return !state.isAir() && !state.hasBlockEntity();
    }

    /**
     * Validates a visual skin state. Runs on both client (list building) and
     * server (payload validation). Uses several independent checks: basic
     * renderability, connected-block exclusion, class checks, denylist, and
     * full-cube outline+collision shape checks.
     */
    public static boolean isAllowedSimpleVisual(BlockState state) {
        if (!isSafeRenderable(state) || isConnectedBlock(state) || hasDeniedName(state)) {
            return false;
        }
        return isFullCubeOutline(state) && isFullCubeCollision(state);
    }

    public static boolean isAllowedConnectedVisual(BlockState target, BlockState visual) {
        if (!isConnectedBlock(target) || !isConnectedBlock(visual) || !isSafeRenderable(visual)) {
            return false;
        }
        return target.getBlock().getClass().isAssignableFrom(visual.getBlock().getClass())
                || visual.getBlock().getClass().isAssignableFrom(target.getBlock().getClass());
    }

    /**
     * Applies sane defaults to a visual state so it is purely cosmetic.
     * Leaves are forced persistent so the visual never implies decay, and
     * waterlogging is stripped from every skin.
     */
    public static BlockState normalizeSimpleVisual(BlockState state) {
        if (state.contains(Properties.PERSISTENT)) {
            state = state.with(Properties.PERSISTENT, true);
        }
        if (state.contains(Properties.DISTANCE_1_7)) {
            state = state.with(Properties.DISTANCE_1_7, 7);
        }
        if (state.contains(Properties.WATERLOGGED)) {
            state = state.with(Properties.WATERLOGGED, false);
        }
        return state;
    }

    public static List<BlockState> simpleVisualStates() {
        List<BlockState> states = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            BlockState state = normalizeSimpleVisual(block.getDefaultState());
            if (isAllowedSimpleVisual(state)) {
                if (state.contains(Properties.AXIS)) {
                    states.add(state.with(Properties.AXIS, Direction.Axis.Y));
                    states.add(state.with(Properties.AXIS, Direction.Axis.X));
                    states.add(state.with(Properties.AXIS, Direction.Axis.Z));
                } else {
                    states.add(state);
                }
            }
        }
        states.sort(Comparator
                .comparing(SkinQueries::category)
                .thenComparing(state -> Registries.BLOCK.getId(state.getBlock()).toString())
                .thenComparing(SkinQueries::axisSort));
        return states;
    }

    public static List<BlockState> connectedVisualStates(BlockState target) {
        List<BlockState> states = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            BlockState state = block.getDefaultState();
            if (isAllowedConnectedVisual(target, state)) {
                states.add(state);
            }
        }
        states.sort(Comparator
                .comparing(SkinQueries::category)
                .thenComparing(state -> Registries.BLOCK.getId(state.getBlock()).toString()));
        return states;
    }

    private static boolean isSafeRenderable(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isOf(Fluids.EMPTY)
                && !state.hasBlockEntity()
                && state.getRenderType() == BlockRenderType.MODEL;
    }

    public static SkinCategory category(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof FenceBlock) {
            return SkinCategory.CONNECTED_FENCES;
        }
        if (block instanceof WallBlock) {
            return SkinCategory.CONNECTED_WALLS;
        }
        if (block instanceof PaneBlock) {
            return SkinCategory.CONNECTED_PANES_AND_BARS;
        }
        if (block instanceof LeavesBlock) {
            return SkinCategory.LEAVES;
        }
        if (isGlassLike(state)) {
            return SkinCategory.TRANSPARENT;
        }
        if (state.contains(Properties.AXIS)) {
            return SkinCategory.LOGS_AND_PILLARS;
        }
        return SkinCategory.FULL_BLOCKS;
    }

    public static TextKey axisLabelKey(BlockState state) {
        if (!state.contains(Properties.AXIS)) {
            return null;
        }
        return switch (state.get(Properties.AXIS)) {
            case Y -> new TextKey("screen.blockreskinner.axis.vertical", "Axis: Y");
            case X -> new TextKey("screen.blockreskinner.axis.east_west", "Axis: X");
            case Z -> new TextKey("screen.blockreskinner.axis.north_south", "Axis: Z");
        };
    }

    private static boolean isFullCubeOutline(BlockState state) {
        try {
            return Block.isShapeFullCube(state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFullCubeCollision(BlockState state) {
        try {
            return Block.isShapeFullCube(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasDeniedName(BlockState state) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (DENIED_PATHS.contains(path)) {
            return true;
        }
        for (String denied : DENIED_PATH_PARTS) {
            if (path.contains(denied)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIceLike(BlockState state) {
        String path = Registries.BLOCK.getId(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        return path.equals("ice") || path.endsWith("_ice");
    }

    private static boolean isGlassLike(BlockState state) {
        String path = Registries.BLOCK.getId(state.getBlock()).getPath().toLowerCase(Locale.ROOT);
        return path.contains("glass") && !isIceLike(state);
    }

    private static int axisSort(BlockState state) {
        if (!state.contains(Properties.AXIS)) {
            return 0;
        }
        return switch (state.get(Properties.AXIS)) {
            case Y -> 0;
            case X -> 1;
            case Z -> 2;
        };
    }

    public record TextKey(String labelKey, String debugText) {
    }
}

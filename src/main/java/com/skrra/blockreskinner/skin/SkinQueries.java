package com.skrra.blockreskinner.skin;

import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.AmethystClusterBlock;
import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.GrateBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.TintedGlassBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
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
            "frosted_ice"
    );

    /** Substring denials, kept deliberately narrow. */
    private static final String[] DENIED_PATH_PARTS = {
            "infested_"
    };

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

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
     * server (payload validation). A visual is allowed when it passes the
     * shared safety checks and one of the category-specific rules below.
     *
     * <p>Heads/skulls are validated separately: their block states have block
     * entities and an invisible chunk model, so they bypass the renderable
     * check and are drawn by the dedicated client-only VisualHeadRenderer
     * instead of the chunk mesh.
     */
    public static boolean isAllowedSimpleVisual(BlockState state) {
        if (state.isAir() || isConnectedBlock(state) || hasDeniedName(state)) {
            return false;
        }
        if (isAllowedHeadVisual(state)) {
            return true;
        }
        if (!isSafeRenderable(state)) {
            return false;
        }
        return isAllowedFullBlockVisual(state)
                || isAllowedPlantVisual(state)
                || isAllowedCrystalVisual(state);
    }

    /**
     * Vanilla mob heads and skulls, floor and wall variants. Player heads are
     * intentionally excluded for now: they need GameProfile/ProfileComponent
     * data that a plain BlockState cannot carry — supporting them requires
     * storing and syncing profile data alongside the skin and resolving the
     * skin texture through the player skin cache. Modded skull types are also
     * excluded because their models are unknown to the visual head renderer.
     */
    public static boolean isAllowedHeadVisual(BlockState state) {
        if (!(state.getBlock() instanceof AbstractSkullBlock skull)) {
            return false;
        }
        if (!state.getFluidState().isOf(Fluids.EMPTY)) {
            return false;
        }
        return skull.getSkullType() instanceof SkullBlock.Type type && type != SkullBlock.Type.PLAYER;
    }

    /**
     * Valid Minecraft username: 3-16 characters, letters/digits/underscore.
     * Used by the GUI, the server payload handler, and save loading.
     */
    public static boolean isValidPlayerName(String name) {
        if (name == null) {
            return false;
        }
        String trimmed = name.trim();
        if (trimmed.length() < 3 || trimmed.length() > 16) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!(c == '_' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Strict full-cube rule: full outline plus full collision. Also admits
     * leaves, glass/ice, and grates — all full cubes with their own category.
     */
    public static boolean isAllowedFullBlockVisual(BlockState state) {
        return isFullCubeOutline(state) && isFullCubeCollision(state);
    }

    /**
     * Safe single-block plant-style visuals (cross models): ferns, dead bushes,
     * grass, flowers, mushrooms, fungi, saplings, roots and similar. Excluded
     * on purpose:
     * <ul>
     *   <li>tall two-block plants (large fern, tall grass, lilac, ...) — rendering
     *       one half would look broken; proper support needs two-block visual
     *       skins (TODO),</li>
     *   <li>crops/stems — growth-stage rows read as farmland, not decoration,</li>
     *   <li>water plants (FluidFillable, e.g. seagrass) — they need water to
     *       make visual sense.</li>
     * </ul>
     */
    public static boolean isAllowedPlantVisual(BlockState state) {
        Block block = state.getBlock();
        if (!(block instanceof PlantBlock)) {
            return false;
        }
        if (block instanceof TallPlantBlock || state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            return false;
        }
        if (block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock) {
            return false;
        }
        return !(block instanceof FluidFillable);
    }

    /** Amethyst clusters and buds, exposed as facing variants in the GUI. */
    public static boolean isAllowedCrystalVisual(BlockState state) {
        return state.getBlock() instanceof AmethystClusterBlock;
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
     * Leaves are forced persistent so the visual never implies decay,
     * waterlogging is stripped, and age-style growth uses the mature look.
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
        if (state.getBlock() instanceof PlantBlock && state.contains(Properties.AGE_3)) {
            state = state.with(Properties.AGE_3, 3);
        }
        if (state.getBlock() instanceof AbstractSkullBlock && state.contains(Properties.POWERED)) {
            state = state.with(Properties.POWERED, false);
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
                } else if (block instanceof AmethystClusterBlock && state.contains(Properties.FACING)) {
                    for (Direction facing : Direction.values()) {
                        states.add(state.with(Properties.FACING, facing));
                    }
                } else if (block instanceof WallSkullBlock && state.contains(WallSkullBlock.FACING)) {
                    for (Direction facing : HORIZONTALS) {
                        states.add(state.with(WallSkullBlock.FACING, facing));
                    }
                } else if (block instanceof SkullBlock && state.contains(SkullBlock.ROTATION)) {
                    for (Direction facing : HORIZONTALS) {
                        states.add(state.with(SkullBlock.ROTATION, RotationPropertyHelper.fromDirection(facing)));
                    }
                } else {
                    states.add(state);
                }
            }
        }
        // The Player Head editor entry: not a valid SimpleSkin visual (the
        // server rejects player skull states there), but selecting it in the
        // GUI opens the name/rotation editor which applies a PlayerHeadSkinData.
        states.add(net.minecraft.block.Blocks.PLAYER_HEAD.getDefaultState());
        states.sort(Comparator
                .comparing(SkinQueries::category)
                .thenComparing(state -> Registries.BLOCK.getId(state.getBlock()).toString())
                .thenComparing(SkinQueries::variantSort));
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
        if (block instanceof AbstractSkullBlock) {
            return SkinCategory.HEADS;
        }
        if (block instanceof LeavesBlock || block instanceof PlantBlock) {
            return SkinCategory.LEAVES;
        }
        if (block instanceof AmethystClusterBlock) {
            return SkinCategory.CRYSTALS;
        }
        if (block instanceof GrateBlock) {
            return SkinCategory.CUTOUT;
        }
        if (isGlassLike(state)) {
            return SkinCategory.TRANSPARENT;
        }
        if (state.contains(Properties.AXIS)) {
            return SkinCategory.LOGS_AND_PILLARS;
        }
        return SkinCategory.FULL_BLOCKS;
    }

    /** Axis (logs/pillars) or facing (crystals) variant label; null when plain. */
    public static TextKey variantLabelKey(BlockState state) {
        TextKey axis = axisLabelKey(state);
        if (axis != null) {
            return axis;
        }
        if (state.getBlock() instanceof AmethystClusterBlock && state.contains(Properties.FACING)) {
            Direction facing = state.get(Properties.FACING);
            return new TextKey("screen.blockreskinner.facing." + facing.asString(), "Facing: " + facing.asString());
        }
        if (state.getBlock() instanceof WallSkullBlock && state.contains(WallSkullBlock.FACING)) {
            Direction facing = state.get(WallSkullBlock.FACING);
            return new TextKey("screen.blockreskinner.facing." + facing.asString(), "Facing: " + facing.asString() + " (wall)");
        }
        if (state.getBlock() instanceof SkullBlock skullBlock && state.contains(SkullBlock.ROTATION)
                && skullBlock.getSkullType() != SkullBlock.Type.PLAYER) {
            Direction facing = rotationDirection(state.get(SkullBlock.ROTATION));
            if (facing != null) {
                return new TextKey("screen.blockreskinner.facing." + facing.asString(), "Rotation: " + state.get(SkullBlock.ROTATION));
            }
        }
        return null;
    }

    private static Direction rotationDirection(int rotation) {
        for (Direction direction : HORIZONTALS) {
            if (RotationPropertyHelper.fromDirection(direction) == rotation) {
                return direction;
            }
        }
        return null;
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

    /**
     * Strict Glass category matching: glass by registry id or glass class only.
     * Ice, slime and honey are translucent but not glass, so a broad
     * "translucent" check must not be used here — those blocks stay available
     * through All / Full Blocks (subject to the normal full-block validation).
     */
    private static boolean isGlassLike(BlockState state) {
        Block block = state.getBlock();
        String path = Registries.BLOCK.getId(block).getPath().toLowerCase(Locale.ROOT);
        if (path.contains("ice") || path.contains("slime") || path.contains("honey")) {
            return false;
        }
        return path.contains("glass")
                || block instanceof StainedGlassBlock
                || block instanceof TintedGlassBlock;
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

    private static int variantSort(BlockState state) {
        if (state.contains(Properties.AXIS)) {
            return switch (state.get(Properties.AXIS)) {
                case Y -> 0;
                case X -> 1;
                case Z -> 2;
            };
        }
        if (state.getBlock() instanceof AmethystClusterBlock && state.contains(Properties.FACING)) {
            return switch (state.get(Properties.FACING)) {
                case UP -> 0;
                case DOWN -> 1;
                case NORTH -> 2;
                case EAST -> 3;
                case SOUTH -> 4;
                case WEST -> 5;
            };
        }
        if (state.getBlock() instanceof WallSkullBlock && state.contains(WallSkullBlock.FACING)) {
            return horizontalSort(state.get(WallSkullBlock.FACING));
        }
        if (state.getBlock() instanceof SkullBlock && state.contains(SkullBlock.ROTATION)) {
            Direction facing = rotationDirection(state.get(SkullBlock.ROTATION));
            return facing == null ? 9 : horizontalSort(facing);
        }
        return 0;
    }

    private static int horizontalSort(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 4;
        };
    }

    public record TextKey(String labelKey, String debugText) {
    }
}

package com.skrra.blockreskinner.skin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SkinQueries {
    private static final String[] ALLOWED_NAME_PARTS = {
            "planks", "stone", "deepslate", "bricks", "brick", "wool", "concrete", "terracotta", "glass",
            "log", "wood", "stem", "hyphae", "pillar", "quartz", "nether", "basalt", "blackstone",
            "end_stone", "purpur", "sandstone", "granite", "diorite", "andesite", "tuff", "calcite",
            "prismarine", "copper", "mud", "packed_mud", "resin"
    };

    private static final String[] DENIED_NAME_PARTS = {
            "air", "water", "lava", "chest", "furnace", "sign", "bed", "banner", "skull", "head",
            "torch", "sapling", "flower", "grass", "door", "trapdoor", "button", "pressure_plate",
            "lever", "rail", "redstone", "piston", "dispenser", "dropper", "hopper", "crafter",
            "candle", "cake", "carpet", "pane", "fence", "wall", "bars", "slab", "stairs"
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

    public static boolean isAllowedSimpleVisual(BlockState state) {
        if (!isSafeRenderable(state) || isConnectedBlock(state)) {
            return false;
        }
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        for (String denied : DENIED_NAME_PARTS) {
            if (path.contains(denied)) {
                return path.contains("concrete") || path.contains("terracotta") || path.contains("sandstone");
            }
        }
        for (String allowed : ALLOWED_NAME_PARTS) {
            if (path.contains(allowed)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllowedConnectedVisual(BlockState target, BlockState visual) {
        if (!isConnectedBlock(target) || !isConnectedBlock(visual) || !isSafeRenderable(visual)) {
            return false;
        }
        return target.getBlock().getClass().isAssignableFrom(visual.getBlock().getClass())
                || visual.getBlock().getClass().isAssignableFrom(target.getBlock().getClass());
    }

    public static List<BlockState> simpleVisualStates() {
        List<BlockState> states = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            BlockState state = block.getDefaultState();
            if (isAllowedSimpleVisual(state)) {
                states.add(state);
                if (state.contains(Properties.AXIS)) {
                    states.add(state.with(Properties.AXIS, net.minecraft.util.math.Direction.Axis.X));
                    states.add(state.with(Properties.AXIS, net.minecraft.util.math.Direction.Axis.Z));
                }
            }
        }
        states.sort(Comparator.comparing(state -> Registries.BLOCK.getId(state.getBlock()).toString()));
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
        states.sort(Comparator.comparing(state -> Registries.BLOCK.getId(state.getBlock()).toString()));
        return states;
    }

    private static boolean isSafeRenderable(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isOf(Fluids.EMPTY)
                && !state.hasBlockEntity()
                && state.getRenderType() == BlockRenderType.MODEL;
    }
}

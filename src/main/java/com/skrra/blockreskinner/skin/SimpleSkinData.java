package com.skrra.blockreskinner.skin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public record SimpleSkinData(BlockPos pos, BlockState visualState) implements SkinData {
    @Override
    public SkinType type() {
        return SkinType.SIMPLE;
    }
}

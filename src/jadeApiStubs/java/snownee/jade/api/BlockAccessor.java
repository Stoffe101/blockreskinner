package snownee.jade.api;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface BlockAccessor extends Accessor<Object> {
    Block getBlock();

    BlockState getBlockState();

    BlockEntity getBlockEntity();

    BlockPos getPosition();

    Direction getSide();
}

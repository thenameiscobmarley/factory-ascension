package dev.factoryascension.block;

import dev.factoryascension.blockentity.MiniCrusherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Mini Crusher: Create's crushing recipes in one block, run by a motor and a battery instead of crushing wheels. */
public class MiniCrusherBlock extends MachineBlock {
    public MiniCrusherBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiniCrusherBlockEntity(pos, state);
    }
}

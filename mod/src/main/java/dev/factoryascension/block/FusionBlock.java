package dev.factoryascension.block;

import dev.factoryascension.blockentity.FusionBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** See FusionBlockEntity. */
public class FusionBlock extends MachineBlock {
    public FusionBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionBlockEntity(pos, state);
    }
}

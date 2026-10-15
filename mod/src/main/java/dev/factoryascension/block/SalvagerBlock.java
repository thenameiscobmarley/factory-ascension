package dev.factoryascension.block;

import dev.factoryascension.blockentity.SalvagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** See SalvagerBlockEntity. */
public class SalvagerBlock extends MachineBlock {
    public SalvagerBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SalvagerBlockEntity(pos, state);
    }
}

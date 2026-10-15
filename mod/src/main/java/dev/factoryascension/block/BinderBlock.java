package dev.factoryascension.block;

import dev.factoryascension.blockentity.BinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Card Binder: holds tome cards, counts your collection and shows off the best one on top. */
public class BinderBlock extends MachineBlock {
    public BinderBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BinderBlockEntity(pos, state);
    }
}

package dev.factoryascension.block;

import dev.factoryascension.blockentity.OpenerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Pack Opener: KubeJS opens the Enchantment Packs put in it, one a second, into its output slots. */
public class OpenerBlock extends MachineBlock {
    public OpenerBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OpenerBlockEntity(pos, state);
    }
}

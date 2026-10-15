package dev.factoryascension.block;

import dev.factoryascension.blockentity.VaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Ascension Vault: scrollable, sortable, searchable storage. Upgraded through the pack's KubeJS rules. */
public class VaultBlock extends MachineBlock {
    public VaultBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaultBlockEntity(pos, state);
    }
}

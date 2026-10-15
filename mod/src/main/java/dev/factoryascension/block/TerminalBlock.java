package dev.factoryascension.block;

import dev.factoryascension.blockentity.TerminalBlockEntity;
import dev.factoryascension.item.StorageLinkerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Universal Storage Terminal: opens every linked chest as one big chest. A Storage Linker in hand binds to it instead. */
public class TerminalBlock extends MachineBlock {
    public TerminalBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        // The linker's own useOn handles the click
        return player.getItemInHand(hand).getItem() instanceof StorageLinkerItem ? InteractionResult.PASS : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TerminalBlockEntity(pos, state);
    }
}

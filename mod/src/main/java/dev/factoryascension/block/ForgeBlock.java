package dev.factoryascension.block;

import dev.factoryascension.blockentity.ForgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidUtil;

/** Essence Forge: gear slot + Liquid Essence tank. Right-click with a bucket to pour; the pack's KubeJS levels the gear. */
public class ForgeBlock extends MachineBlock {
    public ForgeBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        // Buckets (and any fluid container) fill or drain the tank instead of opening the screen
        if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
            && FluidUtil.interactWithFluidHandler(player, hand, level, pos, null)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return null;
    }
}

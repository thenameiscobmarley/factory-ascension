package dev.factoryascension.machine;

import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Warp Hopper: right-click with an item to key it to that item; Warp Hoppers with the same key send items to each other. */
public class WarpHopperBlock extends Block implements EntityBlock {
    public WarpHopperBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpHopperBlockEntity(FactoryAscension.WARP_HOPPER_BE.get(), pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof WarpHopperBlockEntity be))
            return InteractionResult.sidedSuccess(level.isClientSide);
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            be.setKey(held.getItem());
            player.displayClientMessage(Component.literal("Warp Hopper keyed to ").append(held.getHoverName())
                .append(": it sends to every Warp Hopper with the same key (" + be.peers() + " found)"), true);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            dev.factoryascension.network.FANetwork.MachineInfo.send(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }
}

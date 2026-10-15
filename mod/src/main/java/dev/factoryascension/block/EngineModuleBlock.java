package dev.factoryascension.block;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.menu.EngineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A Modular Engine module with nothing inside: a Coil (stress capacity) or a Gearbox (speed). Right-click for its screen. */
public class EngineModuleBlock extends Block {
    public EngineModuleBlock(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp)
            EngineMenu.open(sp, pos, this == FactoryAscension.COIL.get() ? EngineMenu.COIL : EngineMenu.GEARBOX, getName());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

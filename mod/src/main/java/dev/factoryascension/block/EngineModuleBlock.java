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
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Modular Engine module with nothing inside: a Coil (stress capacity) or a Gearbox (speed). It lies on its side and
 * joins the engine parts next to it along its axis into one rod (EngineRod). Right-click for its screen.
 */
public class EngineModuleBlock extends Block {
    public EngineModuleBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(EngineRod.AXIS, Direction.Axis.X).setValue(EngineRod.NEG, false).setValue(EngineRod.POS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EngineRod.AXIS, EngineRod.NEG, EngineRod.POS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return EngineRod.connect(defaultBlockState().setValue(EngineRod.AXIS, EngineRod.axisFor(ctx)), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir.getAxis() == state.getValue(EngineRod.AXIS) ? EngineRod.connect(state, level, pos) : state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp)
            EngineMenu.open(sp, pos, this == FactoryAscension.COIL.get() ? EngineMenu.COIL : EngineMenu.GEARBOX, getName());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

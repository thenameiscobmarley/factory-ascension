package dev.factoryascension.block;

import dev.factoryascension.blockentity.BurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;

/** Burner Module: part of the engine rod. Right-click with fuel to fill it, or with anything else to open its screen. */
public class BurnerBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BurnerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(EngineRod.AXIS, Direction.Axis.X).setValue(EngineRod.NEG, false).setValue(EngineRod.POS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, EngineRod.AXIS, EngineRod.NEG, EngineRod.POS);
    }

    // Lies on its side as part of the engine rod (EngineRod)
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BurnerBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof BurnerBlockEntity burner)) return InteractionResult.sidedSuccess(level.isClientSide);
        ItemStack held = player.getItemInHand(hand);
        if (BurnerBlockEntity.burnTime(held) > 0) {
            ItemStack left = burner.fuel().insertItem(0, held.copy(), false);
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, left);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            dev.factoryascension.menu.EngineMenu.open(sp, pos, dev.factoryascension.menu.EngineMenu.BURNER, getName());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BurnerBlockEntity b) b.dropContents(level, pos);
        super.onRemove(state, level, pos, newState, moving);
    }
}

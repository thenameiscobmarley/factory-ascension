package dev.factoryascension.block;

import dev.factoryascension.blockentity.WorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * Ascension Workbench: two blocks wide, like a bed. The main half (MAIN=true) holds the block entity; the other half
 * sits to its right as you face the bench. Clicking either half opens the bench; breaking either breaks both.
 */
public class WorkbenchBlock extends MachineBlock {
    public static final BooleanProperty MAIN = BooleanProperty.create("main");

    public WorkbenchBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(MAIN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MAIN);
    }

    /** Which way the other half is from this one. */
    public static Direction toPartner(BlockState state) {
        Direction side = state.getValue(FACING).getCounterClockWise();
        return state.getValue(MAIN) ? side : side.getOpposite();
    }

    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return state.getValue(MAIN) ? pos : pos.relative(toPartner(state));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) return null;
        BlockPos other = ctx.getClickedPos().relative(state.getValue(FACING).getCounterClockWise());
        return ctx.getLevel().getBlockState(other).canBeReplaced(ctx) && ctx.getLevel().getWorldBorder().isWithinBounds(other) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(pos.relative(toPartner(state)), state.setValue(MAIN, false), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    // A half without its partner is gone too
    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == toPartner(state) && !(neighbor.is(this) && neighbor.getValue(MAIN) != state.getValue(MAIN))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    // Breaking the side half breaks the main one properly, so the bench drops (only the main half has a drop)
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !state.getValue(MAIN)) {
            BlockPos main = mainPos(state, pos);
            if (level.getBlockState(main).is(this)) level.destroyBlock(main, !player.isCreative(), player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(mainPos(state, pos)) instanceof WorkbenchBlockEntity be)
            NetworkHooks.openScreen(sp, be, be.getBlockPos());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(MAIN) ? new WorkbenchBlockEntity(pos, state) : null;
    }
}

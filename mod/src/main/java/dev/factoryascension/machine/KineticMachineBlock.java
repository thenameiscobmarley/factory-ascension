package dev.factoryascension.machine;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/** A Create machine with its front facing you and its shaft in the back. Right-click: what it's doing, in the action bar. */
public class KineticMachineBlock<T extends KineticBlockEntity & KineticMachineBlock.Status> extends HorizontalKineticBlock implements IBE<T> {
    /** Block entities that can say what they're doing. */
    public interface Status {
        Component status();

        /** Rows for the machine's panel (client/MachineScreen): {label, value, "ok" | "bad" | ""}. */
        default java.util.List<String[]> info() {
            return java.util.List.of();
        }
    }

    private final Class<T> cls;
    private final Supplier<BlockEntityType<? extends T>> type;

    public KineticMachineBlock(Properties props, Class<T> cls, Supplier<BlockEntityType<? extends T>> type) {
        super(props);
        this.cls = cls;
        this.type = type;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && player instanceof net.minecraft.server.level.ServerPlayer sp)
            dev.factoryascension.network.FANetwork.MachineInfo.send(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Buffered b) b.dropBuffer(level, pos);
        IBE.onRemove(state, level, pos, newState);
    }

    /** Machines that hold items until there's room for them. */
    public interface Buffered {
        void dropBuffer(Level level, BlockPos pos);
    }

    @Override
    public Class<T> getBlockEntityClass() {
        return cls;
    }

    @Override
    public BlockEntityType<? extends T> getBlockEntityType() {
        return type.get();
    }
}

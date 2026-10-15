package dev.factoryascension.block;

import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Engine modules lie on their side and join into one long engine rod: each has a horizontal axis and knows whether
 * the next block along it (either way) is another engine part, so open ends get a flange and joined ends don't.
 */
public final class EngineRod {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty NEG = BooleanProperty.create("neg");   // joined toward -x / -z
    public static final BooleanProperty POS = BooleanProperty.create("pos");   // joined toward +x / +z

    private EngineRod() {
    }

    public static boolean isEnginePart(BlockState s) {
        return s.is(FactoryAscension.ENGINE_CORE.get()) || s.is(FactoryAscension.COIL.get()) || s.is(FactoryAscension.GEARBOX.get())
            || s.is(FactoryAscension.BURNER.get());
    }

    /** Along the face you click (placing against the core's back makes the rod go straight out), else the way you face. */
    public static Direction.Axis axisFor(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (face.getAxis().isHorizontal()) return face.getAxis();
        return ctx.getHorizontalDirection().getAxis();
    }

    public static BlockState connect(BlockState state, LevelAccessor level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction neg = Direction.get(Direction.AxisDirection.NEGATIVE, axis), pos2 = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        return state.setValue(NEG, isEnginePart(level.getBlockState(pos.relative(neg))))
            .setValue(POS, isEnginePart(level.getBlockState(pos.relative(pos2))));
    }
}

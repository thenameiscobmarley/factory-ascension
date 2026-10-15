package dev.factoryascension.blockentity;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.EngineModuleBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Modular Engine Core: a Create generator built from modules touching it (and each other). Coils give stress capacity,
 * Gearboxes raise the speed, Burners burn fuel; each lit Burner powers a few Coils. Output is on the core's front face.
 * Nothing burns and nothing turns until it has a Coil and a lit Burner. One core per engine: two cores sharing
 * modules jam.
 */
public class EngineBlockEntity extends GeneratingKineticBlockEntity {
    private final List<BlockPos> burners = new ArrayList<>();
    private int coils, gears, modules, lit, burnerCount;
    private boolean jammed, tooBig;
    private float rpm = 0, capacityPerRpm = 0;
    private int scanTimer = 0;

    public EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static EngineBlockEntity create(BlockPos pos, BlockState state) {
        return new EngineBlockEntity(FactoryAscension.ENGINE_BE.get(), pos, state);
    }

    public int coils() { return coils; }
    public int gears() { return gears; }
    public int burnerCount() { return burnerCount; }
    public int litBurners() { return lit; }
    public boolean jammed() { return jammed; }
    public int modules() { return modules; }
    public float rpm() { return rpm; }
    /** Stress capacity right now, in SU. */
    public float su() { return rpm * capacityPerRpm; }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (--scanTimer <= 0) {
            scanTimer = 20;
            scan();
        }
        // Burn only as many Burners as the Coils need
        int need = jammed ? 0 : (coils + FAConfig.engineCoilsPerBurner - 1) / FAConfig.engineCoilsPerBurner;
        int burning = 0;
        for (BlockPos p : burners) {
            if (!(level.getBlockEntity(p) instanceof BurnerBlockEntity b)) continue;
            if (burning < need && b.burnTick()) burning++;
            else b.idle();
        }
        lit = burning;
        int powered = Math.min(coils, lit * FAConfig.engineCoilsPerBurner);
        float newRpm = powered > 0 ? FAConfig.engineRpm[Math.min(gears, FAConfig.engineRpm.length - 1)] : 0;
        float newCap = newRpm > 0 ? powered * FAConfig.engineSuPerCoil / newRpm : 0;
        if (newRpm != rpm) {
            rpm = newRpm;
            capacityPerRpm = newCap;
            updateGeneratedRotation();
            sendData();
        } else if (newCap != capacityPerRpm) {
            capacityPerRpm = newCap;
            notifyStressCapacityChange(calculateAddedStressCapacity());
            sendData();
        }
    }

    /** Find the modules connected to this core (through each other), up to the limit. */
    private void scan() {
        burners.clear();
        coils = gears = modules = 0;
        jammed = tooBig = false;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        seen.add(worldPosition);
        todo.add(worldPosition);
        while (!todo.isEmpty()) {
            BlockPos at = todo.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                if (!seen.add(n)) continue;
                Block block = level.getBlockState(n).getBlock();
                if (block == FactoryAscension.ENGINE_CORE.get()) {
                    jammed = true;   // another core touching this engine
                    continue;
                }
                if (!(block instanceof EngineModuleBlock) && block != FactoryAscension.BURNER.get()) continue;
                if (modules >= FAConfig.engineMaxModules) {
                    tooBig = true;
                    continue;
                }
                modules++;
                if (block == FactoryAscension.COIL.get()) coils++;
                else if (block == FactoryAscension.GEARBOX.get()) gears++;
                else burners.add(n.immutable());
                todo.add(n);
            }
        }
        burnerCount = burners.size();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!(getBlockState().getBlock() instanceof HorizontalKineticBlock)) return 0;
        return convertToDirection(rpm, getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        lastCapacityProvided = capacityPerRpm;
        return capacityPerRpm;
    }

    /** One line for the action bar when the core is right-clicked. */
    public Component status() {
        if (jammed) return Component.translatable("gui.factoryascension.engine_jammed").withStyle(ChatFormatting.RED);
        if (coils == 0) return Component.translatable("gui.factoryascension.engine_no_coils").withStyle(ChatFormatting.YELLOW);
        if (lit == 0) return Component.translatable("gui.factoryascension.engine_no_fuel", burnerCount).withStyle(ChatFormatting.YELLOW);
        return Component.translatable("gui.factoryascension.engine_status", (int) rpm, (int) su(), coils, gears, lit, burnerCount)
            .withStyle(ChatFormatting.AQUA);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        tooltip.add(Component.literal("    ").append(Component.translatable("gui.factoryascension.engine_modules", coils, gears, burnerCount, modules,
            FAConfig.engineMaxModules)).withStyle(ChatFormatting.GRAY));
        if (tooBig) tooltip.add(Component.literal("    ").append(Component.translatable("gui.factoryascension.engine_too_big")).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("EngineRpm", rpm);
        tag.putFloat("EngineCap", capacityPerRpm);
        tag.putInt("Coils", coils);
        tag.putInt("Gears", gears);
        tag.putInt("Lit", lit);
        tag.putInt("Burners", burnerCount);
        tag.putInt("Modules", modules);
        tag.putBoolean("Jammed", jammed);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        rpm = tag.getFloat("EngineRpm");
        capacityPerRpm = tag.getFloat("EngineCap");
        coils = tag.getInt("Coils");
        gears = tag.getInt("Gears");
        lit = tag.getInt("Lit");
        modules = tag.getInt("Modules");
        burnerCount = tag.getInt("Burners");
        jammed = tag.getBoolean("Jammed");
    }
}

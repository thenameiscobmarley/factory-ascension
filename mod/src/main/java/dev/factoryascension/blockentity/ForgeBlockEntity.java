package dev.factoryascension.blockentity;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.menu.ForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Essence Forge: one gear slot and a Liquid Essence tank. The pack's KubeJS scripts level the gear from the tank (the same
 * rules as the Ascension Altar) and report back through faReport, which the screen shows.
 */
public class ForgeBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents {
    public static final int CAPACITY = 16000;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY, ForgeBlockEntity::isEssence) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
    private int levelsLastSecond = 0;
    private long totalLevels = 0;

    public ForgeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.FORGE_BE.get(), pos, state);
    }

    public static boolean isEssence(FluidStack fluid) {
        var key = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return key != null && "kubejs".equals(key.getNamespace()) && key.getPath().startsWith("liquid_essence");
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank tank() {
        return tank;
    }

    public int levelsLastSecond() {
        return levelsLastSecond;
    }

    public long totalLevels() {
        return totalLevels;
    }

    // ---- For the pack's KubeJS scripts ----
    /** Levels the scripts just added to the gear (0 = idle), shown on the screen. */
    public void faReport(int levels) {
        levelsLastSecond = Math.max(0, levels);
        totalLevels += levelsLastSecond;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.essence_forge");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ForgeMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        ItemStack s = items.getStackInSlot(0);
        if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putLong("TotalLevels", totalLevels);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("Tank")) tank.readFromNBT(tag.getCompound("Tank"));
        totalLevels = tag.getLong("TotalLevels");
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
        fluidCap.invalidate();
    }
}

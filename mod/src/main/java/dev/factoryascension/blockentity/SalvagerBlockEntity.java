package dev.factoryascension.blockentity;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.event.MachineEvent;
import dev.factoryascension.menu.SalvagerMenu;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

/**
 * Salvager: loot goes in the top 9 slots, Ascension Cores come out of the bottom 9. The pack's scripts price each item
 * (the same values as /ascension sell); anything they can't sell, or won't (Artifacts), passes straight through.
 * Fractions of a core wait in the Essence bank until they add up.
 */
public class SalvagerBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents, MachineBlock.Ticking {
    public static final int SLOTS = 9;
    private final ItemStackHandler input = handler();
    private final ItemStackHandler output = handler();
    private final IItemHandler automation = new CombinedInvWrapper(new Automation.InsertOnly(input), new Automation.ExtractOnly(output));
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> automation);
    private double bank = 0;
    private long salvaged = 0;
    private double earned = 0;
    private int timer = 0;

    public SalvagerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.SALVAGER_BE.get(), pos, state);
    }

    private ItemStackHandler handler() {
        return new ItemStackHandler(SLOTS) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public ItemStackHandler input() { return input; }
    public ItemStackHandler output() { return output; }
    public double bank() { return bank; }
    public long salvaged() { return salvaged; }
    public double earned() { return earned; }

    @Override
    public void serverTick() {
        if (++timer < FAConfig.salvagerTicks) return;
        timer = 0;
        boolean any = false;
        for (int i = 0; i < SLOTS && !any; i++) any = !input.getStackInSlot(i).isEmpty();
        if (any || bank >= 1) MinecraftForge.EVENT_BUS.post(new MachineEvent(level, worldPosition, "salvage", null));
    }

    // ---- For the pack's KubeJS scripts ----
    public ItemStackHandler faInput() { return input; }
    public ItemStackHandler faOutput() { return output; }
    public double faGetBank() { return bank; }

    public void faSetBank(double value) {
        bank = Double.isFinite(value) && value > 0 ? value : 0;
        setChanged();
    }

    /** Count salvaged items and the Essence they were worth. */
    public void faReport(int items, double value) {
        if (items > 0) salvaged += items;
        if (Double.isFinite(value) && value > 0) earned += value;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.salvager");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SalvagerMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStackHandler h : new ItemStackHandler[]{input, output})
            for (int i = 0; i < SLOTS; i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
            }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Input", input.serializeNBT());
        tag.put("Output", output.serializeNBT());
        tag.putDouble("Bank", bank);
        tag.putLong("Salvaged", salvaged);
        tag.putDouble("Earned", earned);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Input")) input.deserializeNBT(tag.getCompound("Input"));
        if (tag.contains("Output")) output.deserializeNBT(tag.getCompound("Output"));
        bank = tag.getDouble("Bank");
        salvaged = tag.getLong("Salvaged");
        earned = tag.getDouble("Earned");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return new CompoundTag();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}

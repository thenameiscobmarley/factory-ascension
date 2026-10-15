package dev.factoryascension.blockentity;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.event.MachineEvent;
import dev.factoryascension.menu.WorkbenchMenu;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.items.ItemStackHandler;

/**
 * Ascension Workbench: a gear slot and a modifier slot. The pack's scripts say what Apply would do (tomes, gems,
 * attachments, Chaos Tomes, Polishing Cloths) and run Apply and Star Up (Legendary stars) with the same rules as
 * right-clicking.
 */
public class WorkbenchBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents, MachineBlock.Ticking {
    public static final int GEAR = 0, MOD = 1;
    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != GEAR || stack.getMaxStackSize() == 1;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == GEAR ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            dirty = true;
        }
    };
    private String status = "";
    private boolean canApply = false;
    private boolean dirty = true;

    public WorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.WORKBENCH_BE.get(), pos, state);
    }

    public ItemStackHandler items() { return items; }
    public String status() { return status; }
    public boolean canApply() { return canApply; }

    public void request(String kind, Player player) {
        if (level != null && !level.isClientSide) MinecraftForge.EVENT_BUS.post(new MachineEvent(level, worldPosition, kind, player));
    }

    @Override
    public void serverTick() {
        if (!dirty) return;
        dirty = false;
        request("bench_preview", null);
    }

    // ---- For the pack's KubeJS scripts ----
    public ItemStackHandler faItems() { return items; }

    /** The scripts changed the stacks in place (enchanted the gear, used up the tome): save, sync, re-preview. */
    public void faChanged() {
        setChanged();
        dirty = true;
        sync();
    }

    public void faSetPreview(String text, boolean ok) {
        status = text == null ? "" : text;
        canApply = ok;
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.ascension_workbench");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WorkbenchMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
    }

    private CompoundTag statusTag(CompoundTag tag) {
        tag.putString("Status", status);
        tag.putBoolean("CanApply", canApply);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        statusTag(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        status = tag.getString("Status");
        canApply = tag.getBoolean("CanApply");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return statusTag(new CompoundTag());
    }
}

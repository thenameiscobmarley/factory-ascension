package dev.factoryascension.blockentity;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.event.MachineEvent;
import dev.factoryascension.menu.FusionMenu;
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
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Tome Fusion Table: three tome slots, a core slot and the result. The pack's scripts decide what the tomes make
 * (merge copies of one enchant, or build a Fusion / Triad from its parents) and show it before you press Fuse.
 */
public class FusionBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents, MachineBlock.Ticking {
    public static final int TOMES = 3, CORE = 3, RESULT = 4;
    private final ItemStackHandler items = new ItemStackHandler(5) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < TOMES) return isTome(stack);
            if (slot == CORE) return isCore(stack);
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            dirty = true;
        }
    };
    private ItemStack preview = ItemStack.EMPTY;
    private String status = "";
    private boolean canFuse = false;
    private boolean dirty = true;

    public FusionBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.FUSION_BE.get(), pos, state);
    }

    public static boolean isTome(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "kubejs".equals(key.getNamespace()) && key.getPath().startsWith("tome_");
    }

    public static boolean isCore(ItemStack stack) {
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && "kubejs".equals(key.getNamespace()) && key.getPath().equals("ascension_core");
    }

    public ItemStackHandler items() { return items; }
    public ItemStack preview() { return preview; }
    public String status() { return status; }
    public boolean canFuse() { return canFuse; }

    /** The Fuse button: the scripts check the slots again and make the tome. */
    public void requestFuse(Player player) {
        if (level != null && !level.isClientSide) MinecraftForge.EVENT_BUS.post(new MachineEvent(level, worldPosition, "fuse", player));
    }

    @Override
    public void serverTick() {
        if (!dirty) return;
        dirty = false;
        MinecraftForge.EVENT_BUS.post(new MachineEvent(level, worldPosition, "preview", null));
    }

    // ---- For the pack's KubeJS scripts ----
    public ItemStackHandler faItems() { return items; }

    /** What the slots would make (or empty), a line of text, and whether Fuse is allowed. */
    public void faSetPreview(ItemStack result, String text, boolean ok) {
        preview = result == null ? ItemStack.EMPTY : result.copy();
        status = text == null ? "" : text;
        canFuse = ok;
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.tome_fusion_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FusionMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
    }

    private CompoundTag previewTag(CompoundTag tag) {
        tag.put("Preview", preview.save(new CompoundTag()));
        tag.putString("Status", status);
        tag.putBoolean("CanFuse", canFuse);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        previewTag(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        preview = ItemStack.of(tag.getCompound("Preview"));
        status = tag.getString("Status");
        canFuse = tag.getBoolean("CanFuse");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return previewTag(new CompoundTag());
    }
}

package dev.factoryascension.blockentity;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.menu.VaultMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ascension Vault storage. Room for MAX_SLOTS items, of which tier unlocks: (tier + 1) * SLOTS_PER_TIER.
 * Tiers are set by the pack's KubeJS rules (faSetTier), so upgrade costs live in the KubeJS config.
 * Hoppers and pipes see only the unlocked slots.
 */
public class VaultBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents {
    public static final int SLOTS_PER_TIER = 108;
    public static final int MAX_TIER = 3;
    public static final int MAX_SLOTS = SLOTS_PER_TIER * (MAX_TIER + 1);

    private final ItemStackHandler items = new ItemStackHandler(MAX_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int tier = 0;
    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new RangedWrapper(items, 0, capacity()));

    public VaultBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.VAULT_BE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    public int capacity() {
        return SLOTS_PER_TIER * (tier + 1);
    }

    // ---- For the pack's KubeJS scripts ----
    public int faGetTier() {
        return tier;
    }

    public int faGetCapacity() {
        return capacity();
    }

    public void faSetTier(int newTier) {
        tier = Math.max(0, Math.min(MAX_TIER, newTier));
        itemCap.invalidate();
        itemCap = LazyOptional.of(() -> new RangedWrapper(items, 0, capacity()));
        sync();
    }

    /** Merge matching stacks and sort everything by item id, then count (biggest first). */
    public void sortContents() {
        List<ItemStack> all = new ArrayList<>();
        int cap = capacity();
        for (int i = 0; i < cap; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemStack left = s.copy();
            for (ItemStack have : all) {
                if (left.isEmpty()) break;
                if (ItemStack.isSameItemSameTags(have, left) && have.getCount() < have.getMaxStackSize()) {
                    int move = Math.min(left.getCount(), have.getMaxStackSize() - have.getCount());
                    have.grow(move);
                    left.shrink(move);
                }
            }
            if (!left.isEmpty()) all.add(left);
        }
        all.sort(Comparator.comparing((ItemStack s) -> String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem())))
            .thenComparing(s -> -s.getCount()));
        for (int i = 0; i < cap; i++) items.setStackInSlot(i, i < all.size() ? all.get(i) : ItemStack.EMPTY);
        setChanged();
    }

    public int usedSlots() {
        int used = 0;
        for (int i = 0; i < capacity(); i++) if (!items.getStackInSlot(i).isEmpty()) used++;
        return used;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.ascension_vault");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new VaultMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < MAX_SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Tier", tier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        tier = tag.getInt("Tier");
    }

    @Override
    public CompoundTag getUpdateTag() {
        // Clients only need the tier (the screen gets items through the menu)
        CompoundTag tag = new CompoundTag();
        tag.putInt("Tier", tier);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        tier = tag.getInt("Tier");
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) handleUpdateTag(pkt.getTag());
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

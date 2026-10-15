package dev.factoryascension.blockentity;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.menu.TerminalMenu;
import dev.factoryascension.storage.BigStorage;
import dev.factoryascension.storage.LinkedInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Universal Storage Terminal: every chest linked to it (with a Storage Linker) as one big searchable, sortable chest.
 * Hoppers and funnels can feed it too: what goes in lands in the first linked chest with room.
 */
public class TerminalBlockEntity extends SyncedBlockEntity implements MenuProvider, BigStorage {
    /** A container needs at least this many slots to be linked (chests and barrels, not furnaces or hoppers). */
    public static final int MIN_SLOTS = 9;

    private final Set<BlockPos> links = new LinkedHashSet<>();
    private final LinkedInventory inventory = new LinkedInventory();
    private long builtAt = 0;
    private boolean dirty = true;
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(this::storage);

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.TERMINAL_BE.get(), pos, state);
    }

    /** One position per chest: a double chest is stored as its lower corner, so both halves count once. */
    public static BlockPos canonical(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            return other.compareTo(pos) < 0 ? other.immutable() : pos.immutable();
        }
        return pos.immutable();
    }

    /** The inventory of a block that can be linked, or null. */
    public static IItemHandler linkable(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null || be instanceof TerminalBlockEntity) return null;
        IItemHandler h = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (h == null && be instanceof Container c && c.getContainerSize() >= MIN_SLOTS) h = new net.minecraftforge.items.wrapper.InvWrapper(c);
        return h != null && h.getSlots() >= MIN_SLOTS ? h : null;
    }

    public enum LinkResult { LINKED, UNLINKED, NOT_STORAGE, TOO_FAR, FULL }

    /** Link a chest, or unlink it if it's already linked. */
    public LinkResult toggle(BlockPos pos) {
        BlockPos key = canonical(level, pos);
        if (links.remove(key) || links.remove(pos.immutable())) {
            changed();
            return LinkResult.UNLINKED;
        }
        if (linkable(level, pos) == null) return LinkResult.NOT_STORAGE;
        if (FAConfig.storageRange > 0 && Math.sqrt(key.distSqr(worldPosition)) > FAConfig.storageRange) return LinkResult.TOO_FAR;
        if (links.size() >= FAConfig.storageMaxLinks) return LinkResult.FULL;
        links.add(key);
        changed();
        return LinkResult.LINKED;
    }

    private void changed() {
        dirty = true;
        sync();
    }

    public int linkCount() {
        return links.size();
    }

    public List<BlockPos> linkList() {
        return new ArrayList<>(links);
    }

    /** The linked chests, rebuilt once a second (or right after a link changes). Unloaded chests are skipped for now. */
    @Override
    public IItemHandlerModifiable storage() {
        if (level != null && !level.isClientSide && (dirty || level.getGameTime() - builtAt >= 20)) {
            dirty = false;
            builtAt = level.getGameTime();
            inventory.clear();
            Set<BlockPos> seen = new HashSet<>();
            boolean pruned = false;
            for (BlockPos pos : new ArrayList<>(links)) {
                if (!level.isLoaded(pos)) continue;
                BlockPos key = canonical(level, pos);
                if (!seen.add(key)) continue;
                IItemHandler h = linkable(level, pos);
                if (h == null) {
                    // The chest is gone: forget it
                    links.remove(pos);
                    pruned = true;
                    continue;
                }
                inventory.add(level.getBlockEntity(pos), h);
            }
            if (pruned) sync();
        }
        return inventory;
    }

    @Override
    public void sortContents() {
        storage();
        inventory.sort();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int links() {
        return links.size();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.storage_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TerminalMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        links.forEach(p -> list.add(LongTag.valueOf(p.asLong())));
        tag.put("Links", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        links.clear();
        tag.getList("Links", Tag.TAG_LONG).forEach(t -> links.add(BlockPos.of(((LongTag) t).getAsLong())));
        dirty = true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && level != null && !level.isClientSide) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}

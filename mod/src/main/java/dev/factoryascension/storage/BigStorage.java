package dev.factoryascension.storage;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.IItemHandlerModifiable;

/** Something the big storage screen can show: the Ascension Vault, or a Universal Storage Terminal's linked chests. */
public interface BigStorage {
    /** Every slot the screen shows, in order. */
    IItemHandlerModifiable storage();

    /** Merge matching stacks and sort by item id. */
    void sortContents();

    void setChanged();

    boolean stillValid(Player player);

    /** How many chests are linked, or -1 for storage that isn't linked (the vault). */
    default int links() {
        return -1;
    }

    default int usedSlots() {
        IItemHandlerModifiable s = storage();
        int used = 0;
        for (int i = 0; i < s.getSlots(); i++) if (!s.getStackInSlot(i).isEmpty()) used++;
        return used;
    }
}

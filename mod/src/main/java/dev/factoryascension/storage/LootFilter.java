package dev.factoryascension.storage;

import dev.factoryascension.FactoryAscension;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The pickup filter and the sort button (both from the inventory screens: client/ui/InvToolbar).
 *  - Filter: items a player never wants (rotten flesh from a Plunder farm...) are deleted the moment they'd drop from
 *    that player's kills or be picked up by them. Something a player threw is never deleted. Kept on the player.
 *  - Sort: the open chest (or, with none open, the main inventory, not the hotbar) is merged and ordered: weapons,
 *    tools, armour, blocks, food, the rest, then by name.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LootFilter {
    public static final String TAG = "fa_loot_filter";
    public static final int MAX = 256;

    private LootFilter() {
    }

    public static Set<String> of(Player p) {
        Set<String> out = new HashSet<>();
        ListTag list = p.getPersistentData().getList(TAG, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    /** From the client: the whole list (it's the client's copy that's edited). */
    public static void set(ServerPlayer p, List<String> ids) {
        ListTag list = new ListTag();
        for (String id : ids) {
            if (list.size() >= MAX) break;
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) list.add(StringTag.valueOf(rl.toString()));
        }
        p.getPersistentData().put(TAG, list);
    }

    static boolean filtered(Set<String> ids, ItemStack s) {
        if (ids.isEmpty() || s.isEmpty() || s.hasTag()) return false;   // named or enchanted things are never thrown away
        var key = ForgeRegistries.ITEMS.getKey(s.getItem());
        return key != null && ids.contains(key.toString());
    }

    // After the loot enchants have multiplied the drops (KubeJS, normal priority)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        Set<String> ids = of(p);
        if (!ids.isEmpty()) event.getDrops().removeIf(e -> filtered(ids, e.getItem()));
    }

    @SubscribeEvent
    public static void pickup(EntityItemPickupEvent event) {
        ItemEntity item = event.getItem();
        if (item.getOwner() != null) return;   // someone threw it: never delete that
        if (filtered(of(event.getEntity()), item.getItem())) {
            item.discard();
            event.setCanceled(true);
        }
    }

    // ---- Sort ----

    static int group(ItemStack s) {
        var i = s.getItem();
        if (i instanceof SwordItem || i.getClass().getName().contains(".gun.")) return 0;
        if (i instanceof TieredItem) return 1;
        if (i instanceof ArmorItem) return 2;
        if (i instanceof BlockItem) return 3;
        if (s.isEdible()) return 4;
        return 5;
    }

    public static void sort(ServerPlayer p) {
        var menu = p.containerMenu;
        List<Slot> slots = new ArrayList<>();
        boolean chest = menu != p.inventoryMenu;
        for (Slot s : menu.slots) {
            boolean mine = s.container == p.getInventory();
            if (chest ? mine : !mine || s.getContainerSlot() < 9 || s.getContainerSlot() >= 36) continue;
            if (!s.mayPickup(p)) return;   // a locked or special slot: leave this screen alone
            slots.add(s);
        }
        if (slots.isEmpty()) return;
        // Take everything out, merge equal stacks, order, and put back
        List<ItemStack> items = new ArrayList<>();
        for (Slot s : slots) {
            ItemStack st = s.getItem().copy();
            if (st.isEmpty()) continue;
            for (ItemStack have : items) {
                if (ItemStack.isSameItemSameTags(have, st) && have.getCount() < have.getMaxStackSize()) {
                    int move = Math.min(st.getCount(), have.getMaxStackSize() - have.getCount());
                    have.grow(move);
                    st.shrink(move);
                    if (st.isEmpty()) break;
                }
            }
            if (!st.isEmpty()) items.add(st);
        }
        items.sort(Comparator.comparingInt(LootFilter::group).thenComparing(s -> s.getHoverName().getString()).thenComparing(s -> -s.getCount()));
        // Only if it all fits (it always should; merging only frees room) and every slot takes what it gets
        if (items.size() > slots.size()) return;
        for (int i = 0; i < slots.size(); i++) slots.get(i).set(i < items.size() ? items.get(i) : ItemStack.EMPTY);
        menu.broadcastChanges();
    }

}

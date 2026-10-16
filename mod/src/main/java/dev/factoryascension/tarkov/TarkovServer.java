package dev.factoryascension.tarkov;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.network.FANetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server side of the Tarkov inventory: every tick each player's grid is checked (27 slots, so it costs nothing), saved
 * with the player when it changes, and sent to their game. Picking up an item that has nowhere to go is refused, and
 * carrying more than the config's limit slows you down.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TarkovServer {
    private static final String KEY = "fa_tarkov";
    private static final UUID WEIGHT_ID = UUID.fromString("6c1a0f7e-2b8d-4f5e-9a43-0d7e1b2c3f41");
    private static final Map<ServerPlayer, TarkovLayout> LAYOUTS = new WeakHashMap<>();
    private static final Map<ServerPlayer, int[]> SENT = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> FULL_MESSAGE = new WeakHashMap<>();

    private TarkovServer() {
    }

    /** This player's grid (loaded from their saved data the first time). */
    public static TarkovLayout layout(ServerPlayer player) {
        return LAYOUTS.computeIfAbsent(player, p -> {
            TarkovLayout l = new TarkovLayout();
            l.copyFrom(p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getIntArray(KEY));
            return l;
        });
    }

    private static void save(ServerPlayer player, TarkovLayout l) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putIntArray(KEY, l.pos.clone());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Check the grid now (also used by the self-test and the move message); sends it if it changed. */
    public static void update(ServerPlayer player) {
        TarkovLayout l = layout(player);
        if (FAConfig.tarkovEnabled && l.validate(player.getInventory().items)) save(player, l);
        int[] state = new int[TarkovLayout.SLOTS + 3];
        System.arraycopy(l.pos, 0, state, 0, TarkovLayout.SLOTS);
        state[TarkovLayout.SLOTS] = FAConfig.tarkovEnabled ? 1 : 0;
        state[TarkovLayout.SLOTS + 1] = FAConfig.tarkovWidth;
        state[TarkovLayout.SLOTS + 2] = FAConfig.tarkovHeight;
        if (!java.util.Arrays.equals(state, SENT.get(player))) {
            SENT.put(player, state);
            FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FANetwork.TarkovSync(FAConfig.tarkovEnabled, FAConfig.tarkovWidth, FAConfig.tarkovHeight, l.pos.clone()));
        }
    }

    /** The client asks for a slot's item to sit at x, y (turned or not). For an empty slot it's the spot its next item takes. */
    public static void plan(ServerPlayer player, int slot, int x, int y, boolean turned) {
        if (!FAConfig.tarkovEnabled || slot < 0 || slot >= TarkovLayout.SLOTS || x < 0 || y < 0 || x >= TarkovLayout.width() || y >= TarkovLayout.height()) return;
        TarkovLayout l = layout(player);
        ItemStack st = player.getInventory().items.get(TarkovLayout.FIRST + slot);
        if (!st.isEmpty()) {
            // Moving or turning an item in place: only if it fits there (besides where it is now)
            if (!TarkovLayout.free(l.occupancy(player.getInventory().items), x, y, TarkovLayout.w(st, turned), TarkovLayout.h(st, turned), slot)) {
                SENT.remove(player);   // tell the client where it really is
                return;
            }
        }
        l.pos[slot] = TarkovLayout.pack(x, y, turned);
        save(player, l);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        try {
            update(player);
            if (player.tickCount % 20 == 0) applyWeight(player);
        } catch (Throwable err) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Tarkov inventory check failed for {}", player.getName().getString(), err);
        }
    }

    // ---- Picking things up: refused when there's nowhere for it ----
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void pickup(EntityItemPickupEvent event) {
        if (!FAConfig.tarkovEnabled || !FAConfig.tarkovBlockPickup || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator() || player instanceof net.minecraftforge.common.util.FakePlayer) return;
        ItemStack stack = event.getItem().getItem();
        if (canTake(player, stack)) return;
        event.setCanceled(true);
        long now = player.level().getGameTime();
        Long said = FULL_MESSAGE.get(player);
        if (said == null || now - said > 60) {
            FULL_MESSAGE.put(player, now);
            player.displayClientMessage(Component.literal("No room in your inventory for " + stack.getHoverName().getString()).withStyle(ChatFormatting.RED), true);
        }
    }

    /** Could any of this stack go in: topping up a stack you have, an empty hotbar slot, or a free spot in the grid. */
    public static boolean canTake(ServerPlayer player, ItemStack stack) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) return true;
        }
        ItemStack off = inv.offhand.get(0);
        if (!off.isEmpty() && ItemStack.isSameItemSameTags(off, stack) && off.getCount() < off.getMaxStackSize()) return true;
        for (int i = 0; i < Inventory.getSelectionSize(); i++) if (inv.items.get(i).isEmpty()) return true;
        return layout(player).hasRoomFor(inv.items, stack);
    }

    // ---- Weight ----
    public static float weight(Player player) {
        float kg = 0;
        Inventory inv = player.getInventory();
        for (ItemStack s : inv.items) kg += TarkovLayout.weight(s);
        for (ItemStack s : inv.armor) kg += TarkovLayout.weight(s);
        for (ItemStack s : inv.offhand) kg += TarkovLayout.weight(s);
        return kg;
    }

    /** How much slower: 0 .. weightMaxSlow. */
    public static float slowdown(float kg) {
        if (!FAConfig.weightEnabled || kg <= FAConfig.weightCarry) return 0;
        return Math.min(FAConfig.weightMaxSlow, (kg - FAConfig.weightCarry) * FAConfig.weightSlowPerKg);
    }

    public static void applyWeight(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        float slow = FAConfig.tarkovEnabled && !player.isCreative() && !player.isSpectator() ? slowdown(weight(player)) : 0;
        AttributeModifier now = speed.getModifier(WEIGHT_ID);
        if (now != null && Math.abs(now.getAmount() + slow) < 1e-4) return;
        if (now != null) speed.removeModifier(WEIGHT_ID);
        if (slow > 0) speed.addTransientModifier(new AttributeModifier(WEIGHT_ID, "Factory Ascension: carrying too much", -slow, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    // A fresh copy goes to the game after joining, respawning or changing dimension
    @SubscribeEvent
    public static void join(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) SENT.remove(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { SENT.remove(p); LAYOUTS.remove(p); }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) SENT.remove(p);
    }

    @SubscribeEvent
    public static void leave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) { SENT.remove(p); LAYOUTS.remove(p); FULL_MESSAGE.remove(p); }
    }
}

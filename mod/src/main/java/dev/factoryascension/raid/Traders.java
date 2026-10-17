package dev.factoryascension.raid;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.AmmoType;
import dev.factoryascension.gun.Caliber;
import dev.factoryascension.gun.CartridgeItem;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.network.FANetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Traders (like Tarkov's): three of them at your base, each with their own goods, and jobs for you. Money is rubles in
 * your wallet (cash bundles from loot go into it). Sell them your loot (each buys their own kind of thing; the Fence
 * takes anything, cheaply), buy from them, barter, and do their tasks for rubles and trust; more trust, better stock.
 * Everything here is in the config (traders); these are the defaults. A Trader Contract brings one to you.
 */
public final class Traders {
    private Traders() {
    }

    public enum Kind {
        QUARTERMASTER("Volkov, Quartermaster", "Guns, ammo and armor. Pays well for weapons and gear."),
        MEDIC("Doc Halloran, Medic", "Medicine and the other stuff. Buys meds and anything that grows."),
        FENCE("The Fence", "Takes anything, pays little, asks nothing.");
        public final String name, about;

        Kind(String name, String about) {
            this.name = name;
            this.about = about;
        }
    }

    /** An offer: what you get (`item`: an id, "id*count", or "cartridge:CAL*count"), its price in rubles, the trust level, and items it wants instead or as well. */
    public record Offer(Kind trader, String item, int price, int level, List<String> barter) {
    }

    /** A task: kill (target: an operator kind or faction), collect (target: item id, handed in), or extract. */
    public record Task(Kind trader, String id, String title, String type, String target, int count, int reward, float trust) {
    }

    public static final List<Offer> OFFERS = new ArrayList<>();
    public static final List<Task> TASKS = new ArrayList<>();
    /** What things are worth (rubles); anything not listed is guessed (sell price is this x the trader's rate). */
    public static final Map<String, Integer> VALUES = new java.util.HashMap<>();
    public static float[] trustLevels = {0f, 0.35f, 0.75f};

    static {
        Kind Q = Kind.QUARTERMASTER, M = Kind.MEDIC, F = Kind.FENCE;
        o(Q, "factoryascension:mp5", 12000, 1); o(Q, "factoryascension:ak74", 18000, 1); o(Q, "factoryascension:m870", 9000, 1);
        o(Q, "factoryascension:m4a1", 24000, 2); o(Q, "factoryascension:hk416", 32000, 2); o(Q, "factoryascension:awm", 65000, 3);
        o(Q, "cartridge:P9*30", 900, 1); o(Q, "cartridge:R545*30", 1500, 1); o(Q, "cartridge:R556*30", 1700, 1); o(Q, "cartridge:R762S*30", 1600, 1);
        o(Q, "cartridge:G12*16", 900, 1); o(Q, "cartridge:R762N*20", 2400, 2); o(Q, "cartridge:R338*10", 4000, 3);
        o(Q, "factoryascension:plate_carrier", 6000, 1); o(Q, "factoryascension:steel_plate", 4000, 1); o(Q, "factoryascension:ceramic_plate", 7000, 2);
        o(Q, "factoryascension:hybrid_plate", 12000, 2); o(Q, "factoryascension:steel_helmet", 3000, 1); o(Q, "factoryascension:pasgt_helmet", 6000, 1);
        o(Q, "factoryascension:fast_helmet", 14000, 2); o(Q, "factoryascension:altyn_helmet", 30000, 3); o(Q, "factoryascension:nods_green", 25000, 2);
        o(Q, "factoryascension:thermal_goggles", 80000, 3); o(Q, "factoryascension:frag_grenade", 1200, 1); o(Q, "factoryascension:flashbang", 900, 1);
        o(Q, "factoryascension:weapon_cleaning_kit", 2500, 1); o(Q, "factoryascension:raid_map", 1500, 1);
        OFFERS.add(new Offer(Q, "factoryascension:fast_helmet", 0, 2, List.of("factoryascension:dog_tag*6")));
        o(M, "factoryascension:bandage", 300, 1); o(M, "factoryascension:medkit", 2500, 1); o(M, "factoryascension:splint", 900, 1);
        o(M, "factoryascension:tourniquet", 800, 1); o(M, "factoryascension:canteen", 600, 1); o(M, "factoryascension:surgical_kit", 9000, 2);
        o(M, "factoryascension:morphine", 2000, 1); o(M, "factoryascension:adrenaline_shot", 3500, 2); o(M, "factoryascension:energy_drink", 700, 1);
        o(M, "factoryascension:xanax", 1500, 2);
        OFFERS.add(new Offer(M, "factoryascension:surgical_kit", 0, 1, List.of("minecraft:gold_ingot*2", "factoryascension:medkit")));
        o(F, "factoryascension:raid_map", 1000, 1); o(F, "factoryascension:fuel_can", 800, 1); o(F, "factoryascension:dirt_bike", 40000, 2);
        o(F, "factoryascension:quad_bike", 60000, 2); o(F, "factoryascension:pickup_truck", 140000, 3); o(F, "minecraft:iron_ingot*10", 600, 1);
        TASKS.add(new Task(Q, "clean_up", "Clean Up", "kill", "SCAV", 8, 15000, 0.15f));
        TASKS.add(new Task(Q, "tags", "Bring Me Tags", "collect", "factoryascension:dog_tag", 5, 20000, 0.15f));
        TASKS.add(new Task(Q, "deep_strike", "Deep Strike", "extract", "", 3, 30000, 0.25f));
        TASKS.add(new Task(Q, "raider_problem", "The Raider Problem", "kill", "RAIDER", 10, 40000, 0.25f));
        TASKS.add(new Task(M, "supplies", "Supplies", "collect", "factoryascension:cannabis_leaf", 12, 8000, 0.15f));
        TASKS.add(new Task(M, "survivor", "Survivor", "extract", "", 2, 15000, 0.2f));
        TASKS.add(new Task(M, "night_work", "Night Work", "kill", "night_hunter", 1, 35000, 0.3f));
        TASKS.add(new Task(F, "scrap_run", "Scrap Run", "collect", "minecraft:iron_ingot", 32, 6000, 0.15f));
        TASKS.add(new Task(F, "hot_goods", "Hot Goods", "collect", "factoryascension:m82", 1, 50000, 0.3f));
    }

    static void o(Kind k, String item, int price, int level) {
        OFFERS.add(new Offer(k, item, price, level, List.of()));
    }

    // ---- The config bridge (kubejs/startup_scripts/97_fa_mod.js) ----
    public static void clear() {
        OFFERS.clear();
        TASKS.clear();
    }

    public static void offer(String trader, String item, int price, int level, String barter) {
        OFFERS.add(new Offer(Kind.valueOf(trader), item, price, level, barter == null || barter.isEmpty() ? List.of() : List.of(barter.split(","))));
    }

    public static void task(String trader, String id, String title, String type, String target, int count, int reward, double trust) {
        TASKS.add(new Task(Kind.valueOf(trader), id, title, type, target, count, reward, (float) trust));
    }

    public static void setLoyalty(double l1, double l2, double l3) {
        trustLevels = new float[] {(float) l1, (float) l2, (float) l3};
    }

    public static void value(String item, int rubles) {
        VALUES.put(item, rubles);
    }

    // ---- Items: what a spec makes, what things are worth ----
    @Nullable
    public static ItemStack make(String spec) {
        int count = 1;
        String id = spec;
        int star = spec.indexOf('*');
        if (star > 0) {
            count = Integer.parseInt(spec.substring(star + 1).trim());
            id = spec.substring(0, star).trim();
        }
        if (id.startsWith("cartridge:")) {
            try {
                return CartridgeItem.standard(Caliber.valueOf(id.substring(10)), AmmoType.REGULAR, count);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        var it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(id));
        final int n = count;
        return it.map(i -> new ItemStack(i, n)).orElse(null);
    }

    public static int value(ItemStack s) {
        if (s.isEmpty()) return 0;
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        Integer v = VALUES.get(id);
        int each;
        if (v != null) each = v;
        else if (GunItem.spec(s) != null) each = Math.round(2500 * GunItem.spec(s).caliber.damage * (1 + GunItem.spec(s).weight * 0.1f));
        else if (s.getItem() instanceof CartridgeItem) each = 45;
        else if (s.getItem() instanceof dev.factoryascension.armor.PlateItem) each = 2500;
        else if (s.getItem() instanceof dev.factoryascension.armor.HelmetItem h) each = Math.round(h.kind.protection * 1500);
        else if (s.getItem() instanceof dev.factoryascension.gun.AttachmentItem) {
            var a = dev.factoryascension.gun.AttachmentItem.att(s);
            each = a == null ? 200 : a.cost * 350 + a.tier * 400;
        } else if (s.getItem() instanceof dev.factoryascension.drugs.DrugItem) each = 900;
        else if (s.getItem() instanceof net.minecraft.world.item.ArmorItem ai) each = ai.getDefense() * 120;
        else if (s.getItem() instanceof net.minecraft.world.item.TieredItem) each = 150;
        else if (s.isEdible()) each = 15;
        else each = 3;
        return each * s.getCount();
    }

    /** What this trader pays for it (0 = won't buy it). */
    static int buys(Kind k, ItemStack s) {
        int v = value(s);
        if (v <= 0) return 0;
        boolean theirs = switch (k) {
            case QUARTERMASTER -> GunItem.spec(s) != null || s.getItem() instanceof CartridgeItem || s.getItem() instanceof dev.factoryascension.armor.PlateItem
                || s.getItem() instanceof dev.factoryascension.armor.HelmetItem || s.getItem() instanceof dev.factoryascension.gun.AttachmentItem
                || s.getItem() == FactoryAscension.DOG_TAG.get() || s.getItem() instanceof net.minecraft.world.item.ArmorItem;
            case MEDIC -> s.getItem() instanceof dev.factoryascension.drugs.DrugItem || s.getItem() instanceof dev.factoryascension.body.Treatment.UseItem
                || s.getItem() == FactoryAscension.BANDAGE.get() || s.getItem() == FactoryAscension.MEDKIT.get()
                || BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().matches(".*(leaf|seeds|mushroom|flower|herb).*");
            case FENCE -> true;
        };
        if (!theirs) return 0;
        return Math.max(1, Math.round(v * (k == Kind.FENCE ? 0.55f : 0.8f)));
    }

    // ---- The wallet, trust, tasks (player persistent data "FATraders") ----
    static CompoundTag data(ServerPlayer p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.getCompound("FATraders");
    }

    static void put(ServerPlayer p, CompoundTag t) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put("FATraders", t);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static long rubles(ServerPlayer p) {
        return data(p).getLong("Rubles");
    }

    public static void addRubles(ServerPlayer p, long n) {
        CompoundTag t = data(p);
        t.putLong("Rubles", Math.max(0, t.getLong("Rubles") + n));
        put(p, t);
    }

    static float trust(ServerPlayer p, Kind k) {
        return data(p).getFloat("Trust_" + k.name());
    }

    static int level(ServerPlayer p, Kind k) {
        float t = trust(p, k);
        int l = 1;
        for (int i = 0; i < trustLevels.length; i++) if (t >= trustLevels[i]) l = i + 1;
        return l;
    }

    static void addTrust(ServerPlayer p, Kind k, float n) {
        CompoundTag t = data(p);
        t.putFloat("Trust_" + k.name(), t.getFloat("Trust_" + k.name()) + n);
        put(p, t);
    }

    /** 0 not taken, 1 taken, 2 done */
    static int state(ServerPlayer p, Task task) {
        return data(p).getInt("Task_" + task.id());
    }

    static int progress(ServerPlayer p, Task task) {
        return data(p).getInt("Prog_" + task.id());
    }

    /** Something happened that tasks might count: "kill" (target: kind id|FACTION), "extract". */
    public static void event(ServerPlayer p, String type, String target, int n) {
        CompoundTag t = data(p);
        boolean changed = false;
        for (Task task : TASKS) {
            if (!task.type().equals(type) || t.getInt("Task_" + task.id()) != 1) continue;
            if (type.equals("kill") && !target.contains(task.target())) continue;
            int prog = Math.min(task.count(), t.getInt("Prog_" + task.id()) + n);
            t.putInt("Prog_" + task.id(), prog);
            if (prog >= task.count()) p.displayClientMessage(Component.literal("Task ready to hand in: " + task.title()).withStyle(ChatFormatting.GOLD), false);
            changed = true;
        }
        if (changed) put(p, t);
    }

    static void kills(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (event.getEntity() instanceof dev.factoryascension.npc.Operator o) event(p, "kill", o.type.id + "|" + o.type.faction.name(), 1);
        else if (event.getEntity() instanceof dev.factoryascension.npc.NightHunter) event(p, "kill", "night_hunter", 1);
    }

    // ---- Trading ----
    public static void open(ServerPlayer p, Trader trader) {
        FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new FANetwork.TraderView(view(p, trader)));
    }

    static CompoundTag view(ServerPlayer p, Trader trader) {
        Kind k = trader.kind();
        CompoundTag v = new CompoundTag();
        v.putInt("Id", trader.getId());
        v.putString("Name", k.name);
        v.putString("About", k.about);
        v.putLong("Rubles", rubles(p));
        v.putInt("Level", level(p, k));
        v.putFloat("Trust", trust(p, k));
        ListTag offers = new ListTag();
        for (int i = 0; i < OFFERS.size(); i++) {
            Offer o = OFFERS.get(i);
            if (o.trader() != k) continue;
            ItemStack st = make(o.item());
            if (st == null) continue;
            CompoundTag e = new CompoundTag();
            e.putInt("I", i);
            e.put("Item", st.save(new CompoundTag()));
            e.putInt("Price", o.price());
            e.putInt("Lvl", o.level());
            StringBuilder b = new StringBuilder();
            for (String s : o.barter()) {
                ItemStack bs = make(s);
                if (bs != null) b.append(b.length() > 0 ? ", " : "").append(bs.getCount()).append("x ").append(bs.getHoverName().getString());
            }
            e.putString("Barter", b.toString());
            offers.add(e);
        }
        v.put("Offers", offers);
        ListTag sell = new ListTag();
        var inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack s = inv.items.get(i);
            int price = buys(k, s);
            if (price <= 0) continue;
            CompoundTag e = new CompoundTag();
            e.putInt("Slot", i);
            e.put("Item", s.save(new CompoundTag()));
            e.putInt("Price", price);
            sell.add(e);
        }
        v.put("Sell", sell);
        ListTag tasks = new ListTag();
        for (int i = 0; i < TASKS.size(); i++) {
            Task t = TASKS.get(i);
            if (t.trader() != k) continue;
            CompoundTag e = new CompoundTag();
            e.putInt("I", i);
            e.putString("Title", t.title());
            e.putString("Desc", describe(t));
            e.putInt("State", state(p, t));
            int prog = t.type().equals("collect") ? Math.min(t.count(), count(p, t.target())) : progress(p, t);
            e.putInt("Prog", prog);
            e.putInt("Count", t.count());
            e.putInt("Reward", t.reward());
            tasks.add(e);
        }
        v.put("Tasks", tasks);
        return v;
    }

    static String describe(Task t) {
        return switch (t.type()) {
            case "kill" -> "Kill " + t.count() + " " + (t.target().equals("night_hunter") ? "night hunter" + (t.count() > 1 ? "s" : "") : t.target().toLowerCase().replace('_', ' ') + (t.count() > 1 ? "s" : ""));
            case "extract" -> "Extract from " + t.count() + " raid" + (t.count() > 1 ? "s" : "");
            default -> {
                ItemStack s = make(t.target());
                yield "Bring " + t.count() + " " + (s == null ? t.target() : s.getHoverName().getString());
            }
        };
    }

    static int count(ServerPlayer p, String spec) {
        ItemStack want = make(spec);
        if (want == null) return 0;
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(want.getItem())) n += s.getCount();
        return n;
    }

    static boolean take(ServerPlayer p, String spec, int count) {
        ItemStack want = make(spec);
        if (want == null || count(p, spec) < count) return false;
        int n = count;
        for (ItemStack s : p.getInventory().items) {
            if (n <= 0) break;
            if (!s.is(want.getItem())) continue;
            int k = Math.min(n, s.getCount());
            s.shrink(k);
            n -= k;
        }
        return true;
    }

    public static void action(ServerPlayer p, int traderId, int kind, int index) {
        if (!(p.level().getEntity(traderId) instanceof Trader trader) || trader.distanceToSqr(p) > 64) return;
        Kind k = trader.kind();
        switch (kind) {
            case FANetwork.TraderAction.BUY -> {
                if (index < 0 || index >= OFFERS.size() || OFFERS.get(index).trader() != k) return;
                Offer o = OFFERS.get(index);
                if (level(p, k) < o.level()) { say(p, "They don't trust you enough for that yet"); break; }
                if (rubles(p) < o.price()) { say(p, "Not enough rubles"); break; }
                for (String b : o.barter()) {
                    ItemStack bs = make(b);
                    if (bs == null || count(p, b.contains("*") ? b.substring(0, b.indexOf('*')) : b) < bs.getCount()) { say(p, "You don't have what they want for it"); open(p, trader); return; }
                }
                for (String b : o.barter()) {
                    ItemStack bs = make(b);
                    take(p, b.contains("*") ? b.substring(0, b.indexOf('*')) : b, bs.getCount());
                }
                ItemStack got = make(o.item());
                if (got == null) return;
                addRubles(p, -o.price());
                addTrust(p, k, o.price() / 1_000_000f);
                if (!p.getInventory().add(got)) p.drop(got, false);
                p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.6f, 0.8f);
            }
            case FANetwork.TraderAction.SELL -> {
                var inv = p.getInventory();
                if (index < 0 || index >= inv.items.size()) return;
                ItemStack s = inv.items.get(index);
                int price = buys(k, s);
                if (price <= 0) return;
                inv.items.set(index, ItemStack.EMPTY);
                addRubles(p, price);
                addTrust(p, k, price / 2_000_000f);
                p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.6f, 0.8f);
            }
            case FANetwork.TraderAction.TAKE -> {
                if (index < 0 || index >= TASKS.size() || TASKS.get(index).trader() != k) return;
                Task t = TASKS.get(index);
                if (state(p, t) != 0) return;
                CompoundTag d = data(p);
                d.putInt("Task_" + t.id(), 1);
                d.putInt("Prog_" + t.id(), 0);
                put(p, d);
                say(p, "Task taken: " + t.title());
            }
            case FANetwork.TraderAction.HAND_IN -> {
                if (index < 0 || index >= TASKS.size() || TASKS.get(index).trader() != k) return;
                Task t = TASKS.get(index);
                if (state(p, t) != 1) return;
                boolean done = t.type().equals("collect") ? take(p, t.target(), t.count()) : progress(p, t) >= t.count();
                if (!done) { say(p, "Not done yet"); break; }
                CompoundTag d = data(p);
                d.putInt("Task_" + t.id(), 2);
                put(p, d);
                addRubles(p, t.reward());
                addTrust(p, k, t.trust());
                p.displayClientMessage(Component.literal("Task complete: " + t.title() + " (+" + t.reward() + " ₽)").withStyle(ChatFormatting.GREEN), false);
                p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.1f);
            }
            default -> { }
        }
        open(p, trader);
    }

    static void say(ServerPlayer p, String s) {
        p.displayClientMessage(Component.literal(s).withStyle(ChatFormatting.YELLOW), true);
    }

    // ---- Items: contracts that bring a trader, cash that goes in the wallet ----
    public static class Contract extends Item {
        final Kind kind;

        public Contract(Kind kind, Properties p) {
            super(p.stacksTo(1));
            this.kind = kind;
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
            Trader t = RaidEntities.TRADER.get().create(ctx.getLevel());
            if (t == null) return InteractionResult.FAIL;
            var at = ctx.getClickedPos().relative(ctx.getClickedFace());
            t.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() + 180 : 0, 0);
            t.setKind(kind);
            ctx.getLevel().addFreshEntity(t);
            if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
            return InteractionResult.CONSUME;
        }
    }

    public static class Cash extends Item {
        public Cash(Properties p) {
            super(p.stacksTo(64));
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, net.minecraft.world.InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                int n = player.isShiftKeyDown() ? st.getCount() : 1;
                addRubles(sp, (long) n * dev.factoryascension.FAConfig.cashBundleValue);
                st.shrink(n);
                sp.displayClientMessage(Component.literal("Wallet: " + rubles(sp) + " ₽").withStyle(ChatFormatting.GOLD), true);
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 1.2f);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(st, level.isClientSide);
        }
    }

    public static final Map<Kind, RegistryObject<Item>> CONTRACTS = new EnumMap<>(Kind.class);
    public static final RegistryObject<Item> CASH = FactoryAscension.ITEMS.register("cash_bundle", () -> new Cash(new Item.Properties()));

    static {
        for (Kind k : Kind.values())
            CONTRACTS.put(k, FactoryAscension.ITEMS.register("trader_contract_" + k.name().toLowerCase(), () -> new Contract(k, new Item.Properties())));
    }

    static void attributes(EntityAttributeCreationEvent event) {
        event.put(RaidEntities.TRADER.get(), Trader.attributes().build());
    }

    static void init(IEventBus bus) {
        bus.addListener(Traders::attributes);
        MinecraftForge.EVENT_BUS.addListener(Traders::kills);
    }
}

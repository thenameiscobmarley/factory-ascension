package dev.factoryascension.raid;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.network.FANetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegistryObject;

/**
 * Extraction raids (like Tarkov). A Raid Map starts one: three extraction points are marked a few hundred blocks out in
 * different directions (green smoke rising from each), and a clock starts. Reach one and stay in it for a few seconds and
 * you're out: home, with an XP bonus, your nerves settled, and it counts for traders' tasks. Run out of time and you're
 * missing in action: marked, and hunted, until you make it home. Die out there and everything you carried stays where
 * you fell, in a body bag that lasts a day (raid/BodyBag); anyone can go back for it. All in the config (extraction).
 */
public final class Extraction {
    private Extraction() {
    }

    static final String[] NAMES = {"Old Gas Station", "Rail Bridge", "Dead Man's Road", "Collapsed Tunnel", "Radio Mast", "Burned Farm",
        "Quarry Gate", "Crash Site", "Water Tower", "Checkpoint Zero", "Smuggler's Path", "The Old Church", "Power Lines", "Dam Spillway"};

    public static class RaidMap extends Item {
        public RaidMap(Properties p) {
            super(p.stacksTo(4));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack st = player.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(st);
            ServerPlayer p = (ServerPlayer) player;
            if (active(p)) {
                p.displayClientMessage(Component.literal("You're already on a raid").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(st);
            }
            start(p);
            if (!p.isCreative()) st.shrink(1);
            return InteractionResultHolder.consume(st);
        }
    }

    public static final RegistryObject<Item> RAID_MAP = FactoryAscension.ITEMS.register("raid_map", () -> new RaidMap(new Item.Properties()));

    static CompoundTag data(ServerPlayer p) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.getCompound("FARaid");
    }

    static void put(ServerPlayer p, CompoundTag t) {
        CompoundTag persisted = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put("FARaid", t);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static boolean active(ServerPlayer p) {
        return data(p).getBoolean("On");
    }

    public static void start(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        CompoundTag t = new CompoundTag();
        t.putBoolean("On", true);
        t.putLong("End", level.getGameTime() + FAConfig.raidMinutes * 1200L);
        t.putString("Dim", level.dimension().location().toString());
        ListTag pts = new ListTag();
        var r = p.getRandom();
        double base = r.nextDouble() * Math.PI * 2;
        java.util.List<String> names = new java.util.ArrayList<>(java.util.List.of(NAMES));
        java.util.Collections.shuffle(names, new java.util.Random(r.nextLong()));
        for (int i = 0; i < 3; i++) {
            double a = base + i * Math.PI * 2 / 3 + (r.nextDouble() - 0.5) * 0.8;
            double d = FAConfig.raidMinDist + r.nextDouble() * (FAConfig.raidMaxDist - FAConfig.raidMinDist);
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            CompoundTag e = new CompoundTag();
            e.putInt("X", x);
            e.putInt("Z", z);
            e.putString("Name", names.get(i));
            pts.add(e);
        }
        t.put("Points", pts);
        put(p, t);
        p.displayClientMessage(Component.literal("Raid started: " + FAConfig.raidMinutes + " minutes. Extraction: " + names.get(0) + ", " + names.get(1) + ", " + names.get(2)
            + ". Look for the green smoke.").withStyle(ChatFormatting.GOLD), false);
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1, 0.6f);
        sync(p);
    }

    static void end(ServerPlayer p, String why) {
        CompoundTag t = data(p);
        t.putBoolean("On", false);
        put(p, t);
        FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new FANetwork.RaidSync(new int[0], new String[0], 0, 0, why));
    }

    static void sync(ServerPlayer p) {
        CompoundTag t = data(p);
        ListTag pts = t.getList("Points", 10);
        int[] xz = new int[pts.size() * 2];
        String[] names = new String[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            xz[i * 2] = pts.getCompound(i).getInt("X");
            xz[i * 2 + 1] = pts.getCompound(i).getInt("Z");
            names[i] = pts.getCompound(i).getString("Name");
        }
        int left = (int) Math.max(0, t.getLong("End") - p.level().getGameTime());
        FANetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new FANetwork.RaidSync(xz, names, left, t.getInt("Hold"), ""));
    }

    static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        CompoundTag t = data(p);
        if (!t.getBoolean("On")) return;
        ServerLevel level = p.serverLevel();
        if (!level.dimension().location().toString().equals(t.getString("Dim"))) return;
        long left = t.getLong("End") - level.getGameTime();
        ListTag pts = t.getList("Points", 10);
        // Green smoke over each point you're anywhere near
        boolean inside = false;
        String at = "";
        for (int i = 0; i < pts.size(); i++) {
            CompoundTag e = pts.getCompound(i);
            int x = e.getInt("X"), z = e.getInt("Z");
            double d = Math.hypot(p.getX() - x, p.getZ() - z);
            if (d < 192 && level.hasChunkAt(new BlockPos(x, 0, z))) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int k = 0; k < 6; k++) level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x + 0.5, y + 1 + k * 0.3, z + 0.5, 1, 0.3, 0.2, 0.3, 0.01);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x + 0.5, y + 1.5, z + 0.5, 3, 1.5, 0.5, 1.5, 0);
            }
            if (d < FAConfig.raidExtractRadius) {
                inside = true;
                at = e.getString("Name");
            }
        }
        int hold = inside ? t.getInt("Hold") + 1 : 0;
        t.putInt("Hold", hold);
        put(p, t);
        if (inside && hold >= FAConfig.raidExtractSeconds) {
            extracted(p, at);
            return;
        }
        if (inside) p.displayClientMessage(Component.literal("Extracting at " + at + "... " + (FAConfig.raidExtractSeconds - hold)).withStyle(ChatFormatting.GREEN), true);
        if (left <= 0) {
            missing(p);
            return;
        }
        sync(p);
    }

    static void extracted(ServerPlayer p, String where) {
        end(p, "extracted");
        p.displayClientMessage(Component.literal("Extracted at " + where + ". You made it.").withStyle(ChatFormatting.GREEN), false);
        p.giveExperiencePoints(FAConfig.raidExtractXp);
        dev.factoryascension.gun.Stress.set(p, 0);
        p.getPersistentData().putInt("FAExtracts", p.getPersistentData().getInt("FAExtracts") + 1);
        Traders.event(p, "extract", "", 1);
        // Home: your bed if you have one, else world spawn
        ServerLevel level = p.server.getLevel(p.getRespawnDimension());
        BlockPos bed = p.getRespawnPosition();
        if (level == null || bed == null) {
            level = p.server.overworld();
            bed = level.getSharedSpawnPos();
        }
        p.teleportTo(level, bed.getX() + 0.5, bed.getY() + 1, bed.getZ() + 0.5, p.getYRot(), p.getXRot());
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1, 0.8f);
    }

    static void missing(ServerPlayer p) {
        end(p, "mia");
        p.displayClientMessage(Component.literal("Out of time: missing in action. They know you're out here.").withStyle(ChatFormatting.DARK_RED), false);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(dev.factoryascension.npc.Raids.MARKED.get(), FAConfig.raidMarkTicks, 1));
        dev.factoryascension.npc.Operators.hunters(p.serverLevel(), p.position(), 3);
    }

    /** Killed on a raid: everything you carried goes into a body bag where you fell. */
    static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !active(p) || !FAConfig.raidBodyBags) return;
        BodyBag bag = new BodyBag(RaidEntities.BODY_BAG.get(), p.level());
        bag.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        bag.owner = p.getGameProfile().getName();
        int i = 0;
        for (var e : event.getDrops()) {
            if (i >= bag.items.getContainerSize()) break;
            bag.items.setItem(i++, e.getItem().copy());
        }
        event.getDrops().clear();
        p.level().addFreshEntity(bag);
        end(p, "kia");
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Extraction::tick);
        MinecraftForge.EVENT_BUS.addListener(Extraction::drops);
    }
}

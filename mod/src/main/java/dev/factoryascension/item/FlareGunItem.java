package dev.factoryascension.item;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Flare gun: the shotgun's controls (hold right-click to aim, left-click to fire, R to load), one flare at a time.
 * A red flare is a signal: a bright red star high in the sky. A green flare calls an airdrop: after FAConfig.airdropDelay
 * a supply crate falls from the sky a few blocks ahead of where you fired, under a green smoke column, and lands as a
 * chest filled from the airdrop loot table. R with a flare loaded swaps it for the other colour, if you have one.
 */
public class FlareGunItem extends ShotgunItem {
    public static final int RED = 1, GREEN = 2;

    public FlareGunItem(Properties props) {
        super(props);
    }

    public static int loaded(ItemStack gun) {
        return gun.hasTag() ? gun.getTag().getInt("Flare") : 0;
    }

    private static void setLoaded(ItemStack gun, int colour) {
        gun.getOrCreateTag().putInt("Flare", colour);
        gun.getOrCreateTag().putInt("Shells", colour == 0 ? 0 : 1);   // the shared gun controls read "Shells"
    }

    private static Item shell(int colour) {
        return colour == RED ? FactoryAscension.FLARE_RED.get() : FactoryAscension.FLARE_GREEN.get();
    }

    private static int colourOf(ItemStack s) {
        if (s.is(FactoryAscension.FLARE_RED.get())) return RED;
        if (s.is(FactoryAscension.FLARE_GREEN.get())) return GREEN;
        return 0;
    }

    // ---- Loading (R) ----
    public static void reload(ServerPlayer player, ItemStack gun) {
        if (!(gun.getItem() instanceof FlareGunItem)) return;
        int now = loaded(gun);
        int want = now == 0 ? 0 : (now == RED ? GREEN : RED);
        for (ItemStack s : player.getInventory().items) {
            int c = colourOf(s);
            if (c == 0 || (want != 0 && c != want)) continue;
            s.shrink(1);
            if (now != 0 && !player.getInventory().add(new ItemStack(shell(now)))) player.drop(new ItemStack(shell(now)), false);
            setLoaded(gun, c);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.7f, 1.2f);
            player.displayClientMessage(Component.literal(c == RED ? "Red flare loaded" : "Green flare loaded (airdrop)")
                .withStyle(c == RED ? ChatFormatting.RED : ChatFormatting.GREEN), true);
            return;
        }
        if (now == 0) player.displayClientMessage(Component.literal("No flares").withStyle(ChatFormatting.RED), true);
    }

    // ---- Firing ----
    public static void fire(ServerPlayer player) {
        ItemStack gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof FlareGunItem) || player.getCooldowns().isOnCooldown(gun.getItem())) return;
        ServerLevel level = player.serverLevel();
        int colour = loaded(gun);
        if (colour == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7f, 1.6f);
            reload(player, gun);
            return;
        }
        if (!player.isCreative()) setLoaded(gun, 0);
        player.getCooldowns().addCooldown(gun.getItem(), FAConfig.flareCooldown);
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1);
        FireworkRocketEntity flare = new FireworkRocketEntity(level, rocket(colour), player, eye.x + look.x, eye.y + look.y - 0.1, eye.z + look.z, true);
        flare.shoot(look.x, look.y, look.z, FAConfig.flareSpeed, 1.0f);
        level.addFreshEntity(flare);
        level.sendParticles(ParticleTypes.FLAME, eye.x + look.x * 1.2, eye.y + look.y * 1.2 - 0.2, eye.z + look.z * 1.2, 5, 0.04, 0.04, 0.04, 0.02);
        dev.factoryascension.FxEvents.smoke(level, eye.add(look.scale(1.2)), 0.3f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.4f, 0.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 1.9f);
        if (colour == RED) dev.factoryascension.gun.Rescue.call(player);
        if (colour == GREEN) {
            Vec3 ahead = new Vec3(look.x, 0, look.z);
            ahead = ahead.lengthSqr() < 1e-4 ? Vec3.ZERO : ahead.normalize().scale(8);
            callAirdrop(level, BlockPos.containing(player.getX() + ahead.x, player.getY(), player.getZ() + ahead.z), FAConfig.airdropDelay);
            player.displayClientMessage(Component.literal("Airdrop incoming in " + Math.max(1, FAConfig.airdropDelay / 20) + " seconds: watch for the green smoke")
                .withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** A firework star the flare carries: one bright ball with a trail in its colour. */
    private static ItemStack rocket(int colour) {
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag fw = rocket.getOrCreateTagElement("Fireworks");
        fw.putByte("Flight", (byte) 3);
        CompoundTag star = new CompoundTag();
        star.putByte("Type", (byte) 0);
        star.putBoolean("Trail", true);
        star.putBoolean("Flicker", true);
        star.putIntArray("Colors", colour == RED ? new int[] {0xFF2A1A, 0xFF6040} : new int[] {0x3CFF4A, 0x9CFF70});
        star.putIntArray("FadeColors", new int[] {0xFFFFFF});
        ListTag list = new ListTag();
        list.add(star);
        fw.put("Explosions", list);
        return rocket;
    }

    // ---- Airdrops: waiting (green smoke), then a crate falls ----
    public record Airdrop(ServerLevel level, int x, int z, long at) {
    }

    private static final List<Airdrop> PENDING = new ArrayList<>();
    /** For the self-test: the last crate dropped. */
    @Nullable public static FallingBlockEntity lastCrate;

    public static void callAirdrop(ServerLevel level, BlockPos near, int delayTicks) {
        PENDING.add(new Airdrop(level, near.getX(), near.getZ(), level.getGameTime() + delayTicks));
    }

    public static int pending() {
        return PENDING.size();
    }

    /** For the self-test: forget the airdrops on their way. */
    public static void cancelAirdrops() {
        PENDING.clear();
    }

    /** Drop the crate now: a falling chest that lands as a chest with the airdrop loot. */
    public static FallingBlockEntity dropCrate(ServerLevel level, int x, int z) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int y = Math.min(level.getMaxBuildHeight() - 2, ground + FAConfig.airdropHeight);
        BlockPos from = new BlockPos(x, y, z);
        if (!level.getBlockState(from).isAir()) return null;
        FallingBlockEntity crate = FallingBlockEntity.fall(level, from, Blocks.CHEST.defaultBlockState());
        CompoundTag data = new CompoundTag();
        data.putString("LootTable", new ResourceLocation(FAConfig.airdropLoot).toString());
        data.putLong("LootTableSeed", level.getRandom().nextLong());
        data.putString("CustomName", Component.Serializer.toJson(Component.literal("Airdrop")));
        crate.blockData = data;
        crate.time = 1;
        level.playSound(null, x, ground + 4, z, SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, 1.2f, 0.6f);
        lastCrate = crate;
        return crate;
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        @SubscribeEvent
        public static void tick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
            PENDING.removeIf(drop -> {
                ServerLevel level = drop.level;
                long left = drop.at - level.getGameTime();
                if (left > 0) {
                    // A green smoke column marks the spot while you wait
                    if (left % 4 == 0) {
                        double gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, drop.x, drop.z) + 0.2;
                        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, drop.x + 0.5, gy, drop.z + 0.5, 1, 0.1, 0.1, 0.1, 0.02);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, drop.x + 0.5, gy + 0.6, drop.z + 0.5, 3, 0.3, 0.6, 0.3, 0);
                    }
                    return false;
                }
                try {
                    dropCrate(level, drop.x, drop.z);
                } catch (Throwable err) {
                    org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Airdrop failed", err);
                }
                return true;
            });
        }

        @SubscribeEvent
        public static void stop(ServerStoppingEvent event) {
            PENDING.clear();
            lastCrate = null;
        }
    }

    // ---- Shown as one shot ----
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return loaded(stack) != 0 ? 13 : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return loaded(stack) == GREEN ? 0x3CFF4A : 0xFF3A2A;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        int c = loaded(stack);
        tip.add(c == 0 ? Component.literal("Empty").withStyle(ChatFormatting.GRAY)
            : Component.literal(c == RED ? "Red flare loaded: a signal" : "Green flare loaded: calls an airdrop").withStyle(c == RED ? ChatFormatting.RED : ChatFormatting.GREEN));
        tip.add(Component.literal("Green flares call a supply crate after " + Math.max(1, FAConfig.airdropDelay / 20) + " seconds.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Left-click: fire. R: load (again to swap colour).").withStyle(ChatFormatting.DARK_GRAY));
    }
}

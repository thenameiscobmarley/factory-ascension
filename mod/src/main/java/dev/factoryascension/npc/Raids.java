package dev.factoryascension.npc;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Hit a held place and they remember you. Killing an operator that was holding a structure marks you ("Marked", for
 * a few days; the more you kill, the stronger it gets). While you're marked, now and then a raid party comes for you:
 * to your bed (your respawn point) if you're near it, otherwise wherever you are. You hear them before you see them -
 * they talk among themselves (villager and pillager voices), they bash in wooden doors, trapdoors, gates and glass,
 * and they bring grenades. Build for it: ballistic concrete, ballistic glass (and panes) and steel doors don't break,
 * stop bullets (gun/GunItem) and shrug off blasts.
 */
public final class Raids {
    private Raids() {
    }

    /** Doesn't break for raiders, stops bullets. */
    public interface Ballistic {
    }

    public static class BallisticBlock extends Block implements Ballistic {
        public BallisticBlock(Properties p) {
            super(p);
        }
    }

    public static class BallisticGlass extends AbstractGlassBlock implements Ballistic {
        public BallisticGlass(Properties p) {
            super(p);
        }
    }

    public static class BallisticPane extends IronBarsBlock implements Ballistic {
        public BallisticPane(Properties p) {
            super(p);
        }
    }

    public static class SteelDoor extends DoorBlock implements Ballistic {
        public SteelDoor(Properties p, BlockSetType t) {
            super(p, t);
        }
    }

    public static class SteelTrapdoor extends TrapDoorBlock implements Ballistic {
        public SteelTrapdoor(Properties p, BlockSetType t) {
            super(p, t);
        }
    }

    /** Steel that opens by hand (iron doors only open with redstone). */
    public static final BlockSetType STEEL = BlockSetType.register(new BlockSetType("factoryascension_steel", true, SoundType.METAL,
        SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
        SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));

    static BlockBehaviour.Properties armored() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(25f, 1200f).sound(SoundType.STONE);
    }

    public static final RegistryObject<Block> CONCRETE = block("ballistic_concrete", () -> new BallisticBlock(armored()));
    public static final RegistryObject<Block> GLASS = block("ballistic_glass", () -> new BallisticGlass(BlockBehaviour.Properties.of()
        .mapColor(MapColor.NONE).requiresCorrectToolForDrops().strength(12f, 1200f).sound(SoundType.GLASS).noOcclusion()
        .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final RegistryObject<Block> PANE = block("ballistic_glass_pane", () -> new BallisticPane(BlockBehaviour.Properties.of()
        .mapColor(MapColor.NONE).requiresCorrectToolForDrops().strength(10f, 1200f).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Block> STEEL_DOOR = FactoryAscension.BLOCKS.register("steel_door", () -> new SteelDoor(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(20f, 1200f).noOcclusion(), STEEL));
    public static final RegistryObject<Item> STEEL_DOOR_ITEM = FactoryAscension.ITEMS.register("steel_door", () -> new DoubleHighBlockItem(STEEL_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Block> STEEL_TRAPDOOR = block("steel_trapdoor", () -> new SteelTrapdoor(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(20f, 1200f).noOcclusion().isValidSpawn((s, l, p, e) -> false), STEEL));

    public static final List<RegistryObject<Item>> ITEMS = new ArrayList<>();

    static RegistryObject<Block> block(String name, java.util.function.Supplier<Block> make) {
        RegistryObject<Block> b = FactoryAscension.BLOCKS.register(name, make);
        ITEMS.add(FactoryAscension.ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    public static final RegistryObject<MobEffect> MARKED = FactoryAscension.EFFECTS.register("marked", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8A1A12) {
    });

    public static boolean ballistic(BlockState s) {
        return s.getBlock() instanceof Ballistic;
    }

    // ---- Being marked ----

    static void killed(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Operator o) || !o.fromStructure) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        if (!FAConfig.raids) return;
        CompoundTag d = p.getPersistentData();
        int kills = d.getInt("FARaidKills") + 1;
        d.putInt("FARaidKills", kills);
        d.putString("FARaidLead", o.type.name());
        boolean was = p.hasEffect(MARKED.get());
        int amp = Math.min(2, kills / Math.max(1, FAConfig.raidKillsPerLevel));
        p.addEffect(new MobEffectInstance(MARKED.get(), FAConfig.raidMarkTicks, amp, false, false, true));
        if (!d.contains("FARaidNext") || !was) d.putLong("FARaidNext", p.level().getGameTime() + FAConfig.raidFirstDelay);
        if (!was) p.displayClientMessage(Component.literal("You've been marked. They know who hit them, and they'll come for you.")
            .withStyle(ChatFormatting.DARK_RED), false);
    }

    // ---- Sending raids ----

    static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        CompoundTag d = p.getPersistentData();
        MobEffectInstance mark = p.getEffect(MARKED.get());
        if (mark == null) {
            if (d.contains("FARaidKills")) {
                d.remove("FARaidKills");
                d.remove("FARaidNext");
            }
            return;
        }
        if (!FAConfig.raids || p.isCreative() || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (now < d.getLong("FARaidNext")) return;
        RandomSource r = p.getRandom();
        int amp = mark.getAmplifier();
        d.putLong("FARaidNext", now + Math.round(FAConfig.raidInterval * (0.7 + r.nextDouble() * 0.6) / (1 + amp * 0.4)));
        send(level, p, amp);
    }

    /** A raid party for p: to its bed if it's near it, otherwise to where it is. */
    public static int send(ServerLevel level, ServerPlayer p, int amp) {
        RandomSource r = level.getRandom();
        Vec3 target = p.position();
        BlockPos bed = p.getRespawnPosition();
        boolean home = bed != null && p.getRespawnDimension() == level.dimension() && bed.distSqr(p.blockPosition()) < 160 * 160;
        if (home) target = Vec3.atCenterOf(bed);
        OpType lead;
        try {
            lead = OpType.valueOf(p.getPersistentData().getString("FARaidLead"));
        } catch (IllegalArgumentException e) {
            lead = OpType.RAIDER;
        }
        // Snipers and bosses don't lead raids: send their side's assaulters
        lead = switch (lead.faction) {
            case SCAV -> OpType.SCAV_RIFLEMAN;
            case RAIDER -> OpType.BREACHER;
            case ROGUE -> OpType.ROGUE;
            case CULT -> OpType.CULTIST;
            case BANDIT -> OpType.BODYGUARD;
        };
        double a = r.nextDouble() * Math.PI * 2;
        double dist = 40 + r.nextDouble() * 20;
        int n = FAConfig.raidSize + amp * 2 + r.nextInt(2);
        final Vec3 goal = target;
        final ServerPlayer victim = p;
        int made = Operators.squad(level, target.x + Math.cos(a) * dist, target.z + Math.sin(a) * dist, lead, n, goal, o -> {
            o.addTag("FARaid");
            o.setPersistenceRequired();
            o.startAlert(2);
            o.investigate = goal;
            o.investigateUntil = level.getGameTime() + 6000;
            o.getPersistentData().putUUID("FARaidOn", victim.getUUID());
        });
        if (made > 0) {
            p.displayClientMessage(Component.literal(home ? "Voices outside. Someone's come for you." : "Voices, getting closer. They've found you.")
                .withStyle(ChatFormatting.RED), false);
            level.playSound(null, target.x + Math.cos(a) * 20, target.y, target.z + Math.sin(a) * 20, SoundEvents.PILLAGER_AMBIENT, SoundSource.HOSTILE, 2.5f, 0.75f);
        }
        return made;
    }

    // ---- Raiders: talking, breaking in ----

    static void raiderTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Operator o) || o.level().isClientSide || !o.getTags().contains("FARaid")) return;
        RandomSource r = o.getRandom();
        // Talking among themselves
        if (r.nextInt(140) == 0) {
            SoundEvent s = switch (o.type.faction) {
                case SCAV -> SoundEvents.VILLAGER_AMBIENT;
                case CULT -> SoundEvents.EVOKER_AMBIENT;
                case RAIDER -> SoundEvents.PILLAGER_AMBIENT;
                default -> r.nextBoolean() ? SoundEvents.VINDICATOR_AMBIENT : SoundEvents.PILLAGER_AMBIENT;
            };
            o.level().playSound(null, o.getX(), o.getY(), o.getZ(), s, SoundSource.HOSTILE, 1.4f, 0.7f + r.nextFloat() * 0.25f);
        }
        // Once the victim is close, they know exactly where it is
        if (o.tickCount % 40 == 0 && o.getPersistentData().hasUUID("FARaidOn") && o.getTarget() == null) {
            var v = ((ServerLevel) o.level()).getPlayerByUUID(o.getPersistentData().getUUID("FARaidOn"));
            if (v != null && v.distanceToSqr(o) < 24 * 24 && !v.isCreative() && !v.isSpectator()) {
                o.investigate = v.position();
                o.investigateUntil = o.level().getGameTime() + 400;
            }
        }
        // Breaking in: whatever's in their way that isn't ballistic
        if (!FAConfig.raidBreaksDoors || o.tickCount % 5 != 0) return;
        if (!o.horizontalCollision && !o.getNavigation().isStuck() && !(o.getNavigation().isDone() && o.investigate != null)) {
            o.getPersistentData().remove("FABreach");
            return;
        }
        BlockPos at = null;
        Vec3 look = Vec3.directionFromRotation(0, o.getYRot());
        for (double up : new double[] {0.5, 1.5}) {
            for (double fwd : new double[] {0.6, 1.1}) {
                BlockPos b = BlockPos.containing(o.getX() + look.x * fwd, o.getY() + up, o.getZ() + look.z * fwd);
                if (breakable(o.level().getBlockState(b), o.level(), b)) {
                    at = b;
                    break;
                }
            }
            if (at != null) break;
        }
        CompoundTag d = o.getPersistentData();
        if (at == null) {
            d.remove("FABreach");
            return;
        }
        long key = at.asLong();
        int prog = d.getLong("FABreachAt") == key ? d.getInt("FABreach") : 0;
        prog += o.type == OpType.BREACHER || o.type == OpType.JUGGERNAUT ? 3 : 1;
        d.putLong("FABreachAt", key);
        d.putInt("FABreach", prog);
        int need = 12;
        BlockState s = o.level().getBlockState(at);
        o.level().playSound(null, at, s.getSoundType().getHitSound(), SoundSource.BLOCKS, 1.0f, 0.7f);
        o.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        o.level().destroyBlockProgress(o.getId(), at, Math.min(9, prog * 10 / need));
        if (prog >= need) {
            o.level().destroyBlock(at, true, o);
            o.level().destroyBlockProgress(o.getId(), at, -1);
            d.remove("FABreach");
        }
    }

    /** What a raider can break through: wooden doors, trapdoors, gates, glass, panes, nothing ballistic. */
    static boolean breakable(BlockState s, net.minecraft.world.level.Level level, BlockPos pos) {
        if (s.isAir() || ballistic(s)) return false;
        float hard = s.getDestroySpeed(level, pos);
        if (hard < 0 || hard > 5) return false;
        Block b = s.getBlock();
        return (b instanceof DoorBlock d && d.type().canOpenByHand()) || (b instanceof TrapDoorBlock && hard < 4) || b instanceof FenceGateBlock
            || s.is(Tags.Blocks.GLASS) || s.is(Tags.Blocks.GLASS_PANES);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(Raids::killed);
        MinecraftForge.EVENT_BUS.addListener(Raids::playerTick);
        MinecraftForge.EVENT_BUS.addListener(Raids::raiderTick);
    }
}

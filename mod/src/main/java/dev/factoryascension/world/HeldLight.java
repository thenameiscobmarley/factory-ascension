package dev.factoryascension.world;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Light in your hands: holding a torch, lantern, glowstone, shroomlight, ... (either hand) lights the area around you,
 * as bright as that block would be when placed. An invisible light block (vanilla's) follows your head through the
 * air; it never replaces anything. Every one it places is written down (saved with the world) and taken away again,
 * even after a crash. Config: heldLights.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HeldLight {
    private static final Map<UUID, BlockPos> AT = new HashMap<>();

    private HeldLight() {
    }

    /** How bright what you hold is (0 = not a light). */
    static int brightness(ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof BlockItem bi)) return 0;
        try {
            return bi.getBlock().defaultBlockState().getLightEmission();
        } catch (Throwable e) {
            return 0;
        }
    }

    /** The world's record of light blocks we put down. */
    static final class Placed extends SavedData {
        final Set<Long> pos = new HashSet<>();

        static Placed of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(t -> {
                Placed p = new Placed();
                for (long l : t.getLongArray("Pos")) p.pos.add(l);
                return p;
            }, Placed::new, "fa_held_lights");
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.put("Pos", new LongArrayTag(pos.stream().mapToLong(Long::longValue).toArray()));
            return t;
        }
    }

    static boolean ours(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).is(Blocks.LIGHT);
    }

    static void clear(ServerLevel level, BlockPos p) {
        if (p == null) return;
        if (level.isLoaded(p) && ours(level, p)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        Placed d = Placed.of(level);
        if (d.pos.remove(p.asLong())) d.setDirty();
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 2 != 0) return;
        ServerLevel level = p.serverLevel();
        BlockPos old = AT.get(p.getUUID());
        int lvl = !FAConfig.heldLights || p.isSpectator() ? 0 : Math.max(brightness(p.getMainHandItem()), brightness(p.getOffhandItem()));
        if (lvl <= 0) {
            if (old != null) {
                clear(level, old);
                AT.remove(p.getUUID());
            }
            return;
        }
        lvl = Math.min(15, Math.max(1, lvl - FAConfig.heldLightDim));
        BlockPos want = null;
        for (BlockPos c : new BlockPos[] {BlockPos.containing(p.getEyePosition()), p.blockPosition(), BlockPos.containing(p.getEyePosition()).above()}) {
            BlockState st = level.getBlockState(c);
            if (st.isAir() || (st.is(Blocks.LIGHT) && c.equals(old))) { want = c; break; }
        }
        if (want == null) return;   // in water, in a wall: keep what's there
        if (want.equals(old) && level.getBlockState(want).is(Blocks.LIGHT) && level.getBlockState(want).getValue(LightBlock.LEVEL) == lvl) return;
        if (old != null && !old.equals(want)) clear(level, old);
        level.setBlock(want, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, lvl), 3);
        Placed d = Placed.of(level);
        if (d.pos.add(want.asLong())) d.setDirty();
        AT.put(p.getUUID(), want);
    }

    static void drop(ServerPlayer p) {
        BlockPos old = AT.remove(p.getUUID());
        if (old != null) clear(p.serverLevel(), old);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) drop(p);
    }

    @SubscribeEvent
    public static void changeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        BlockPos old = AT.remove(p.getUUID());
        var from = p.server.getLevel(event.getFrom());
        if (old != null && from != null) clear(from, old);
    }

    @SubscribeEvent
    public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) drop(p);
    }

    /** A world loading: any light we left (a crash, a kill) is taken away. */
    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        try {
            Placed d = Placed.of(level);
            if (d.pos.isEmpty()) return;
            level.getServer().execute(() -> {
                for (long l : Set.copyOf(d.pos)) {
                    BlockPos p = BlockPos.of(l);
                    if (level.isLoaded(p)) {
                        if (ours(level, p)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        d.pos.remove(l);
                    }
                }
                d.setDirty();
            });
        } catch (Throwable e) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Held lights: cleanup failed", e);
        }
    }

    /** Chunks loading later: their leftover lights go too. */
    @SubscribeEvent
    public static void chunk(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Placed d = Placed.of(level);
        if (d.pos.isEmpty()) return;
        var cp = event.getChunk().getPos();
        level.getServer().execute(() -> {
            boolean changed = false;
            for (long l : Set.copyOf(d.pos)) {
                BlockPos p = BlockPos.of(l);
                if ((p.getX() >> 4) != cp.x || (p.getZ() >> 4) != cp.z) continue;
                if (AT.containsValue(p)) continue;
                if (ours(level, p)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                d.pos.remove(l);
                changed = true;
            }
            if (changed) d.setDirty();
        });
    }
}

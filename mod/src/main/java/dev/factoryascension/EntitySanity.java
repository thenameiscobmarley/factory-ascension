package dev.factoryascension;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Safety net: an entity whose position or motion has become NaN or infinite (or flung far outside the world) makes
 * every mob that looks for it try to load a chunk that can never load, which freezes the server for good. Before each
 * level ticks its entities, such an entity is removed (a player is put back at the world spawn instead).
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntitySanity {
    private static final Logger LOG = LoggerFactory.getLogger("Factory Ascension");
    private static final double LIMIT = 3.0E7;

    private EntitySanity() {
    }

    static boolean bad(double v) {
        return !Double.isFinite(v) || Math.abs(v) > LIMIT;
    }

    static boolean broken(Entity e) {
        Vec3 p = e.position(), m = e.getDeltaMovement();
        return bad(p.x) || bad(p.z) || !Double.isFinite(p.y) || p.y < -4096 || p.y > 8192
            || !Double.isFinite(m.x) || !Double.isFinite(m.y) || !Double.isFinite(m.z) || Math.abs(m.x) + Math.abs(m.y) + Math.abs(m.z) > 1000;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.level instanceof ServerLevel level)) return;
        List<Entity> fix = null;
        for (Entity e : level.getAllEntities()) {
            if (e != null && broken(e)) {
                if (fix == null) fix = new ArrayList<>();
                fix.add(e);
            }
        }
        if (fix == null) return;
        for (Entity e : fix) {
            LOG.warn("Removed {} with a broken position {} / motion {} (it would freeze the server)", e, e.position(), e.getDeltaMovement());
            if (e instanceof ServerPlayer p) {
                var spawn = level.getSharedSpawnPos();
                p.setDeltaMovement(Vec3.ZERO);
                p.teleportTo(level, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, 0, 0);
            } else {
                e.discard();
            }
        }
    }
}

package dev.factoryascension.npc;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/** Bringing the night hunter: once a minute at night, per player out in the danger zones, a chance one comes (one at a time). */
public final class NightHunters {
    private NightHunters() {
    }

    public static final RegistryObject<EntityType<NightHunter>> TYPE = Operators.ENTITIES.register("night_hunter",
        () -> EntityType.Builder.<NightHunter>of(NightHunter::new, MobCategory.MONSTER).sized(0.6f, 2.5f).clientTrackingRange(10).build(FactoryAscension.MODID + ":night_hunter"));
    public static final RegistryObject<net.minecraft.world.item.Item> EGG = FactoryAscension.ITEMS.register("night_hunter_spawn_egg",
        () -> new net.minecraftforge.common.ForgeSpawnEggItem(TYPE, 0x1A1A1E, 0xD8D8D0, new net.minecraft.world.item.Item.Properties()));

    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TYPE.get(), NightHunter.attributes().build());
    }

    static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 1200 != 0 || !FAConfig.nightHunter) return;
        ServerLevel level = p.serverLevel();
        if (!level.isNight() || !level.dimensionType().hasSkyLight() || p.isCreative() || p.isSpectator()) return;
        if (dev.factoryascension.gun.Stress.danger(p) < FAConfig.nightHunterMinDanger) return;
        if (level.getBrightness(LightLayer.BLOCK, p.blockPosition()) >= 9) return;
        if (!level.getEntitiesOfClass(NightHunter.class, p.getBoundingBox().inflate(128)).isEmpty()) return;
        if (p.getRandom().nextFloat() >= FAConfig.nightHunterChance) return;
        // Behind you, out in the dark
        for (int tries = 0; tries < 12; tries++) {
            double a = Math.toRadians(p.getYRot() + 180 + (p.getRandom().nextFloat() - 0.5f) * 120);
            double dist = 32 + p.getRandom().nextDouble() * 16;
            int x = (int) (p.getX() - Math.sin(a) * dist), z = (int) (p.getZ() + Math.cos(a) * dist);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (level.getBrightness(LightLayer.BLOCK, at) >= 6) continue;
            NightHunter h = TYPE.get().create(level);
            if (h == null) return;
            h.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(h);
            return;
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NightHunters::attributes);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(NightHunters::tick);
    }
}

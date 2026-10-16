package dev.factoryascension.client.gun;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** A gun with a laser attachment puts a red dot wherever it points (up to 48 blocks). */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LaserDot {
    static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1f, 0.08f, 0.05f), 0.6f);

    private LaserDot() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) return;
        GunStats st = GunStats.of(mc.player.getMainHandItem());
        if (st == null || !st.laser) return;
        HitResult hit = mc.player.pick(48, 1, false);
        if (hit.getType() == HitResult.Type.MISS) return;
        Vec3 at = hit.getLocation().add(mc.player.getViewVector(1).scale(-0.05));
        mc.level.addParticle(RED, at.x, at.y, at.z, 0, 0, 0);
    }
}

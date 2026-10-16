package dev.factoryascension.client.gun;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunStats;
import dev.factoryascension.gun.Lean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Leaning (Q left, E right, while aiming a gun): the view tilts and slides around the corner, and your shots leave from
 * there (the fire packet carries it). While you aim, Q and E lean instead of dropping and opening the inventory.
 * And the hip-fire crosshair: where the gun actually points (it sways, kicks and fidgets), with a ring for the spread.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LeanClient {
    /** -1 (left) .. 1 (right), eased. */
    public static float lean = 0;
    private static float target = 0, last = 0;
    private static final java.lang.reflect.Method SET_POSITION = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
        net.minecraft.client.Camera.class, "m_90581_", Vec3.class);

    private LeanClient() {
    }

    static boolean aimingGun(net.minecraft.client.player.LocalPlayer p) {
        return p != null && GunItem.spec(p.getMainHandItem()) != null && p.isUsingItem() && p.getUseItem() == p.getMainHandItem();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.screen != null) { target = 0; return; }
        if (aimingGun(p)) {
            // Q and E are ours while aiming: no dropping the gun, no inventory
            while (mc.options.keyDrop.consumeClick()) { }
            while (mc.options.keyInventory.consumeClick()) { }
            target = (mc.options.keyInventory.isDown() ? 1 : 0) - (mc.options.keyDrop.isDown() ? 1 : 0);
        } else {
            target = 0;
        }
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        float now = (net.minecraft.Util.getMillis() % 1_000_000L) / 1000f;
        float dt = Mth.clamp(now - last, 0, 0.1f);
        last = now;
        lean += (target - lean) * Math.min(1, dt * 9);
        if (Math.abs(lean) < 0.002f) { lean = 0; return; }
        if (!mc.options.getCameraType().isFirstPerson() || mc.player == null) return;
        event.setRoll(event.getRoll() + lean * 14);
        var cam = event.getCamera();
        Vec3 pos = cam.getPosition();
        double yaw = Math.toRadians(cam.getYRot());
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 to = pos.add(right.scale(lean * Lean.DISTANCE)).add(0, -Math.abs(lean) * 0.08, 0);
        var hit = mc.level.clip(new ClipContext(pos, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.MISS) to = pos.add(hit.getLocation().subtract(pos).scale(0.75));
        try {
            SET_POSITION.invoke(cam, to);
        } catch (ReflectiveOperationException ignored) {
            // Without it the view only tilts
        }
    }

    /** The hip-fire crosshair. */
    @SubscribeEvent
    public static void crosshair(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || !mc.options.getCameraType().isFirstPerson() || mc.options.hideGui) return;
        var stack = p.getMainHandItem();
        if (GunItem.spec(stack) == null || aimingGun(p)) return;
        GunStats st = GunStats.of(stack);
        if (st == null) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        double fov = mc.options.fov().get() * p.getFieldOfViewModifier();
        double focal = (h / 2.0) / Math.tan(Math.toRadians(fov / 2));
        float cx = (float) (w / 2.0 + Math.tan(Math.toRadians(GunAnim.aimYaw)) * focal);
        float cy = (float) (h / 2.0 + Math.tan(Math.toRadians(GunAnim.aimPitch)) * focal);
        float spread = (float) (Math.tan(Math.toRadians(st.spreadHip)) * focal);
        int col = 0xC8E8E8E8;
        RenderSystem.enableBlend();
        // A dot where it points, four ticks out at the spread
        g.fill(Math.round(cx) - 1, Math.round(cy) - 1, Math.round(cx) + 1, Math.round(cy) + 1, col);
        int r = Math.max(3, Math.round(spread)), len = 3;
        g.fill(Math.round(cx) - r - len, Math.round(cy), Math.round(cx) - r, Math.round(cy) + 1, col);
        g.fill(Math.round(cx) + r, Math.round(cy), Math.round(cx) + r + len, Math.round(cy) + 1, col);
        g.fill(Math.round(cx), Math.round(cy) - r - len, Math.round(cx) + 1, Math.round(cy) - r, col);
        g.fill(Math.round(cx), Math.round(cy) + r, Math.round(cx) + 1, Math.round(cy) + r + len, col);
    }
}

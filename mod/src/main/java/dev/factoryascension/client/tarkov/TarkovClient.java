package dev.factoryascension.client.tarkov;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import dev.factoryascension.tarkov.TarkovLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of the Tarkov inventory: keeps the grid the server sends, opens the Tarkov screen instead of the game's
 * inventory (not in creative), and does the opening: you look down, a duffel bag comes up in your hands, its zip opens
 * and the inventory grows out of it. Any failure here falls back to the game's own inventory screen.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TarkovClient {
    public static final TarkovLayout LAYOUT = new TarkovLayout();
    public static boolean enabled = false;            // only once the server says so (a server without the mod: never)
    // The opening and closing, in seconds
    static final float LOOK = 0.30f, ZIP_AT = 0.28f, ZIP = 0.18f, GROW_AT = 0.38f, GROW = 0.32f, CLOSE = 0.25f;
    static final float TILT = 32;                     // degrees you look down at the bag
    static long openedAt = 0, closedAt = 0;
    /** Dev screenshots: a fixed point in the opening, in seconds (null = real time). */
    public static Float debugTime = null;

    private TarkovClient() {
    }

    public static void sync(boolean on, int width, int height, int[] pos) {
        enabled = on;
        FAConfig.tarkovWidth = width;
        FAConfig.tarkovHeight = height;
        LAYOUT.copyFrom(pos);
    }

    static boolean animated() {
        return FAClientConfig.TARKOV_ANIMATION.get();
    }

    /** Seconds since the inventory opened. */
    static float sinceOpen() {
        if (debugTime != null) return debugTime;
        return (Util.getMillis() - openedAt) / 1000f;
    }

    static float ease(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** How far the bag is up and the view tilted down: 0..1 (opening while open, easing back after closing). */
    static float raised() {
        if (!animated()) return 0;
        Minecraft mc = Minecraft.getInstance();
        if (ours(mc.screen)) return ease(sinceOpen() / LOOK);
        float since = (Util.getMillis() - closedAt) / 1000f;
        return since < CLOSE ? 1 - ease(since / CLOSE) : 0;
    }

    /** 0 closed .. 1 open (the zip). */
    static float zip() {
        Minecraft mc = Minecraft.getInstance();
        return ours(mc.screen) ? ease((sinceOpen() - ZIP_AT) / ZIP) : 0;
    }

    /** 0..1: how grown the inventory is. */
    static float grown() {
        return animated() ? ease((sinceOpen() - GROW_AT) / GROW) : 1;
    }

    /** One of our inventory screens (the dark one, or the grid one if the config turns the grid on). */
    static boolean ours(net.minecraft.client.gui.screens.Screen s) {
        return s instanceof TarkovScreen || s instanceof DarkInventoryScreen;
    }

    static void opened() {
        openedAt = Util.getMillis();
    }

    /** Re-opened after the window changed size: straight to fully open, no animation. */
    static void openedAlready() {
        openedAt = Util.getMillis() - 10_000;
    }

    static void closed() {
        closedAt = Util.getMillis();
    }

    // ---- The game's inventory screen becomes the Tarkov one ----
    @SubscribeEvent
    public static void open(ScreenEvent.Opening event) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || event.getNewScreen() == null || event.getNewScreen().getClass() != InventoryScreen.class) return;
        try {
            event.setNewScreen(FAConfig.tarkovGrid ? new TarkovScreen(mc.player) : new DarkInventoryScreen(mc.player));
        } catch (Throwable err) {
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Tarkov inventory failed to open; using the normal one", err);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        enabled = false;
    }

    // ---- Looking down at the bag ----
    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        float r = raised();
        if (r <= 0 || !Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        event.setPitch(Math.min(90, event.getPitch() + TILT * r));
    }

    // ---- The bag in your hands ----
    @SubscribeEvent
    public static void hands(RenderHandEvent event) {
        float r = raised();
        if (r <= 0) return;
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        try {
            drawBag(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), r, zip());
        } catch (Throwable err) {
            FAClientConfig.TARKOV_ANIMATION.set(false);
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").error("Duffel bag drawing failed; the opening animation is off now", err);
        }
    }

    static void drawBag(PoseStack pose, MultiBufferSource buffers, int light, float raise, float zip) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) return;
        // Comes up from below the view, held in front of you, top tipped toward you
        pose.pushPose();
        pose.translate(0, -0.95 + 0.72 * raise, -0.72);
        pose.mulPose(Axis.XP.rotationDegrees(40));
        ItemStack bag = new ItemStack(FactoryAscension.DUFFEL_BAG.get());
        if (zip > 0.5f) bag.getOrCreateTag().putInt("CustomModelData", 1);   // the open-zip model
        pose.pushPose();
        pose.scale(0.85f, 0.85f, 0.85f);
        mc.getItemRenderer().renderStatic(bag, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
        pose.popPose();
        pose.popPose();
        // Both arms in the game's own first-person arm pose, brought in from the sides to the bag's ends
        if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer arms) {
            for (int side = -1; side <= 1; side += 2) {
                float f = side;
                pose.pushPose();
                pose.translate(f * 0.46f, -0.86f + 0.42f * raise, -0.72f);
                pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
                pose.translate(f * -1.0F, 3.6F, 3.5F);
                pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
                pose.mulPose(Axis.XP.rotationDegrees(200.0F));
                pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
                pose.translate(f * 5.6F, 0.0F, 0.0F);
                if (side > 0) arms.renderRightHand(pose, buffers, light, player);
                else arms.renderLeftHand(pose, buffers, light, player);
                pose.popPose();
            }
        }
    }

    // ---- Size and weight in the tooltip, while the inventory is open ----
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        if (!enabled || !FAConfig.tarkovGrid || !(Minecraft.getInstance().screen instanceof TarkovScreen) || event.getItemStack().isEmpty()) return;
        int[] s = TarkovLayout.size(event.getItemStack());
        event.getToolTip().add(Component.literal(s[0] + "x" + s[1] + " · " + String.format("%.1f kg", TarkovLayout.weight(event.getItemStack())))
            .withStyle(ChatFormatting.DARK_AQUA));
    }
}

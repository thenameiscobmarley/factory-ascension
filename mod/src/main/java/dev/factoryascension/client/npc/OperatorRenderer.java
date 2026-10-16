package dev.factoryascension.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.gun.ArmPoses;
import dev.factoryascension.client.gun.GunRenderer;
import dev.factoryascension.npc.Operator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * An operator looks like a player: the player model (with its jacket, sleeves and trouser layers, which their skins use
 * for helmets, plate carriers and pouches), armor over it. Calm, its gun hangs on its back on the sling; on alert it's up
 * in its hands (shouldered when it aims, low ready otherwise), and reloads, fumbles and patching up all show in its arms.
 * Over its head, while it's unsure, a "?" fills from white to orange; when it raises the alarm, a red "!". Up close its
 * gun is drawn smooth, like yours.
 */
public class OperatorRenderer extends HumanoidMobRenderer<Operator, PlayerModel<Operator>> {
    private static final Map<Operator, Integer> ALARM_AT = new WeakHashMap<>();

    public OperatorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        // The gun: in its hands on alert, on its back otherwise
        layers.removeIf(l -> l instanceof ItemInHandLayer);
        addLayer(new GunLayer(this, ctx));
    }

    @Override
    public ResourceLocation getTextureLocation(Operator e) {
        return new ResourceLocation(FactoryAscension.MODID, "textures/entity/operator/" + e.type.id + ".png");
    }

    @Override
    public void render(Operator e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        PoseStack.Pose fa$top = pose.last();
        try {
            PlayerModel<Operator> m = getModel();
            boolean armed = !e.getMainHandItem().isEmpty() && e.alertLevel() == 2;
            HumanoidModel.ArmPose p = armed ? ArmPoses.forOperator(e) : HumanoidModel.ArmPose.EMPTY;
            if (e.action() == 3) p = ArmPoses.MEDIC;
            m.rightArmPose = p;
            m.leftArmPose = p == HumanoidModel.ArmPose.EMPTY ? HumanoidModel.ArmPose.EMPTY : p;
            m.crouching = e.isCrouching();
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            boolean near = e.position().distanceToSqr(cam) < 100;
            GunRenderer.closeUp = near;
            try {
                super.render(e, yaw, partial, pose, buffers, light);
            } finally {
                GunRenderer.closeUp = false;
            }
            indicator(e, pose, buffers, light);
        } catch (Throwable err) {
            while (pose.last() != fa$top && !pose.clear()) pose.popPose();   // back to where it started
            dev.factoryascension.client.FAClientConfig.renderFailed("Operator drawing", err);
        }
    }

    /** "?" (unsure, filling up) or "!" (just raised the alarm) over its head. */
    private void indicator(Operator e, PoseStack pose, MultiBufferSource buffers, int light) {
        String text;
        int colour;
        float s = e.suspicion();
        if (e.alertLevel() == 2) {
            Integer at = ALARM_AT.computeIfAbsent(e, k -> e.tickCount);
            if (e.tickCount - at > 60) return;
            text = "!";
            colour = 0xFFE83020;
        } else {
            ALARM_AT.remove(e);
            if (s < 0.04f) return;
            text = "?";
            int r = 255, g = Math.round(255 - 140 * s), b = Math.round(255 - 235 * s);
            colour = (Math.round(120 + 135 * Math.min(1, s * 2)) << 24) | r << 16 | g << 8 | b;
        }
        pose.pushPose();
        pose.translate(0, e.getBbHeight() + 0.75, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float sc = 0.045f;
        pose.scale(-sc, -sc, sc);
        Font font = getFont();
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, colour, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        pose.popPose();
    }

    /** Its gun: in hand when it's on alert (the vanilla hand layer), slung across its back when it's calm. */
    static final class GunLayer extends ItemInHandLayer<Operator, PlayerModel<Operator>> {
        GunLayer(OperatorRenderer parent, EntityRendererProvider.Context ctx) {
            super(parent, ctx.getItemInHandRenderer());
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Operator e, float limb, float limbAmount, float partial, float age, float yaw, float pitch) {
            if (e.alertLevel() == 2) {
                super.render(pose, buffers, light, e, limb, limbAmount, partial, age, yaw, pitch);
                return;
            }
            ItemStack gun = e.getMainHandItem();
            if (gun.isEmpty()) return;
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            pose.translate(0, 0.38, 0.2);                      // against the back
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-40));         // across it, muzzle up over a shoulder
            pose.scale(0.85f, 0.85f, 0.85f);
            Minecraft.getInstance().getItemRenderer().renderStatic(e, gun, ItemDisplayContext.FIXED, false, pose, buffers, e.level(), light, OverlayTexture.NO_OVERLAY, e.getId());
            pose.popPose();
        }
    }
}

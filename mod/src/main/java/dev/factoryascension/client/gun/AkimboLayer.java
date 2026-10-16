package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Akimbo seen from outside: the second gun in the player's other hand (the off hand must be empty). */
public class AkimboLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public AkimboLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limb, float limbAmount, float partial, float age, float yaw, float pitch) {
        ItemStack main = p.getMainHandItem();
        if (GunItem.spec(main) == null || !p.getOffhandItem().isEmpty()) return;
        GunStats st = GunStats.of(main);
        if (st == null || !st.akimbo) return;
        HumanoidArm other = p.getMainArm().getOpposite();
        pose.pushPose();
        getParentModel().translateToHand(other, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        boolean left = other == HumanoidArm.LEFT;
        pose.translate((left ? -1 : 1) / 16f, 0.125f, -0.625f);
        Minecraft.getInstance().getItemRenderer().renderStatic(p, main, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            left, pose, buffers, p.level(), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, p.getId());
        pose.popPose();
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Add {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new AkimboLayer(r));
            }
        }
    }
}

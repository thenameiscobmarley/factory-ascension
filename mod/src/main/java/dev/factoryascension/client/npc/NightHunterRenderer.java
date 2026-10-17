package dev.factoryascension.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.npc.NightHunter;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** The night hunter: a humanoid stretched tall and thin, long arms hanging, and eyes that glow in the dark. */
public class NightHunterRenderer extends HumanoidMobRenderer<NightHunter, HumanoidModel<NightHunter>> {
    static final ResourceLocation SKIN = new ResourceLocation(FactoryAscension.MODID, "textures/entity/night_hunter.png");
    static final RenderType EYES = RenderType.eyes(new ResourceLocation(FactoryAscension.MODID, "textures/entity/night_hunter_eyes.png"));

    public NightHunterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(NightHunter e, PoseStack pose, float partial) {
        pose.scale(0.82f, 1.4f, 0.82f);
    }

    @Override
    public ResourceLocation getTextureLocation(NightHunter e) {
        return SKIN;
    }
}

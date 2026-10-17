package dev.factoryascension.client.npc;

import dev.factoryascension.FactoryAscension;
import dev.factoryascension.raid.Trader;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** A trader: a person in their own clothes (textures/entity/trader_<kind>.png, from tools/v25_art.py). */
public class TraderRenderer extends HumanoidMobRenderer<Trader, HumanoidModel<Trader>> {
    public TraderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(Trader t) {
        return new ResourceLocation(FactoryAscension.MODID, "textures/entity/trader_" + t.kind().name().toLowerCase() + ".png");
    }
}

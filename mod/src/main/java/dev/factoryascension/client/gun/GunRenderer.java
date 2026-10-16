package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.Attachments;
import dev.factoryascension.gun.GunGeometry;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSpec;
import dev.factoryascension.gun.GunStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a gun: its own model (models/item/gunbase/<id>), then every attachment on it at that slot's mount point
 * (models/item/attach/<shape>, in the attachment's finish colour). Several of the same slot (a Jury-Rigged gun) are
 * spread out so you can see them all.
 */
public final class GunRenderer extends BlockEntityWithoutLevelRenderer {
    public static final GunRenderer INSTANCE = new GunRenderer();
    private static final RandomSource RANDOM = RandomSource.create();

    private GunRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static ResourceLocation base(String id) {
        return new ResourceLocation(FactoryAscension.MODID, "item/gunbase/" + id);
    }

    static ResourceLocation shape(String id) {
        return new ResourceLocation(FactoryAscension.MODID, "item/attach/" + id);
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Models {
        @SubscribeEvent
        public static void register(ModelEvent.RegisterAdditional event) {
            for (GunSpec g : dev.factoryascension.gun.Guns.ALL) event.register(base(g.id));
            for (String s : GunGeometry.SHAPES) event.register(shape(s));
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        GunSpec spec = GunItem.spec(stack);
        if (spec == null) return;
        var models = Minecraft.getInstance().getModelManager();
        VertexConsumer vc = buffers.getBuffer(Sheets.cutoutBlockSheet());
        draw(pose, vc, models.getModel(base(spec.id)), -1, light, overlay);
        GunStats stats = GunStats.of(stack);
        if (stats == null || stats.atts.isEmpty()) return;
        Map<Attachments.Slot, Integer> count = new HashMap<>();
        for (Attachments.Att a : stats.atts) {
            if (a.shape.equals("none")) continue;
            int n = count.merge(a.slot, 1, Integer::sum) - 1;
            float[] m = spec.mount(a.slot);
            pose.pushPose();
            // A second (third...) of the same slot sits a little further along / out
            float off = n * 2.2f;
            float dx = 0, dy = 0, dz = 0;
            switch (a.slot) {
                case MUZZLE, BARREL -> dx = off * 2.0f;
                case OPTIC -> dx = -off * 2.4f;
                case STOCK -> dx = -off;
                case TACTICAL, CHARM -> dz = off * 0.6f;
                default -> dx = off * 1.2f;
            }
            pose.translate((m[0] - 8 + dx) / 16f, (m[1] - 8 + dy) / 16f, (m[2] - 8 + dz) / 16f);
            draw(pose, vc, models.getModel(shape(a.shape)), a.colour, light, overlay);
            pose.popPose();
        }
    }

    private static void draw(PoseStack pose, VertexConsumer vc, BakedModel model, int colour, int light, int overlay) {
        float r = 1, g = 1, b = 1;
        if (colour >= 0) {
            r = (colour >> 16 & 255) / 255f;
            g = (colour >> 8 & 255) / 255f;
            b = (colour & 255) / 255f;
        }
        for (Direction d : Direction.values()) quads(pose, vc, model.getQuads(null, d, RANDOM), r, g, b, light, overlay);
        quads(pose, vc, model.getQuads(null, null, RANDOM), r, g, b, light, overlay);
    }

    private static void quads(PoseStack pose, VertexConsumer vc, List<BakedQuad> quads, float r, float g, float b, int light, int overlay) {
        for (BakedQuad q : quads) {
            boolean tint = q.isTinted();
            vc.putBulkData(pose.last(), q, tint ? r : 1, tint ? g : 1, tint ? b : 1, light, overlay);
        }
    }
}

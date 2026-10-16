package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.gun.Attachments;
import dev.factoryascension.gun.GunGeometry;
import dev.factoryascension.gun.GunItem;
import dev.factoryascension.gun.GunSpec;
import dev.factoryascension.gun.GunStats;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a gun: up close (first person, your own gun in third person, the bench's preview) as its smooth mesh
 * (client/gun/GunMesh, gunmesh/<id>.fam) with its skin; otherwise (icons, the ground, other people's guns far away)
 * as its cuboid model (models/item/gunbase/<id>). Then every attachment on it at that slot's mount point, in the
 * attachment's finish colour. Several of the same slot (a Jury-Rigged gun) are spread out so you can see them all.
 * A Gangster grip turns the gun on its side in third person (GunAnim does it in first person; AkimboHands draws the
 * second gun of an Akimbo kit).
 */
public final class GunRenderer extends BlockEntityWithoutLevelRenderer {
    public static final GunRenderer INSTANCE = new GunRenderer();
    private static final RandomSource RANDOM = RandomSource.create();
    /** Set while your own player is drawn (your gun in third person is up close) and by screens with a big preview. */
    public static boolean closeUp = false, preview = false;

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

        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) rm -> {
                GunMesh.clear();
                dev.factoryascension.client.render.RealismCamera.free();   // its shaders come from the resources too
            });
        }
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Players {
        @SubscribeEvent
        public static void pre(RenderPlayerEvent.Pre event) {
            closeUp = event.getEntity() == Minecraft.getInstance().player;
        }

        @SubscribeEvent
        public static void post(RenderPlayerEvent.Post event) {
            closeUp = false;
        }
    }

    static boolean detailed(ItemDisplayContext ctx) {
        if (!dev.factoryascension.client.FAClientConfig.gunMeshes()) return false;
        return switch (ctx) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> true;
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> closeUp;
            case GUI, FIXED -> preview;
            default -> false;
        };
    }

    public static float time() {
        return (Util.getMillis() % 3_600_000L) / 1000f;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        GunSpec spec = GunItem.spec(stack);
        if (spec == null) return;
        GunStats stats = GunStats.of(stack);
        boolean firstPerson = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean held = firstPerson || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        pose.pushPose();
        if (held && !firstPerson && stats != null && stats.gangster) {
            // Gangster grip: rolled onto its side about the hand
            pose.translate(5 / 16f, 6 / 16f, 8 / 16f);
            pose.mulPose(Axis.XP.rotationDegrees(-78));
            pose.translate(-5 / 16f, -6 / 16f, -8 / 16f);
        }
        drawGun(stack, spec, stats, ctx, pose, buffers, light, overlay);
        pose.popPose();
    }

    private void drawGun(ItemStack stack, GunSpec spec, GunStats stats, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var models = Minecraft.getInstance().getModelManager();
        boolean detail = detailed(ctx);
        float time = time();
        GunMesh mesh = detail ? GunMesh.get(spec.id) : null;
        GunSkins.Skin skin = GunSkins.of(stack);
        VertexConsumer vc = null;
        if (mesh != null) {
            mesh.draw(pose, buffers, light, overlay, -1, skin != null ? skin.shader : null, time);
            if (skin != null) GunSkins.extras(skin, spec.id, mesh, pose, buffers, light, overlay, time);
        } else {
            vc = buffers.getBuffer(Sheets.cutoutBlockSheet());
            draw(pose, vc, models.getModel(base(spec.id)), skin != null ? skin.iconTint : -1, light, overlay, skin != null);
        }
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
            GunMesh am = detail ? GunMesh.get("attach/" + a.shape) : null;
            if (am != null) {
                am.draw(pose, buffers, light, overlay, a.colour, skin != null && skin.coversAttachments ? skin.shader : null, time);
            } else {
                if (vc == null) vc = buffers.getBuffer(Sheets.cutoutBlockSheet());
                draw(pose, vc, models.getModel(shape(a.shape)), a.colour, light, overlay, false);
            }
            pose.popPose();
        }
    }

    private static void draw(PoseStack pose, VertexConsumer vc, BakedModel model, int colour, int light, int overlay, boolean tintAll) {
        float r = 1, g = 1, b = 1;
        if (colour >= 0) {
            r = (colour >> 16 & 255) / 255f;
            g = (colour >> 8 & 255) / 255f;
            b = (colour & 255) / 255f;
        }
        for (Direction d : Direction.values()) quads(pose, vc, model.getQuads(null, d, RANDOM), r, g, b, light, overlay, tintAll);
        quads(pose, vc, model.getQuads(null, null, RANDOM), r, g, b, light, overlay, tintAll);
    }

    private static void quads(PoseStack pose, VertexConsumer vc, List<BakedQuad> quads, float r, float g, float b, int light, int overlay, boolean tintAll) {
        for (BakedQuad q : quads) {
            boolean tint = tintAll || q.isTinted();
            vc.putBulkData(pose.last(), q, tint ? r : 1, tint ? g : 1, tint ? b : 1, light, overlay);
        }
    }
}

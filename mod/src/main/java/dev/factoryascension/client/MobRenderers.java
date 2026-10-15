package dev.factoryascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.mob.FAMobs;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Each mob uses its vanilla base's renderer and model with its own skin (textures/entity/<id>.png); bosses are scaled up. */
final class MobRenderers {
    private MobRenderers() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        for (FAMobs.Entry m : FAMobs.ALL) {
            ResourceLocation tex = new ResourceLocation(FactoryAscension.MODID, "textures/entity/" + m.id() + ".png");
            float sc = m.scale();   // bosses are drawn bigger
            EntityRendererProvider provider = switch (m.base()) {
                case "zombie" -> ctx -> new ZombieRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Zombie e) { return tex; }
                    @Override protected void scale(Zombie e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "skeleton" -> ctx -> new SkeletonRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return tex; }
                    @Override protected void scale(AbstractSkeleton e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "spider" -> ctx -> new SpiderRenderer<Spider>(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Spider e) { return tex; }
                    @Override protected void scale(Spider e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "creeper" -> ctx -> new CreeperRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Creeper e) { return tex; }
                    @Override protected void scale(Creeper e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "slime" -> ctx -> new SlimeRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Slime e) { return tex; }
                    @Override protected void scale(Slime e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "silverfish" -> ctx -> new SilverfishRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Silverfish e) { return tex; }
                    @Override protected void scale(Silverfish e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "blaze" -> ctx -> new BlazeRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Blaze e) { return tex; }
                    @Override protected void scale(Blaze e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "enderman" -> ctx -> new EndermanRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(EnderMan e) { return tex; }
                    @Override protected void scale(EnderMan e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "vindicator" -> ctx -> new VindicatorRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Vindicator e) { return tex; }
                    @Override protected void scale(Vindicator e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                case "chicken" -> ctx -> new ChickenRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Chicken e) { return tex; }
                    @Override protected void scale(Chicken e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
                default -> ctx -> new CowRenderer(ctx) { { shadowRadius *= sc; } @Override public ResourceLocation getTextureLocation(Cow e) { return tex; }
                    @Override protected void scale(Cow e, PoseStack ps, float pt) { super.scale(e, ps, pt); if (sc != 1f) ps.scale(sc, sc, sc); } };
            };
            event.registerEntityRenderer((EntityType) m.type().get(), provider);
        }
    }
}

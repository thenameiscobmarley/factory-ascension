package dev.factoryascension.client;

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

/** Each mob uses its vanilla base's renderer and model with its own skin (textures/entity/<id>.png). */
final class MobRenderers {
    private MobRenderers() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void register(EntityRenderersEvent.RegisterRenderers event) {
        for (FAMobs.Entry m : FAMobs.ALL) {
            ResourceLocation tex = new ResourceLocation(FactoryAscension.MODID, "textures/entity/" + m.id() + ".png");
            EntityRendererProvider provider = switch (m.base()) {
                case "zombie" -> ctx -> new ZombieRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Zombie e) { return tex; } };
                case "skeleton" -> ctx -> new SkeletonRenderer(ctx) { @Override public ResourceLocation getTextureLocation(AbstractSkeleton e) { return tex; } };
                case "spider" -> ctx -> new SpiderRenderer<Spider>(ctx) { @Override public ResourceLocation getTextureLocation(Spider e) { return tex; } };
                case "creeper" -> ctx -> new CreeperRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Creeper e) { return tex; } };
                case "slime" -> ctx -> new SlimeRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Slime e) { return tex; } };
                case "silverfish" -> ctx -> new SilverfishRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Silverfish e) { return tex; } };
                case "blaze" -> ctx -> new BlazeRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Blaze e) { return tex; } };
                case "enderman" -> ctx -> new EndermanRenderer(ctx) { @Override public ResourceLocation getTextureLocation(EnderMan e) { return tex; } };
                case "vindicator" -> ctx -> new VindicatorRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Vindicator e) { return tex; } };
                case "chicken" -> ctx -> new ChickenRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Chicken e) { return tex; } };
                default -> ctx -> new CowRenderer(ctx) { @Override public ResourceLocation getTextureLocation(Cow e) { return tex; } };
            };
            event.registerEntityRenderer((EntityType) m.type().get(), provider);
        }
    }
}

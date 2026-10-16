package dev.factoryascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.drugs.Drugs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Things that aren't there (psychosis after meth, magic mushrooms; only you see them, they're never real entities):
 * <ul>
 * <li>A figure standing still far off, watching you. It's there until you get close, or stare right at it for a moment,
 * and then it's simply gone.</li>
 * <li>A head peeking round a corner. When you look towards it, it slides back behind the wall. Go and check: nothing.</li>
 * <li>Footsteps behind you, a whisper, a door, a breath at your ear.</li>
 * </ul>
 * Drawn in the world after the entities; only in first person.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Hallucinations {
    private static final RandomSource RNG = RandomSource.create();
    private static final ResourceLocation[] SKINS = {
        skin("ghost"), skin("cultist"), skin("deserter"), skin("hunter"), skin("scav")};
    @Nullable private static PlayerModel<LocalPlayer> model;

    /** One thing that isn't there. */
    static final class Ghost {
        final boolean peeker;
        Vec3 pos;            // feet (a figure) or the head's centre (a peeker)
        Vec3 hide;           // a peeker: the way back behind the wall
        float yaw;
        int age, life, stared;
        float out = 0;       // a peeker: 0 hidden .. 1 peeking
        boolean leaving;
        final ResourceLocation skin;

        Ghost(boolean peeker, Vec3 pos, float yaw, int life) {
            this.peeker = peeker;
            this.pos = pos;
            this.yaw = yaw;
            this.life = life;
            this.skin = SKINS[RNG.nextInt(SKINS.length)];
        }
    }

    private static final List<Ghost> GHOSTS = new ArrayList<>();
    private static int nextSound = 200, nextGhost = 200;

    private Hallucinations() {
    }

    static ResourceLocation skin(String id) {
        return new ResourceLocation(FactoryAscension.MODID, "textures/entity/operator/" + id + ".png");
    }

    /** How strongly you're seeing things: 0 (not at all) .. 1. */
    static float level(LocalPlayer p) {
        float k = 0;
        if (p.hasEffect(Drugs.PSYCHOSIS.get())) k = Math.max(k, 0.7f + 0.15f * p.getEffect(Drugs.PSYCHOSIS.get()).getAmplifier());
        if (p.hasEffect(Drugs.TRIPPING.get())) k = Math.max(k, 0.5f);
        if (p.hasEffect(Drugs.TWEAKING.get())) k = Math.max(k, 0.15f);
        return Math.min(1, k);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused()) {
            if (p == null) GHOSTS.clear();
            return;
        }
        float k = level(p);
        if (k <= 0) {
            GHOSTS.clear();
            return;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1);
        for (var it = GHOSTS.iterator(); it.hasNext(); ) {
            Ghost g = it.next();
            g.age++;
            Vec3 to = (g.peeker ? g.pos : g.pos.add(0, 1.6, 0)).subtract(eye);
            double dist = to.length();
            double dot = to.normalize().dot(look);
            if (g.peeker) {
                // Slides out, waits; when you look its way, slides back in (and is gone)
                if (dot > 0.93 || dist < 4) g.leaving = true;
                g.out = g.leaving ? Math.max(0, g.out - 0.16f) : Math.min(1, g.out + 0.04f);
                if ((g.leaving && g.out <= 0) || g.age > g.life) it.remove();
            } else {
                // Stands there until you get near, or stare at it
                g.yaw = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG) + 180;
                if (dot > 0.985) g.stared++;
                if (dist < 9 || g.stared > 30 || g.age > g.life || !visible(mc.level, eye, g.pos.add(0, 1.4, 0))) it.remove();
            }
        }
        if (--nextGhost <= 0) {
            nextGhost = Math.round((300 + RNG.nextInt(600)) / (0.4f + k));
            if (GHOSTS.size() < 2) {
                Ghost g = RNG.nextFloat() < 0.55f ? peeker(mc.level, p) : figure(mc.level, p);
                if (g != null) GHOSTS.add(g);
            }
        }
        if (--nextSound <= 0) {
            nextSound = Math.round((160 + RNG.nextInt(500)) / (0.4f + k));
            sound(mc.level, p);
        }
    }

    static boolean visible(Level level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null)).getType() == HitResult.Type.MISS;
    }

    /** A figure: 16-34 blocks off, on the ground, in the open, at the edge of your view (not in the middle). */
    @Nullable
    static Ghost figure(Level level, LocalPlayer p) {
        Vec3 eye = p.getEyePosition();
        for (int tries = 0; tries < 20; tries++) {
            float a = p.getYRot() + (RNG.nextBoolean() ? 1 : -1) * (25 + RNG.nextFloat() * 30);
            double d = 16 + RNG.nextDouble() * 18;
            double x = p.getX() - Mth.sin(a * Mth.DEG_TO_RAD) * d, z = p.getZ() + Mth.cos(a * Mth.DEG_TO_RAD) * d;
            BlockPos top = BlockPos.containing(x, p.getY() + 6, z);
            int y = -1;
            for (int dy = 0; dy < 14; dy++) {
                BlockPos b = top.below(dy);
                if (level.getBlockState(b).isAir() && level.getBlockState(b.above()).isAir() && level.getBlockState(b.below()).isFaceSturdy(level, b.below(), Direction.UP)) {
                    y = b.getY();
                    break;
                }
            }
            if (y < 0) continue;
            Vec3 feet = new Vec3(x, y, z);
            if (!visible(level, eye, feet.add(0, 1.5, 0))) continue;
            return new Ghost(false, feet, 0, 400 + RNG.nextInt(600));
        }
        return null;
    }

    /**
     * A peeker: a wall corner 6-16 blocks away that you can see past - a solid block with open air beside it (across your
     * view). The head sits just behind the wall's edge and comes out sideways into the air.
     */
    @Nullable
    static Ghost peeker(Level level, LocalPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 fwd = Vec3.directionFromRotation(0, p.getYRot());
        for (int tries = 0; tries < 60; tries++) {
            float a = p.getYRot() + (RNG.nextFloat() - 0.5f) * 140;
            double d = 6 + RNG.nextDouble() * 10;
            Vec3 at = new Vec3(p.getX() - Mth.sin(a * Mth.DEG_TO_RAD) * d, eye.y + (RNG.nextFloat() - 0.6f) * 2, p.getZ() + Mth.cos(a * Mth.DEG_TO_RAD) * d);
            BlockPos wall = BlockPos.containing(at);
            if (!level.getBlockState(wall).isSolidRender(level, wall)) continue;
            // The open side: across the view
            Direction side = Direction.fromYRot(a + (RNG.nextBoolean() ? 90 : -90));
            BlockPos open = wall.relative(side);
            if (!level.getBlockState(open).isAir() || !level.getBlockState(open.below()).isAir() && !level.getBlockState(open.above()).isAir()) continue;
            // Behind the wall (away from you) has to be open too, so a head could be there
            Direction away = Direction.fromYRot(a);
            BlockPos behind = wall.relative(away);
            BlockPos behindOpen = open.relative(away);
            if (!level.getBlockState(behind).isAir() && !level.getBlockState(behindOpen).isAir()) continue;
            if (level.getBlockState(behindOpen).isSolidRender(level, behindOpen)) continue;
            // The head: at the wall's edge, behind its front face
            Vec3 c = Vec3.atCenterOf(wall);
            Vec3 edge = c.add(side.getStepX() * 0.5, 0, side.getStepZ() * 0.5).add(away.getStepX() * 0.75, 0, away.getStepZ() * 0.75);
            Vec3 head = new Vec3(edge.x, wall.getY() + 0.5, edge.z);
            Vec3 peekOut = head.add(side.getStepX() * 0.22, 0, side.getStepZ() * 0.22);
            if (!visible(level, eye, peekOut.add(side.getStepX() * 0.15, 0, side.getStepZ() * 0.15))) continue;
            if (peekOut.subtract(eye).normalize().dot(p.getViewVector(1)) > 0.9) continue;   // not right where you're looking
            Vec3 to = eye.subtract(peekOut);
            float yaw = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
            Ghost g = new Ghost(true, peekOut, yaw, 300 + RNG.nextInt(400));
            g.hide = new Vec3(-side.getStepX() * 0.45, 0, -side.getStepZ() * 0.45);
            return g;
        }
        return null;
    }

    static void sound(Level level, LocalPlayer p) {
        SoundEvent s;
        float vol = 0.5f, pitch = 0.8f + RNG.nextFloat() * 0.3f;
        double d = 3 + RNG.nextDouble() * 6;
        switch (RNG.nextInt(7)) {
            case 0, 1 -> s = SoundEvents.GRAVEL_STEP;
            case 2 -> s = SoundEvents.WOOD_STEP;
            case 3 -> {
                s = SoundEvents.AMBIENT_CAVE.value();
                vol = 0.6f;
            }
            case 4 -> {
                s = SoundEvents.WOODEN_DOOR_OPEN;
                d = 10 + RNG.nextDouble() * 8;
            }
            case 5 -> {
                s = SoundEvents.PLAYER_BREATH;
                d = 1.2;
                vol = 0.35f;
            }
            default -> {
                s = SoundEvents.SOUL_ESCAPE;
                d = 1.5;
                vol = 0.9f;
                pitch = 0.5f + RNG.nextFloat() * 0.2f;
            }
        }
        // Behind you
        float a = p.getYRot() + 180 + (RNG.nextFloat() - 0.5f) * 100;
        double x = p.getX() - Mth.sin(a * Mth.DEG_TO_RAD) * d, z = p.getZ() + Mth.cos(a * Mth.DEG_TO_RAD) * d;
        level.playLocalSound(x, p.getY() + 0.5, z, s, SoundSource.HOSTILE, vol, pitch, false);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GHOSTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        try {
            if (model == null) model = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            PoseStack pose = event.getPoseStack();
            Vec3 cam = event.getCamera().getPosition();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            float partial = event.getPartialTick();
            for (Ghost g : GHOSTS) draw(mc, g, pose, cam, buffers, partial);
            buffers.endBatch();
        } catch (Throwable err) {
            GHOSTS.clear();
            org.slf4j.LoggerFactory.getLogger("Factory Ascension").warn("Hallucinations failed to draw", err);
        }
    }

    private static void draw(Minecraft mc, Ghost g, PoseStack pose, Vec3 cam, MultiBufferSource buffers, float partial) {
        PlayerModel<LocalPlayer> m = model;
        Vec3 at = g.pos;
        if (g.peeker) {
            float o = 1 - g.out;
            at = at.add(g.hide.scale(o * o));
        }
        int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at.add(0, g.peeker ? 0 : 1, 0)));
        // Dim and dark, like it's standing in its own shadow
        float shade = 0.28f;
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(g.skin));
        for (var part : new net.minecraft.client.model.geom.ModelPart[] {m.head, m.hat, m.body, m.jacket, m.rightArm, m.leftArm, m.rightSleeve, m.leftSleeve,
            m.rightLeg, m.leftLeg, m.rightPants, m.leftPants}) part.setRotation(0, 0, 0);
        if (g.peeker) {
            // Just the head, tilted round the corner
            pose.mulPose(Axis.YP.rotationDegrees(180 - g.yaw));
            pose.mulPose(Axis.ZP.rotationDegrees(14));
            pose.scale(-0.9375f, -0.9375f, 0.9375f);
            pose.translate(0, 0.25, 0);
            m.head.setPos(0, 0, 0);
            m.hat.setPos(0, 0, 0);
            m.head.render(pose, vc, light, OverlayTexture.NO_OVERLAY, shade, shade, shade, 1);
            m.hat.render(pose, vc, light, OverlayTexture.NO_OVERLAY, shade, shade, shade, 1);
            m.head.setPos(0, 0, 0);
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(180 - g.yaw));
            pose.scale(-0.9375f, -0.9375f, 0.9375f);
            pose.translate(0, -1.501, 0);
            // Arms hanging, head a little to one side
            m.head.zRot = 0.12f;
            m.hat.copyFrom(m.head);
            m.young = false;
            m.crouching = false;
            m.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY, shade * 0.8f, shade * 0.8f, shade * 0.8f, 1);
        }
        pose.popPose();
    }

    /** Dev screenshots: a figure and a peeker right now. */
    public static void devFigure() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        var p = mc.player;
        float a = p.getYRot() + 14;
        double x = p.getX() - Mth.sin(a * Mth.DEG_TO_RAD) * 18, z = p.getZ() + Mth.cos(a * Mth.DEG_TO_RAD) * 18;
        int y = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        GHOSTS.add(new Ghost(false, new Vec3(x, y, z), 0, 2000));
        Ghost h = peeker(mc.level, mc.player);
        if (h != null) {
            h.out = 1;
            GHOSTS.add(h);
        }
        nextGhost = 100000;
    }
}

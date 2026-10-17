package dev.factoryascension.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Breaking a block takes chunks out of it: pieces of each face go, more as you dig, leaving holes with walls and a
 * floor (shaders/core/fa_break: parallax, so they have depth). Stop digging and the pieces grow back over a second or
 * so. Full cubes only (anything else keeps the game's cracks); off in Pause > Factory Ascension (Breaking chunks), and
 * off by itself if it ever fails.
 */
public final class BreakChunks {
    private static ShaderInstance shader;
    private static final Map<BlockPos, Float> SHOWN = new HashMap<>();
    private static boolean broken = false;
    public static boolean devHold = false;
    public static int drawn = 0;
    public static String why = "no frame yet";

    private BreakChunks() {
    }

    /** Loaded with Ascension Lighting's shaders (AscensionLighting.Shaders). */
    static void setShader(ShaderInstance s) {
        shader = s;
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.isPaused()) return;
            BlockPos at = null;
            float target = 0;
            if (mc.gameMode != null && mc.gameMode.isDestroying() && mc.hitResult instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
                at = bh.getBlockPos().immutable();
                target = (mc.gameMode.getDestroyStage() + 1) / 10f;
            }
            if (at != null) SHOWN.merge(at, 0f, (a, b) -> a);
            for (Iterator<Map.Entry<BlockPos, Float>> it = SHOWN.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                if (mc.level.getBlockState(e.getKey()).isAir()) { it.remove(); continue; }   // broken through
                float v = e.getValue();
                if (e.getKey().equals(at)) v += (Math.max(v, target) - v) * 0.4f;            // digging: catch up
                else if (!devHold) v -= 0.035f;                                                // left alone: it grows back
                if (v <= 0) it.remove();
                else e.setValue(Math.min(1, v));
            }
        }

        @SubscribeEvent
        public static void draw(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
            why = SHOWN.isEmpty() ? "nothing" : shader == null ? "no shader" : broken ? "broken" : !FAClientConfig.on(FAClientConfig.BREAK_CHUNKS) ? "off" : "drawing";
            if (!why.equals("drawing")) return;
            try {
                drawAll(event.getPoseStack(), event.getProjectionMatrix());
            } catch (Throwable err) {
                broken = true;
                FAClientConfig.failSafe("Breaking chunks", err);
            }
        }
    }

    /** For tests: show this much broken at `p` (it grows back unless dug). */
    public static void devSet(BlockPos p, float v) {
        SHOWN.put(p.immutable(), v);
    }

    static final float[] SHADE = {0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f};   // down, up, north, south, west, east

    static void drawAll(PoseStack pose, Matrix4f proj) {
        Minecraft mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515);           // LEQUAL
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1f, -10f);
        RenderSystem.disableCull();            // whichever way round the corners go
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShader(() -> shader);
        RandomSource r = RandomSource.create(7);
        // The camera's turn goes in the model-view matrix, so the shader sees camera-relative world positions
        var mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.mulPoseMatrix(pose.last().pose());
        RenderSystem.applyModelViewMatrix();
        try {
            drawn = 0;
            for (var e : SHOWN.entrySet()) {
                BlockPos p = e.getKey();
                BlockState st = mc.level.getBlockState(p);
                if (!st.isSolidRender(mc.level, p)) continue;
                var model = mc.getBlockRenderer().getBlockModel(st);
                for (Direction d : Direction.values()) {
                    BlockPos side = p.relative(d);
                    if (mc.level.getBlockState(side).isSolidRender(mc.level, side)) continue;   // hidden face
                    var quads = model.getQuads(st, d, r, ModelData.EMPTY, null);
                    if (quads.isEmpty()) continue;
                    TextureAtlasSprite s = quads.get(0).getSprite();
                    // Lit by the air in front of the face (inside a solid block the light is always 0)
                    face(pose, proj, p, cam, d, s, e.getValue(), LevelRenderer.getLightColor(mc.level, side));
                    drawn++;
                }
            }
        } finally {
            mv.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(true);
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            mc.gameRenderer.lightTexture().turnOffLightLayer();
        }
    }

    static void face(PoseStack pose, Matrix4f proj, BlockPos p, net.minecraft.world.phys.Vec3 cam, Direction d, TextureAtlasSprite s, float progress, int light) {
        // The face's corners (just outside the block) and its u, v directions
        var n = d.getNormal();
        float ox = (float) (p.getX() - cam.x), oy = (float) (p.getY() - cam.y), oz = (float) (p.getZ() - cam.z);
        float e = 0.002f;
        float[][] c;
        float[] U, V;
        switch (d) {
            case UP -> { c = new float[][] {{0, 1 + e, 0}, {0, 1 + e, 1}, {1, 1 + e, 1}, {1, 1 + e, 0}}; U = new float[] {1, 0, 0}; V = new float[] {0, 0, 1}; }
            case DOWN -> { c = new float[][] {{0, -e, 1}, {0, -e, 0}, {1, -e, 0}, {1, -e, 1}}; U = new float[] {1, 0, 0}; V = new float[] {0, 0, -1}; }
            case NORTH -> { c = new float[][] {{1, 1, -e}, {1, 0, -e}, {0, 0, -e}, {0, 1, -e}}; U = new float[] {-1, 0, 0}; V = new float[] {0, -1, 0}; }
            case SOUTH -> { c = new float[][] {{0, 1, 1 + e}, {0, 0, 1 + e}, {1, 0, 1 + e}, {1, 1, 1 + e}}; U = new float[] {1, 0, 0}; V = new float[] {0, -1, 0}; }
            case WEST -> { c = new float[][] {{-e, 1, 0}, {-e, 0, 0}, {-e, 0, 1}, {-e, 1, 1}}; U = new float[] {0, 0, 1}; V = new float[] {0, -1, 0}; }
            default -> { c = new float[][] {{1 + e, 1, 1}, {1 + e, 0, 1}, {1 + e, 0, 0}, {1 + e, 1, 0}}; U = new float[] {0, 0, -1}; V = new float[] {0, -1, 0}; }
        }
        // u, v of each corner from where it sits along U and V
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        shader.safeGetUniform("Progress").set(progress);
        shader.safeGetUniform("SpriteBox").set(u0, v0, u1, v1);
        shader.safeGetUniform("FaceU").set(U[0], U[1], U[2]);
        shader.safeGetUniform("FaceV").set(V[0], V[1], V[2]);
        shader.safeGetUniform("FaceN").set((float) n.getX(), (float) n.getY(), (float) n.getZ());
        shader.safeGetUniform("Seed").set((float) ((p.asLong() * 31 + d.ordinal()) % 997));
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        float sh = SHADE[d.ordinal()];
        for (float[] k : c) {
            // Position along U and V (0..1), with the block's origin at the corner where U and V start
            float au = (U[0] < 0 || U[1] < 0 || U[2] < 0 ? 1 : 0) + k[0] * U[0] + k[1] * U[1] + k[2] * U[2];
            float av = (V[0] < 0 || V[1] < 0 || V[2] < 0 ? 1 : 0) + k[0] * V[0] + k[1] * V[1] + k[2] * V[2];
            b.vertex(ox + k[0], oy + k[1], oz + k[2]).color(sh, sh, sh, 1f).uv(u0 + (u1 - u0) * au, v0 + (v1 - v0) * av)
                .uv2(light).normal(n.getX(), n.getY(), n.getZ()).endVertex();
        }
        BufferUploader.drawWithShader(b.end());
    }
}

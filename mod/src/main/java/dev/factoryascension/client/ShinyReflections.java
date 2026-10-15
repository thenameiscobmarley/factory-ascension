package dev.factoryascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.FactoryAscension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shiny Reflections: light sources (and the sun) glint off shiny blocks (tag factoryascension:shiny), and the glints
 * slide across the surface as you move, the way a mirror image would. No shaders: twice a second it lists the exposed
 * shiny faces and the lights near you; every frame, for each face and light it finds where the light's mirror image
 * lands on the face from your eye and draws a small additive glint there.
 */
@Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ShinyReflections {
    static final TagKey<Block> SHINY = BlockTags.create(new ResourceLocation(FactoryAscension.MODID, "shiny"));

    private record Face(double cx, double cy, double cz, Direction dir, float shine, boolean sky) {
    }

    private record Light(double x, double y, double z, float power, float r, float g, float b) {
    }

    private static List<Face> faces = new ArrayList<>();
    private static List<Light> lights = new ArrayList<>();
    private static int timer = 0;

    private ShinyReflections() {
    }

    static boolean enabled() {
        return FAClientConfig.REFLECTIONS.get() && !ClientConflicts.reflectionsOff;
    }

    @SubscribeEvent
    public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientConflicts.tell();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!enabled() || mc.level == null || mc.player == null) {
            faces = new ArrayList<>();
            return;
        }
        if (--timer > 0) return;
        timer = 10;
        scan(mc.level, mc.player.blockPosition());
    }

    private static float shininess(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String id = key == null ? "" : key.getPath();
        if (id.contains("glass") || id.contains("ice")) return 0.85f;
        if (id.contains("polished") || id.contains("smooth") || id.contains("calcite") || id.contains("prismarine") || id.contains("obsidian")) return 0.5f;
        return 1.0f;   // metals, gems, quartz, the mod's machines
    }

    private static float[] lightColor(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String id = key == null ? "" : key.getPath();
        if (id.contains("soul")) return new float[]{0.45f, 0.8f, 1f};
        if (id.contains("redstone")) return new float[]{1f, 0.3f, 0.25f};
        if (id.contains("sea_lantern") || id.contains("beacon") || id.contains("end_rod") || id.contains("conduit")) return new float[]{0.8f, 1f, 1f};
        if (id.contains("amethyst") || id.contains("crying")) return new float[]{0.85f, 0.6f, 1f};
        if (id.contains("verdant")) return new float[]{0.6f, 1f, 0.6f};
        return new float[]{1f, 0.85f, 0.6f};   // fire: torches, lanterns, glowstone, lava
    }

    private static void scan(ClientLevel level, BlockPos center) {
        int r = FAClientConfig.RADIUS.get();
        List<Face> found = new ArrayList<>();
        List<Light> lit = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r - 4; dx <= r + 4; dx++)
            for (int dy = -r - 4; dy <= r + 4; dy++)
                for (int dz = -r - 4; dz <= r + 4; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    int emit = state.getLightEmission(level, pos);
                    if (emit > 0) {
                        float[] c = lightColor(state);
                        lit.add(new Light(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, emit / 15f, c[0], c[1], c[2]));
                    }
                    if (Math.abs(dx) > r || Math.abs(dy) > r || Math.abs(dz) > r || !state.is(SHINY) || found.size() > 8000) continue;
                    float shine = shininess(state);
                    for (Direction d : Direction.values()) {
                        BlockPos n = pos.relative(d);
                        BlockState ns = level.getBlockState(n);
                        if (!(ns.isAir() || (!ns.canOcclude() && !ns.is(state.getBlock())))) continue;
                        boolean sky = level.getBrightness(LightLayer.SKY, n) >= 12;
                        found.add(new Face(pos.getX() + 0.5 + d.getStepX() * 0.5, pos.getY() + 0.5 + d.getStepY() * 0.5, pos.getZ() + 0.5 + d.getStepZ() * 0.5, d, shine, sky));
                    }
                }
        double px = center.getX(), py = center.getY(), pz = center.getZ();
        lit.sort(Comparator.comparingDouble(l -> (l.x - px) * (l.x - px) + (l.y - py) * (l.y - py) + (l.z - pz) * (l.z - pz)));
        lights = lit.size() > 48 ? new ArrayList<>(lit.subList(0, 48)) : lit;
        faces = found;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || faces.isEmpty() || !enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        float strength = FAClientConfig.STRENGTH.get().floatValue();
        int budget = FAClientConfig.MAX_GLINTS.get();
        // The sun (or moon, dimmer) as a far light, for faces open to the sky
        boolean skyLit = level.dimensionType().hasSkyLight();
        float angle = level.getSunAngle(event.getPartialTick());
        double sx = -Math.sin(angle), sy = Math.cos(angle);
        boolean day = sy > 0;
        if (!day) { sx = -sx; sy = -sy; }
        float sunPower = day ? 0.7f : 0.25f;

        for (Face f : faces) {
            if (budget <= 0) break;
            int nx = f.dir.getStepX(), ny = f.dir.getStepY(), nz = f.dir.getStepZ();
            double vx = cam.x - f.cx, vy = cam.y - f.cy, vz = cam.z - f.cz;
            double side = vx * nx + vy * ny + vz * nz;
            if (side <= 0.02) continue;                         // seen from behind
            for (Light l : lights) {
                if (budget <= 0) break;
                double lx = l.x - f.cx, ly = l.y - f.cy, lz = l.z - f.cz;
                double ld = lx * nx + ly * ny + lz * nz;
                if (ld <= 0.05 || lx * lx + ly * ly + lz * lz > 256) continue;
                // The light's mirror image behind the face, and where the line from the eye to it crosses the face
                double mx = l.x - 2 * ld * nx, my = l.y - 2 * ld * ny, mz = l.z - 2 * ld * nz;
                double dx = mx - cam.x, dy = my - cam.y, dz = mz - cam.z;
                double denom = dx * nx + dy * ny + dz * nz;
                if (Math.abs(denom) < 1e-6) continue;
                double t = -side / denom;
                if (t <= 0 || t >= 1) continue;
                double hx = cam.x + dx * t, hy = cam.y + dy * t, hz = cam.z + dz * t;
                double ox = hx - f.cx, oy = hy - f.cy, oz = hz - f.cz;
                if (Math.abs(ox) > 0.5 || Math.abs(oy) > 0.5 || Math.abs(oz) > 0.5) continue;
                double path = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float a = (float) (f.shine * l.power * strength * 0.85 / (1 + path * path / 90));
                if (a < 0.03f) continue;
                glint(vc, m, cam, f, hx, hy, hz, 0.1 + 0.1 * f.shine, l.r, l.g, l.b, Math.min(1f, a));
                budget--;
            }
            if (skyLit && f.sky && budget > 0) {
                // Mirror the sun across the face and compare with the direction to the eye
                double sd = sx * nx + sy * ny;
                if (sd > 0) {
                    double rx = -sx + 2 * sd * nx, ry = -sy + 2 * sd * ny, rz = 2 * sd * nz;
                    double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
                    double spec = Math.pow(Math.max(0, (rx * vx + ry * vy + rz * vz) / vlen), 32);
                    float a = (float) (spec * f.shine * sunPower * strength);
                    if (a > 0.04f) {
                        glint(vc, m, cam, f, f.cx, f.cy, f.cz, 0.32, 1f, day ? 0.95f : 0.85f, day ? 0.85f : 1f, Math.min(1f, a));
                        budget--;
                    }
                }
            }
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** A soft glint on the face around (x, y, z): bright in the middle, fading to its edges, kept inside the face. */
    private static void glint(VertexConsumer vc, Matrix4f m, Vec3 cam, Face f, double x, double y, double z, double size, float r, float g, float b, float a) {
        int nx = f.dir.getStepX(), ny = f.dir.getStepY(), nz = f.dir.getStepZ();
        // Two in-plane axes u, v with u x v = n (so the quads face outward)
        double ux = ny != 0 ? 1 : nz != 0 ? 1 : 0, uy = 0, uz = nx != 0 ? 1 : 0;
        if (nx != 0) { ux = 0; uz = 1; }
        double vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
        double s = Math.min(size, 0.5);
        // Keep it on the face
        double cu = clamp((x - f.cx) * ux + (y - f.cy) * uy + (z - f.cz) * uz, s), cv = clamp((x - f.cx) * vx + (y - f.cy) * vy + (z - f.cz) * vz, s);
        double px = f.cx + cu * ux + cv * vx + nx * 0.004 - cam.x, py = f.cy + cu * uy + cv * vy + ny * 0.004 - cam.y, pz = f.cz + cu * uz + cv * vz + nz * 0.004 - cam.z;
        double[][] ring = {{s, 0}, {s, s}, {0, s}, {-s, s}, {-s, 0}, {-s, -s}, {0, -s}, {s, -s}};
        for (int i = 0; i < 8; i += 2) {
            double[] p1 = ring[i], p2 = ring[i + 1], p3 = ring[(i + 2) % 8];
            put(vc, m, px, py, pz, 0, 0, ux, uy, uz, vx, vy, vz, r, g, b, a);
            put(vc, m, px, py, pz, p1[0], p1[1], ux, uy, uz, vx, vy, vz, r, g, b, 0);
            put(vc, m, px, py, pz, p2[0], p2[1], ux, uy, uz, vx, vy, vz, r, g, b, 0);
            put(vc, m, px, py, pz, p3[0], p3[1], ux, uy, uz, vx, vy, vz, r, g, b, 0);
        }
    }

    private static double clamp(double v, double s) {
        return Math.max(-0.5 + s * 0.5, Math.min(0.5 - s * 0.5, v));
    }

    private static void put(VertexConsumer vc, Matrix4f m, double px, double py, double pz, double a, double b, double ux, double uy, double uz,
                            double vx, double vy, double vz, float r, float g, float bl, float alpha) {
        vc.vertex(m, (float) (px + a * ux + b * vx), (float) (py + a * uy + b * vy), (float) (pz + a * uz + b * vz)).color(r, g, bl, alpha).endVertex();
    }
}

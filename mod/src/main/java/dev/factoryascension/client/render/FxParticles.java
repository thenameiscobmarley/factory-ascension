package dev.factoryascension.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The mod's own particles (client only, added straight to the particle engine, no registry entries):
 *  - VoxelDebris: a small 3D cube cut from a broken block's texture that tumbles, bounces and settles.
 *  - SoftSmoke: big soft puffs that grow, drift with a little wind and fade.
 *  - Foam: white froth that floats where water falls or flows.
 * Every spawner checks its switch and a cap, so the cost never runs away.
 */
public final class FxParticles {
    private FxParticles() {
    }

    static int liveDebris = 0, liveSmoke = 0, liveFoam = 0;

    /** Our soft particle shader (no hard alpha cut-off); vanilla's particle shader until it's loaded. */
    public static net.minecraft.client.renderer.ShaderInstance softShader;

    /** Smoke and foam: translucent, from the particle atlas, drawn with the soft shader, not writing depth. */
    static final ParticleRenderType SOFT = new ParticleRenderType() {
        @Override
        public void begin(com.mojang.blaze3d.vertex.BufferBuilder b, net.minecraft.client.renderer.texture.TextureManager tm) {
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShader(() -> softShader != null ? softShader : net.minecraft.client.renderer.GameRenderer.getParticleShader());
            b.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(com.mojang.blaze3d.vertex.Tesselator t) {
            t.end();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "FACTORYASCENSION_SOFT";
        }
    };

    public static int liveDebris() { return liveDebris; }
    public static int liveSmoke() { return liveSmoke; }
    public static int liveFoam() { return liveFoam; }

    // Particle textures live in the particle engine's own atlas (not the block/model ones)
    private static TextureAtlasSprite particleSprite(String name) {
        return ((TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES)).getSprite(new ResourceLocation(name));
    }

    // ---- Spawning (called from FxClient with what the server said happened) ----

    /** A block shattering into `count` voxel pieces flying out from `from` (a block centre or an explosion). */
    public static void debris(ClientLevel level, BlockPos pos, BlockState state, Vec3 from, int count, double speed) {
        if (!FAClientConfig.on(FAClientConfig.DEBRIS) || state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) return;
        RandomSource r = level.random;
        int room = FAClientConfig.MAX_DEBRIS.get() - liveDebris;
        for (int i = 0; i < Math.min(count, room); i++) {
            double x = pos.getX() + 0.15 + r.nextDouble() * 0.7, y = pos.getY() + 0.15 + r.nextDouble() * 0.7, z = pos.getZ() + 0.15 + r.nextDouble() * 0.7;
            Vec3 out = new Vec3(x - from.x, y - from.y, z - from.z);
            out = out.lengthSqr() < 1e-4 ? new Vec3(r.nextGaussian(), 1, r.nextGaussian()) : out.normalize();
            double s = speed * (0.5 + r.nextDouble());
            Minecraft.getInstance().particleEngine.add(new VoxelDebris(level, x, y, z, out.x * s, out.y * s + 0.15 + r.nextDouble() * 0.2, out.z * s, state, pos));
        }
    }

    /** A burst of smoke `size` blocks across (an explosion is about its radius, a gunshot about 0.4). */
    public static void smoke(ClientLevel level, double x, double y, double z, double size, Vec3 push) {
        if (!FAClientConfig.on(FAClientConfig.SMOKE)) return;
        RandomSource r = level.random;
        int n = (int) Math.ceil(Mth.clamp(size * 1.6 * FAClientConfig.SMOKE_AMOUNT.get(), 1, 18));
        for (int i = 0; i < n && liveSmoke < 150; i++) {   // a cap, so a pile of TNT can't fog the whole screen
            double ox = r.nextGaussian() * size * 0.4, oy = r.nextDouble() * size * 0.5, oz = r.nextGaussian() * size * 0.4;
            Minecraft.getInstance().particleEngine.add(new SoftSmoke(level, x + ox, y + oy, z + oz,
                push.x * 0.05 + ox * 0.03, 0.02 + r.nextDouble() * 0.03 + push.y * 0.05, push.z * 0.05 + oz * 0.03, 0.6 + size * 0.5));
        }
    }

    /** Foam on the water's surface. */
    public static void foam(ClientLevel level, double x, double y, double z, double vx, double vz) {
        if (liveFoam >= 900) return;
        Minecraft.getInstance().particleEngine.add(new Foam(level, x, y, z, vx, vz));
    }

    // ---- The particles ----

    /** A tumbling cube of block texture. */
    static final class VoxelDebris extends TextureSheetParticle {
        private final float u0, v0, u1, v1, size;
        private final Vector3f spinAxis;
        private float angle, angleO, spin;
        private boolean settled;

        VoxelDebris(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, BlockState state, BlockPos pos) {
            super(level, x, y, z);
            setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state));
            xd = vx; yd = vy; zd = vz;
            gravity = 1.0f;
            friction = 0.98f;
            hasPhysics = true;
            size = 0.05f + random.nextFloat() * 0.07f;
            setSize(size * 2, size * 2);
            lifetime = 80 + random.nextInt(80);
            // A random 4x4 corner of the block's texture, like vanilla breaking particles
            float pu = random.nextFloat() * 0.75f, pv = random.nextFloat() * 0.75f;
            u0 = sprite.getU(pu * 16); u1 = sprite.getU((pu + 0.25f) * 16);
            v0 = sprite.getV(pv * 16); v1 = sprite.getV((pv + 0.25f) * 16);
            int tint = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            if (tint != -1 && !state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                rCol = (tint >> 16 & 255) / 255f; gCol = (tint >> 8 & 255) / 255f; bCol = (tint & 255) / 255f;
            } else if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                rCol = 0.85f; gCol = 0.8f; bCol = 0.7f;   // grass blocks break into dirt-coloured bits
            }
            spinAxis = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize();
            spin = (random.nextFloat() - 0.5f) * 0.6f;
            liveDebris++;
        }

        @Override
        public void tick() {
            xo = x; yo = y; zo = z;
            angleO = angle;
            if (age++ >= lifetime) { remove(); return; }
            double vyBefore = yd;
            yd -= 0.04 * gravity;
            move(xd, yd, zd);
            if (onGround) {
                // Bounce, losing most of the energy; then roll to a stop
                if (Math.abs(vyBefore) > 0.12) { yd = -vyBefore * 0.35; spin *= 0.6f; }
                else { yd = 0; settled = true; }
                xd *= 0.6; zd *= 0.6;
            }
            xd *= friction; zd *= friction;
            if (!settled) angle += spin;
            if (lifetime - age < 20) alpha = (lifetime - age) / 20f;
        }

        @Override
        public void render(VertexConsumer out, Camera cam, float partial) {
            Vec3 c = cam.getPosition();
            float px = (float) (Mth.lerp(partial, xo, x) - c.x), py = (float) (Mth.lerp(partial, yo, y) - c.y) + size, pz = (float) (Mth.lerp(partial, zo, z) - c.z);
            Quaternionf q = new Quaternionf().fromAxisAngleRad(spinAxis, Mth.lerp(partial, angleO, angle));
            int light = getLightColor(partial);
            float s = size;
            // Six faces, each a quad of the texture piece, shaded a little per side like blocks
            float[][] faces = {
                {-1, 0, 0, 0.6f}, {1, 0, 0, 0.6f}, {0, -1, 0, 0.5f}, {0, 1, 0, 1f}, {0, 0, -1, 0.8f}, {0, 0, 1, 0.8f}};
            for (float[] f : faces) {
                Vector3f n = new Vector3f(f[0], f[1], f[2]);
                Vector3f a = Math.abs(n.y) > 0.5f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
                Vector3f b = new Vector3f(n).cross(a);
                Vector3f[] corners = new Vector3f[4];
                float[][] sign = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
                for (int k = 0; k < 4; k++) {
                    Vector3f v = new Vector3f(n).mul(s).add(new Vector3f(a).mul(sign[k][0] * s)).add(new Vector3f(b).mul(sign[k][1] * s));
                    q.transform(v);
                    corners[k] = v.add(px, py, pz);
                }
                float shade = f[3];
                float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
                for (int k = 0; k < 4; k++)
                    out.vertex(corners[k].x, corners[k].y, corners[k].z).uv(uv[k][0], uv[k][1]).color(rCol * shade, gCol * shade, bCol * shade, alpha).uv2(light).endVertex();
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.TERRAIN_SHEET;
        }

        @Override
        public void remove() {
            if (!removed) liveDebris--;
            super.remove();
        }
    }

    /** A soft, growing puff of smoke. */
    static final class SoftSmoke extends TextureSheetParticle {
        private final float startSize;

        SoftSmoke(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, double size) {
            super(level, x, y, z);
            setSprite(particleSprite("factoryascension:soft_smoke_" + random.nextInt(4)));
            xd = vx; yd = vy; zd = vz;
            gravity = -0.01f;
            friction = 0.96f;
            hasPhysics = true;
            startSize = (float) size * (0.7f + random.nextFloat() * 0.6f);
            quadSize = startSize;
            lifetime = 70 + random.nextInt(90);
            float g = 0.45f + random.nextFloat() * 0.25f;
            rCol = gCol = bCol = g;
            alpha = 0.26f;
            roll = oRoll = random.nextFloat() * 6.28f;
            liveSmoke++;
        }

        @Override
        public void tick() {
            super.tick();
            float life = age / (float) lifetime;
            quadSize = startSize * (1 + life * 1.5f);
            alpha = 0.26f * Math.min(1, age / 4f) * (1 - life) * (1 - life);
            xd += 0.0015;   // a light breeze
            oRoll = roll;
            roll += 0.004f;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SOFT;
        }

        @Override
        public void remove() {
            if (!removed) liveSmoke--;
            super.remove();
        }
    }

    /** White froth floating on water. */
    static final class Foam extends TextureSheetParticle {
        Foam(ClientLevel level, double x, double y, double z, double vx, double vz) {
            super(level, x, y, z);
            setSprite(particleSprite("minecraft:generic_" + (4 + random.nextInt(4))));
            xd = vx; yd = 0; zd = vz;
            gravity = 0;
            friction = 0.9f;
            hasPhysics = false;
            quadSize = 0.1f + random.nextFloat() * 0.12f;
            lifetime = 40 + random.nextInt(50);
            rCol = gCol = bCol = 0.95f;
            alpha = 0.85f;
            liveFoam++;
        }

        @Override
        public void tick() {
            super.tick();
            alpha = 0.85f * (1 - age / (float) lifetime);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SOFT;
        }

        @Override
        public void remove() {
            if (!removed) liveFoam--;
            super.remove();
        }
    }
}

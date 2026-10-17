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

    /**
     * Voxel debris: the game's terrain particle sheet, but opaque. With blending on, the depth pack's block heights
     * (kept in the texture's alpha) made the pieces see-through.
     */
    static final ParticleRenderType SOLID_TERRAIN = new ParticleRenderType() {
        @Override
        public void begin(com.mojang.blaze3d.vertex.BufferBuilder b, net.minecraft.client.renderer.texture.TextureManager tm) {
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
            b.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(com.mojang.blaze3d.vertex.Tesselator t) {
            t.end();
        }

        @Override
        public String toString() {
            return "FACTORYASCENSION_SOLID_TERRAIN";
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

    /**
     * A piece broken off a block that's being mined (BreakChunks): a box `sx` x `sy` x `sz` (half sizes) centred at
     * x, y, z, wearing the given piece of `sprite`. Returns false when the cap is full.
     */
    public static boolean chunk(ClientLevel level, BlockState state, BlockPos pos, double x, double y, double z, double vx, double vy, double vz,
                                TextureAtlasSprite sprite, float pu, float pv, float uw, float vh, float sx, float sy, float sz, int tint) {
        if (!FAClientConfig.on(FAClientConfig.DEBRIS) || liveDebris >= FAClientConfig.MAX_DEBRIS.get()) return false;
        VoxelDebris d = new VoxelDebris(level, x, y - sy, z, vx, vy, vz, state, pos);
        d.cut(sprite, pu, pv, uw, vh, sx, sy, sz, tint);
        Minecraft.getInstance().particleEngine.add(d);
        return true;
    }

    /** Small crumbs and a little dust where a piece breaks off or lands (the game's own block particles). */
    public static void crumbs(ClientLevel level, BlockState state, double x, double y, double z, int n, double spread) {
        if (!FAClientConfig.on(FAClientConfig.DEBRIS)) return;
        RandomSource r = level.random;
        var crumb = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, state);
        for (int i = 0; i < n; i++)
            level.addParticle(crumb, x + (r.nextDouble() - 0.5) * spread, y + r.nextDouble() * spread * 0.5, z + (r.nextDouble() - 0.5) * spread,
                (r.nextDouble() - 0.5) * 0.1, r.nextDouble() * 0.08, (r.nextDouble() - 0.5) * 0.1);
    }

    private static long soundTick;
    private static int soundsThisTick;

    /** A piece landing hard: a quiet knock in the block's own sound (a few a tick at most, or a pile would roar). */
    static void knock(ClientLevel level, BlockState state, double x, double y, double z, float impact) {
        long t = level.getGameTime();
        if (t != soundTick) { soundTick = t; soundsThisTick = 0; }
        if (soundsThisTick++ >= 3) return;
        var st = state.getSoundType();
        level.playLocalSound(x, y, z, st.getHitSound(), net.minecraft.sounds.SoundSource.BLOCKS, Math.min(0.35f, 0.08f + impact * 0.8f),
            st.getPitch() * (1.15f + level.random.nextFloat() * 0.35f), false);
    }

    // ---- The particles ----

    /**
     * A tumbling piece of a block: a small rigid box with the block's texture. It spins about any axis while it flies,
     * bounces off the ground losing most of its speed (a hard landing knocks it into a new spin), slides and rolls to a
     * stop and settles flat on one of its faces, then shrinks away at the end of its life. Its faces are shaded by
     * which way they really face, like the blocks around it.
     */
    static final class VoxelDebris extends TextureSheetParticle {
        private float u0, v0, u1, v1;
        private float hx, hy, hz;
        private final BlockState state;
        private final Quaternionf rot = new Quaternionf(), rotO = new Quaternionf();
        private final Vector3f spin = new Vector3f();
        private boolean resting, chunky;
        private float shrink = 1, shrinkO = 1;

        VoxelDebris(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, BlockState state, BlockPos pos) {
            super(level, x, y, z);
            this.state = state;
            setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state));
            xd = vx; yd = vy; zd = vz;
            hasPhysics = true;
            float size = 0.05f + random.nextFloat() * 0.07f;
            hx = size * (0.8f + random.nextFloat() * 0.4f);
            hy = size * (0.8f + random.nextFloat() * 0.4f);
            hz = size * (0.8f + random.nextFloat() * 0.4f);
            lifetime = 90 + random.nextInt(80);
            // A random 4x4 corner of the block's texture, like vanilla breaking particles
            float pu = random.nextFloat() * 0.75f, pv = random.nextFloat() * 0.75f;
            u0 = sprite.getU(pu * 16); u1 = sprite.getU((pu + 0.25f) * 16);
            v0 = sprite.getV(pv * 16); v1 = sprite.getV((pv + 0.25f) * 16);
            int tint = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) { rCol = 0.85f; gCol = 0.8f; bCol = 0.7f; }   // dirt-coloured bits
            else if (tint != -1) { rCol = (tint >> 16 & 255) / 255f; gCol = (tint >> 8 & 255) / 255f; bCol = (tint & 255) / 255f; }
            rot.rotateXYZ(random.nextFloat() * 6.28f, random.nextFloat() * 6.28f, random.nextFloat() * 6.28f);
            rotO.set(rot);
            spin.set(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize().mul(0.15f + random.nextFloat() * 0.3f);
            fitBox();
            liveDebris++;
        }

        /** Re-cut as a BreakChunks piece: its own spot on the face texture, its size and tint, starting square. */
        void cut(TextureAtlasSprite s, float pu, float pv, float uw, float vh, float sx, float sy, float sz, int tint) {
            setSprite(s);
            u0 = s.getU(pu * 16); u1 = s.getU((pu + uw) * 16);
            v0 = s.getV(pv * 16); v1 = s.getV((pv + vh) * 16);
            hx = sx; hy = sy; hz = sz;
            if (tint == -1) rCol = gCol = bCol = 1;
            else { rCol = (tint >> 16 & 255) / 255f; gCol = (tint >> 8 & 255) / 255f; bCol = (tint & 255) / 255f; }
            rot.identity();
            rotO.identity();
            spin.set(random.nextFloat() - 0.5f, (random.nextFloat() - 0.5f) * 0.5f, random.nextFloat() - 0.5f).normalize().mul(0.08f + random.nextFloat() * 0.2f);
            chunky = true;
            lifetime = 180 + random.nextInt(100);
            fitBox();
        }

        /** How far the box reaches along each world axis, turned as it is now. */
        private Vector3f extent() {
            org.joml.Matrix3f m = new org.joml.Matrix3f().set(rot);
            return new Vector3f(Math.abs(m.m00()) * hx + Math.abs(m.m10()) * hy + Math.abs(m.m20()) * hz,
                Math.abs(m.m01()) * hx + Math.abs(m.m11()) * hy + Math.abs(m.m21()) * hz,
                Math.abs(m.m02()) * hx + Math.abs(m.m12()) * hy + Math.abs(m.m22()) * hz).mul(shrink);
        }

        /** The collision box: around the turned box, standing on the particle's position (its bottom). */
        private void fitBox() {
            Vector3f e = extent();
            setBoundingBox(new net.minecraft.world.phys.AABB(x - e.x, y, z - e.z, x + e.x, y + e.y * 2, z + e.z));
        }

        /** The nearest way to lie flat: each of its axes turned to the nearest world axis. */
        private Quaternionf flat() {
            Vector3f a = rot.transform(new Vector3f(1, 0, 0)), b = rot.transform(new Vector3f(0, 1, 0));
            Vector3f sa = snap(a, null), sb = snap(b, sa);
            Vector3f sc = new Vector3f(sa).cross(sb);
            org.joml.Matrix3f m = new org.joml.Matrix3f(sa, sb, sc);
            return new Quaternionf().setFromNormalized(m);
        }

        private static Vector3f snap(Vector3f v, @javax.annotation.Nullable Vector3f not) {
            float ax = Math.abs(v.x), ay = Math.abs(v.y), az = Math.abs(v.z);
            if (not != null) { if (not.x != 0) ax = -1; if (not.y != 0) ay = -1; if (not.z != 0) az = -1; }
            if (ax >= ay && ax >= az) return new Vector3f(Math.signum(v.x) == 0 ? 1 : Math.signum(v.x), 0, 0);
            if (ay >= az) return new Vector3f(0, Math.signum(v.y) == 0 ? 1 : Math.signum(v.y), 0);
            return new Vector3f(0, 0, Math.signum(v.z) == 0 ? 1 : Math.signum(v.z));
        }

        @Override
        public void tick() {
            xo = x; yo = y; zo = z;
            rotO.set(rot);
            shrinkO = shrink;
            if (age++ >= lifetime) { remove(); return; }
            int left = lifetime - age;
            if (left < 16) shrink = left / 16f;
            if (!resting) {
                double vyBefore = yd;
                yd -= 0.045;                     // gravity
                xd *= 0.99; yd *= 0.99; zd *= 0.99;   // air
                collideMove(xd, yd, zd);
                if (onGround) {
                    float impact = (float) -vyBefore;
                    if (impact > 0.09f) {
                        // A bounce: most of the speed goes, and the knock sets it spinning a new way
                        yd = impact * 0.28;
                        xd *= 0.72; zd *= 0.72;
                        spin.mul(0.5f).add((random.nextFloat() - 0.5f) * impact * 3, (random.nextFloat() - 0.5f) * impact, (random.nextFloat() - 0.5f) * impact * 3);
                        if (impact > 0.16f) {
                            knock(level, state, x, y, z, impact);
                            crumbs(level, state, x, y, z, chunky ? 2 : 1, hx * 2);
                        }
                    } else {
                        // On the ground: slide and roll to a stop, and tip onto the nearest face
                        yd = 0;
                        xd *= 0.6; zd *= 0.6;
                        spin.mul(0.45f);
                        rot.slerp(flat(), 0.3f);
                        if (xd * xd + zd * zd < 1e-5 && spin.lengthSquared() < 1e-4) {
                            rot.set(flat());
                            resting = true;
                        }
                    }
                }
                float w = spin.length();
                if (w > 1e-4f) rot.premul(new Quaternionf().fromAxisAngleRad(spin.x / w, spin.y / w, spin.z / w, w));
                fitBox();
            } else if (left < 16) {
                fitBox();
            }
        }

        /**
         * Our own move: vanilla particles stop moving for good after their first vertical bump (a ceiling, the edge of
         * a block mid-bounce), which left pieces frozen in the air. This one always keeps falling until it's resting.
         */
        private void collideMove(double dx, double dy, double dz) {
            double ox = dx, oy = dy, oz = dz;
            Vec3 v = net.minecraft.world.entity.Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), getBoundingBox(), level, java.util.List.of());
            dx = v.x; dy = v.y; dz = v.z;
            if (dx != 0 || dy != 0 || dz != 0) {
                setBoundingBox(getBoundingBox().move(dx, dy, dz));
                setLocationFromBoundingbox();
            }
            onGround = oy != dy && oy < 0;
            if (ox != dx) { xd = -xd * 0.35; spin.y += (float) (ox * 2); }   // off a wall, turning
            if (oz != dz) { zd = -zd * 0.35; spin.y -= (float) (oz * 2); }
            if (oy != dy && oy > 0) yd = 0;    // under a ceiling: drop
            // Stuck inside a block (it spawned in one): let it fall through rather than hang there
            if (dx == 0 && dy == 0 && dz == 0 && oy < 0 && !level.getBlockState(net.minecraft.core.BlockPos.containing(x, y - 0.05, z)).isSolid()) {
                setBoundingBox(getBoundingBox().move(0, oy, 0));
                setLocationFromBoundingbox();
                onGround = false;
            }
        }

        /**
         * Lit by the brightest spot around it: a piece resting in a half-mined block's hole (or sunk a little into
         * the ground) is inside a solid block as far as light goes, which is always dark.
         */
        @Override
        protected int getLightColor(float partial) {
            BlockPos at = BlockPos.containing(x, y + 0.05, z);
            int best = 0, bestSum = -1;
            for (BlockPos p : new BlockPos[] {at, at.above(), at.north(), at.south(), at.east(), at.west()}) {
                if (!level.hasChunkAt(p)) continue;
                int c = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, p);
                int sum = (c & 0xFFFF) + (c >>> 16);
                if (sum > bestSum) { bestSum = sum; best = c; }
                if (p == at && !level.getBlockState(p).isSolidRender(level, p)) break;   // in the open: its own light
            }
            return best;
        }

        // Face normals and the two edge directions of each face (anticlockwise seen from outside)
        private static final float[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

        @Override
        public void render(VertexConsumer out, Camera cam, float partial) {
            Vec3 c = cam.getPosition();
            Quaternionf q = new Quaternionf(rotO).slerp(rot, partial);
            float s = Mth.lerp(partial, shrinkO, shrink);
            if (s <= 0.01f) return;
            Vector3f e = extent();
            float px = (float) (Mth.lerp(partial, xo, x) - c.x), py = (float) (Mth.lerp(partial, yo, y) - c.y) + e.y, pz = (float) (Mth.lerp(partial, zo, z) - c.z);
            int light = getLightColor(partial);
            float[] half = {hx * s, hy * s, hz * s};
            for (float[] f : FACES) {
                Vector3f n = new Vector3f(f[0], f[1], f[2]);
                // The two directions across this face, and how far the box reaches along each
                int ai = n.x != 0 ? 1 : 0, bi = n.z != 0 ? 1 : 2;
                if (n.y != 0) { ai = 0; bi = 2; }
                Vector3f a = axis(ai), b = axis(bi);
                if (new Vector3f(a).cross(b).dot(n) < 0) { Vector3f t = a; a = b; b = t; int ti = ai; ai = bi; bi = ti; }
                float hn = n.x != 0 ? half[0] : n.y != 0 ? half[1] : half[2];
                float[][] sign = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                Vector3f wn = q.transform(new Vector3f(n));
                // Shaded by where it really faces, as blocks are: up bright, down dark, sides between
                float shade = (Math.max(wn.y, 0) * 1.0f + Math.max(-wn.y, 0) * 0.5f + Math.abs(wn.x) * 0.6f + Math.abs(wn.z) * 0.8f);
                float[][] uv = {{u0, v1}, {u1, v1}, {u1, v0}, {u0, v0}};
                for (int k = 0; k < 4; k++) {
                    Vector3f v = new Vector3f(n).mul(hn).add(new Vector3f(a).mul(sign[k][0] * half[ai])).add(new Vector3f(b).mul(sign[k][1] * half[bi]));
                    q.transform(v);
                    out.vertex(v.x + px, v.y + py, v.z + pz).uv(uv[k][0], uv[k][1]).color(rCol * shade, gCol * shade, bCol * shade, alpha).uv2(light).endVertex();
                }
            }
        }

        private static Vector3f axis(int i) {
            return i == 0 ? new Vector3f(1, 0, 0) : i == 1 ? new Vector3f(0, 1, 0) : new Vector3f(0, 0, 1);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return SOLID_TERRAIN;
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

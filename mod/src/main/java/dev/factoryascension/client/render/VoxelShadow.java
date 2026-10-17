package dev.factoryascension.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * The blocks around you, for sun shadows that stay put: a 96 x 64 x 96 grid (how much each block stops the sun:
 * solid 255, leaves 170, slabs/stairs/fences and other part-blocks 130, else 0) kept on the GPU as one 2D texture
 * (96 slices of 96 x 64, 12 across, 8 down). Addressed by world position modulo the size, so as you move only the
 * slices that came into range are refilled (a few a frame; changed blocks catch up within a second or two). The
 * shadow pass marches toward the sun through it in world space: the same shadow wherever the camera looks.
 */
public final class VoxelShadow {
    public static final int SX = 96, SY = 64, SZ = 96, COLS = 12, ROWS = 8;
    /** The window: from the camera's block, -48..+47 across, -24..+39 up. */
    public static final int BELOW = 24;
    private static int tex = -1;
    private static ByteBuffer slice;
    /** Per z slice: the world z, centre x and y it was filled for (Integer.MIN_VALUE = never). */
    private static final int[] wzOf = new int[SZ], cxOf = new int[SZ], cyOf = new int[SZ];
    private static int robin = 0;
    private static Object levelOf = null;

    private VoxelShadow() {
    }

    public static int texture() {
        return tex;
    }

    static void ensure() {
        if (tex >= 0) return;
        tex = GlStateManager._genTexture();
        RenderSystem.bindTexture(tex);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, SX * COLS, SY * ROWS, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        slice = MemoryUtil.memAlloc(SX * SY);
        java.util.Arrays.fill(wzOf, Integer.MIN_VALUE);
        // Start empty (no shadows) rather than with whatever the driver had there
        for (int i = 0; i < SX * SY; i++) slice.put(i, (byte) 0);
        for (int z = 0; z < SZ; z++) upload(z);
    }

    public static void free() {
        if (tex >= 0) GlStateManager._deleteTexture(tex);
        tex = -1;
        if (slice != null) MemoryUtil.memFree(slice);
        slice = null;
    }

    static byte opacity(BlockState s) {
        if (s.isAir()) return 0;
        if (s.is(BlockTags.LEAVES)) return (byte) 170;
        if (s.canOcclude() && s.isSolidRender(Minecraft.getInstance().level, BlockPos.ZERO)) return (byte) 255;
        if (!s.getFluidState().isEmpty() && s.getCollisionShape(Minecraft.getInstance().level, BlockPos.ZERO).isEmpty()) return 0;
        if (s.canOcclude()) return (byte) 200;
        return s.getCollisionShape(Minecraft.getInstance().level, BlockPos.ZERO).isEmpty() ? (byte) 0 : (byte) 130;
    }

    static void upload(int zi) {
        RenderSystem.bindTexture(tex);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 1);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, (zi % COLS) * SX, (zi / COLS) * SY, SX, SY, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, slice);
    }

    /** Fill one z slice (world z `wz`) around the centre. */
    static void fill(int zi, int wz, int cx, int cy) {
        var level = Minecraft.getInstance().level;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy < SY; dy++) {
            int wy = cy - BELOW + dy;
            int row = Math.floorMod(wy, SY);
            for (int dx = 0; dx < SX; dx++) {
                int wx = cx - SX / 2 + dx;
                int col = Math.floorMod(wx, SX);
                p.set(wx, wy, wz);
                slice.put(row * SX + col, opacity(level.getBlockState(p)));
            }
        }
        upload(zi);
        wzOf[zi] = wz;
        cxOf[zi] = cx;
        cyOf[zi] = cy;
    }

    /** Each frame: refill the slices that are out of date (moved into range first), then a few round the clock. */
    public static void update(int budget) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ensure();
        if (levelOf != mc.level) {
            levelOf = mc.level;
            java.util.Arrays.fill(wzOf, Integer.MIN_VALUE);
        }
        BlockPos c = mc.gameRenderer.getMainCamera().getBlockPosition();
        int done = 0;
        // Out of date: a slice's world z no longer the one in range, or the window moved a few blocks across or up
        for (int k = 0; k < SZ && done < budget; k++) {
            int wz = c.getZ() - SZ / 2 + k;
            int zi = Math.floorMod(wz, SZ);
            if (wzOf[zi] != wz || Math.abs(cxOf[zi] - c.getX()) > 6 || Math.abs(cyOf[zi] - c.getY()) > 6) {
                fill(zi, wz, c.getX(), c.getY());
                done++;
            }
        }
        // And keep refreshing in turn, so broken and placed blocks show up
        while (done < budget) {
            robin = (robin + 1) % SZ;
            int wz = c.getZ() - SZ / 2 + robin;
            fill(Math.floorMod(wz, SZ), wz, c.getX(), c.getY());
            done++;
        }
    }

    /** Right away, the slice of a block that changed (broken, placed). */
    public static void changed(BlockPos pos) {
        if (tex < 0 || Minecraft.getInstance().level == null) return;
        int zi = Math.floorMod(pos.getZ(), SZ);
        if (wzOf[zi] == pos.getZ()) fill(zi, pos.getZ(), cxOf[zi], cyOf[zi]);
    }
}

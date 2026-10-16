package dev.factoryascension.worldgen;

import dev.factoryascension.FAConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Where the mod's overworld structures may go: the world is cut into square cells (2 x FAConfig.structureSpacing a
 * side, 1000 blocks by default) and each cell holds at most one site, somewhere in its first `spacing` x `spacing`
 * corner, so any two sites are at least `spacing` (500) blocks apart. Whether a cell has a site at all, where it is and
 * what it is come from the world seed and the cell (the same every time, on any thread). Which kind: weighted by how
 * common each kind is, from those allowed that far from spawn. Placing one by command (/place feature) skips all this.
 */
public final class SiteGrid {
    private SiteGrid() {
    }

    /** kind id -> {weight (bigger = more common), minimum distance from spawn} */
    static final Map<String, int[]> KINDS = new LinkedHashMap<>();

    static {
        // Weights are 100000 / the old per-chunk rarity, so the mix stays what it was
        kind("factory_ruin", 400, 0);
        kind("ascension_crypt", 300, 0);
        for (Outposts.Kind k : Outposts.Kind.values()) kind(k.id, switch (k) {
            case SCAV_CAMP -> 220; case RUINS -> 260; case GAS_STATION -> 340; case CHECKPOINT -> 300; case TRENCH -> 360;
            case SNIPER_TOWER -> 320; case OUTPOST -> 520; case BUNKER -> 420; case CRASH_SITE -> 480; case RADIO_TOWER -> 460;
            case FORT -> 900; case CULT_SHRINE -> 700;
        }, k.minDistance);
    }

    static void kind(String id, int rarity, int minDist) {
        KINDS.put(id, new int[] {100000 / rarity, minDist});
    }

    record Site(int x, int z, String kind) {
    }

    @Nullable
    static Site site(long seed, int gx, int gz, BlockPos spawn) {
        int spacing = Math.max(64, FAConfig.structureSpacing), cell = spacing * 2;
        Random r = new Random(seed ^ (gx * 341873128712L) ^ (gz * 132897987541L) ^ 0x5DEECE66DL);
        if (r.nextFloat() >= FAConfig.structureCellChance) return null;
        int x = gx * cell + r.nextInt(spacing), z = gz * cell + r.nextInt(spacing);
        double dist = Math.hypot(x - spawn.getX(), z - spawn.getZ());
        int total = 0;
        for (int[] k : KINDS.values()) if (dist >= k[1]) total += k[0];
        if (total <= 0) return null;
        int pick = r.nextInt(total);
        for (var e : KINDS.entrySet()) {
            if (dist < e.getValue()[1]) continue;
            pick -= e.getValue()[0];
            if (pick < 0) return new Site(x, z, e.getKey());
        }
        return null;
    }

    /** Should `kind` be built in the chunk around `origin`? Always yes outside world generation (a command). */
    public static boolean here(WorldGenLevel level, String kind, BlockPos origin) {
        if (!(level instanceof net.minecraft.server.level.WorldGenRegion)) return true;
        int cell = Math.max(64, FAConfig.structureSpacing) * 2;
        Site s = site(level.getSeed(), Math.floorDiv(origin.getX(), cell), Math.floorDiv(origin.getZ(), cell), level.getLevel().getSharedSpawnPos());
        return s != null && s.kind().equals(kind) && (s.x() >> 4) == (origin.getX() >> 4) && (s.z() >> 4) == (origin.getZ() >> 4);
    }

    /**
     * Dev check: every site in a square `radius` blocks round (0, 0) for this seed; returns {count, closest distance}
     * and draws a map (one pixel per 20 blocks, a coloured dot per site) to `png` if given.
     */
    public static double[] survey(long seed, BlockPos spawn, int radius, @Nullable java.io.File png) {
        int cell = Math.max(64, FAConfig.structureSpacing) * 2;
        java.util.List<Site> all = new java.util.ArrayList<>();
        for (int gx = -radius / cell - 1; gx <= radius / cell; gx++)
            for (int gz = -radius / cell - 1; gz <= radius / cell; gz++) {
                Site s = site(seed, gx, gz, spawn);
                if (s != null && Math.abs(s.x()) <= radius && Math.abs(s.z()) <= radius) all.add(s);
            }
        double best = Double.MAX_VALUE;
        for (int i = 0; i < all.size(); i++)
            for (int j = i + 1; j < all.size(); j++)
                best = Math.min(best, Math.hypot(all.get(i).x() - all.get(j).x(), all.get(i).z() - all.get(j).z()));
        if (png != null) {
            int size = radius * 2 / 20;
            var img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = img.createGraphics();
            g.setColor(new java.awt.Color(24, 28, 24));
            g.fillRect(0, 0, size, size);
            g.setColor(new java.awt.Color(48, 56, 48));
            for (int c = -radius; c <= radius; c += cell) {
                int q = (c + radius) / 20;
                g.drawLine(q, 0, q, size);
                g.drawLine(0, q, size, q);
            }
            for (Site s : all) {
                int px = (s.x() + radius) / 20, pz = (s.z() + radius) / 20;
                g.setColor(java.awt.Color.getHSBColor((Math.abs(s.kind().hashCode()) % 360) / 360f, 0.7f, 1f));
                g.fillOval(px - 4, pz - 4, 9, 9);
                g.setColor(new java.awt.Color(255, 255, 255, 60));
                int r = Math.max(64, FAConfig.structureSpacing) / 20;
                g.drawOval(px - r / 2, pz - r / 2, r, r);
            }
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(size / 2 - 2, size / 2 - 2, 5, 5);
            g.dispose();
            try {
                javax.imageio.ImageIO.write(img, "png", png);
            } catch (java.io.IOException ignored) {
            }
        }
        return new double[] {all.size(), best};
    }
}

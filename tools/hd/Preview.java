import dev.factoryascension.client.hd.Upscale;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.zip.ZipFile;

/**
 * Before/after sheet for the HD texture pack's painter (client/hd/Upscale), without the game:
 *   javac -d <out> mod/src/main/java/dev/factoryascension/client/hd/Upscale.java tools/hd/Preview.java
 *   java -cp <out> Preview <minecraft client jar> <out.png> block/stone block/oak_planks item/diamond_sword ...
 * Each row: the original (scaled up 8x, as it looks in game) and the HD version (scaled 4x, same size).
 */
public class Preview {
    public static void main(String[] a) throws Exception {
        int n = a.length - 2, cell = 128;
        BufferedImage sheet = new BufferedImage(cell * 2 + 12, n * (cell + 6), BufferedImage.TYPE_INT_ARGB);
        try (ZipFile zip = new ZipFile(a[0])) {
            for (int i = 0; i < n; i++) {
                String name = a[i + 2];
                var e = zip.getEntry("assets/minecraft/textures/" + name + ".png");
                BufferedImage img = ImageIO.read(zip.getInputStream(e));
                int w = img.getWidth(), h = Math.min(img.getHeight(), w);   // first frame only
                int[] px = img.getRGB(0, 0, w, h, null, 0, w);
                int[] hd = Upscale.x2(px, w, h, name.hashCode(), name.startsWith("item/") ? Upscale.Style.ITEM : Upscale.Style.BLOCK);
                int y0 = i * (cell + 6);
                for (int y = 0; y < cell; y++)
                    for (int x = 0; x < cell; x++) {
                        sheet.setRGB(x, y0 + y, bg(px[(y * h / cell) * w + x * w / cell], x, y));
                        sheet.setRGB(cell + 12 + x, y0 + y, bg(hd[(y * 2 * h / cell) * (w * 2) + x * 2 * w / cell], x, y));
                    }
            }
        }
        ImageIO.write(sheet, "png", new File(a[1]));
    }

    /** See-through pixels over a dark checker. */
    static int bg(int argb, int x, int y) {
        if (argb >>> 24 > 0) return argb | 0xFF000000;
        return ((x / 8 + y / 8) % 2 == 0) ? 0xFF202020 : 0xFF2A2A2A;
    }
}

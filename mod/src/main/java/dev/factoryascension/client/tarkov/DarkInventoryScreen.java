package dev.factoryascension.client.tarkov;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * The game's own inventory (same slots, same layout, crafting, armor, recipe book), drawn in the Tarkov look: the world
 * blurred behind it, dark shaded panels, inset cells, the UI font and an amber accent. It still comes out of the duffel
 * bag (TarkovClient: you look down, the bag comes up, unzips, and the inventory grows out of it).
 */
public class DarkInventoryScreen extends InventoryScreen {
    private boolean opened = false;
    private final Player who;

    public DarkInventoryScreen(Player player) {
        super(player);
        this.who = player;
    }

    @Override
    protected void init() {
        super.init();
        if (!opened) {
            opened = true;
            TarkovClient.opened();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ARMOR_EQUIP_LEATHER, 0.8f, 0.7f));
        }
    }

    @Override
    public void removed() {
        super.removed();
        TarkovClient.closed();
        TarkovScreen.freeBlur();
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        float grow = TarkovClient.grown();
        TarkovScreen.blurWorld(grow);
        int a = (int) (0xB8 * grow), b = (int) (0xE8 * grow);
        g.fillGradient(0, 0, width, height / 2, b << 24 | 0x070808, a << 24 | 0x0E0F0F);
        g.fillGradient(0, height / 2, width, height, a << 24 | 0x0E0F0F, b << 24 | 0x060606);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        float grow = TarkovClient.grown();
        if (grow <= 0) return;   // still coming out of the bag
        renderBackground(g);
        g.pose().pushPose();
        if (grow < 1) {
            float s = 0.08f + 0.92f * grow, ox = width / 2f, oy = height * 0.82f;
            g.pose().translate(ox, oy, 0);
            g.pose().scale(s, s, 1);
            g.pose().translate(-ox, -oy, 0);
            mx = my = -10000;
        }
        super.render(g, mx, my, partial);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        // The panel: soft shadow, shaded fill, outline, a lit top edge and the amber bar
        TarkovScreen.shadowBox(g, x, y, w, h);
        g.fillGradient(x, y, x + w, y + h, 0xF01C1C1B, 0xF00B0B0B);
        g.renderOutline(x, y, w, h, TarkovScreen.EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, TarkovScreen.HILITE);
        g.fill(x + 1, y + 1, x + 3, y + 14, TarkovScreen.ACCENT);
        // Where you stand: a dark box with you in it
        int px = x + 26, py = y + 8;
        g.fill(px - 1, py - 1, px + 52, py + 71, TarkovScreen.EDGE);
        g.fillGradient(px, py, px + 51, py + 70, 0xFF101010, 0xFF060606);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 51, y + 75, 30, (float) (x + 51) - mx, (float) (y + 75 - 50) - my, who);
        // A cell under every slot
        for (Slot s : menu.slots) {
            int cx = x + s.x - 1, cy = y + s.y - 1;
            g.fill(cx, cy, cx + 18, cy + 18, TarkovScreen.CELL_LINE);
            g.fillGradient(cx + 1, cy + 1, cx + 17, cy + 17, 0xFF0B0B0B, 0xFF151514);
            g.fill(cx + 1, cy + 1, cx + 17, cy + 2, 0x60000000);
        }
        // Between the main inventory and the hotbar: a thin rule
        g.fill(x + 7, y + 139, x + w - 7, y + 140, TarkovScreen.EDGE);
        // Crafting arrow
        g.drawString(font, TarkovScreen.ui("→"), x + 136, y + 32, TarkovScreen.DIM, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, TarkovScreen.uiBold("CRAFTING"), 97, 6, TarkovScreen.TEXT, false);
        g.drawString(font, TarkovScreen.uiBold("INVENTORY"), 8, 73, TarkovScreen.DIM, false);
    }
}

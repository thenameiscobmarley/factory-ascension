package dev.factoryascension.client.tarkov;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/**
 * The game's own inventory (same slots, same layout, crafting, armor, recipe book), drawn in the Tarkov look: the world
 * blurred behind it, dark shaded panels, inset cells, the UI font and an amber accent. It still comes out of the duffel
 * bag (TarkovClient: you look down, the bag comes up, unzips, and the inventory grows out of it).
 */
public class DarkInventoryScreen extends InventoryScreen {
    private boolean opened = false;
    /** Drag and drop: the slot the item was picked up from by a press (null when not dragging one). */
    @org.jetbrains.annotations.Nullable private Slot dragFrom;
    private int dragButton;
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

    // ---- Drag and drop: press on an item, drag it, let go over a slot: it lands there (swapping if the slot is taken) ----
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Slot s = findSlot(mx, my);
        boolean emptyHand = menu.getCarried().isEmpty();
        boolean r = super.mouseClicked(mx, my, button);
        // Only a plain press that picked something up starts a drag (shift-clicks, number keys, creative act as usual)
        dragFrom = emptyHand && s != null && !hasShiftDown() && !menu.getCarried().isEmpty() && (button == 0 || button == 1) ? s : null;
        dragButton = button;
        return r;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        Slot from = dragFrom;
        dragFrom = null;
        if (from != null && button == dragButton && !menu.getCarried().isEmpty()) {
            Slot to = findSlot(mx, my);
            if (to != null && to != from && to.mayPlace(menu.getCarried())) {
                // Drop it on the target (places, merges, or swaps: the target's item comes up into the hand)...
                slotClicked(to, to.index, 0, ClickType.PICKUP);
                // ...and a swapped-out item goes back where the dragged one came from
                if (!menu.getCarried().isEmpty() && !from.hasItem() && from.mayPlace(menu.getCarried())) slotClicked(from, from.index, 0, ClickType.PICKUP);
                return true;
            }
        }
        return super.mouseReleased(mx, my, button);
    }

    @org.jetbrains.annotations.Nullable
    private Slot findSlot(double mx, double my) {
        for (Slot s : menu.slots) {
            if (s.isActive() && mx >= leftPos + s.x - 1 && mx < leftPos + s.x + 17 && my >= topPos + s.y - 1 && my < topPos + s.y + 17) return s;
        }
        return null;
    }

    /** Where a dragged item will land: an amber outline on the slot under the mouse. */
    private void drawDropTarget(GuiGraphics g, int mx, int my) {
        if (dragFrom == null || menu.getCarried().isEmpty()) return;
        Slot to = findSlot(mx, my);
        if (to == null || to == dragFrom) return;
        int x = leftPos + to.x - 1, y = topPos + to.y - 1;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.renderOutline(x, y, 18, 18, to.mayPlace(menu.getCarried()) ? TarkovScreen.ACCENT : 0xFFC8452E);
        g.renderOutline(x - 1, y - 1, 20, 20, 0x80000000 | (TarkovScreen.ACCENT & 0xFFFFFF));
        g.pose().popPose();
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
        drawDropTarget(g, mx, my);
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

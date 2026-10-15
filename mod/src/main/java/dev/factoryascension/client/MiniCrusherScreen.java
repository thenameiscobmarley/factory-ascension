package dev.factoryascension.client;

import dev.factoryascension.menu.MiniCrusherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Mini Crusher: motor and battery slots with a charge bar, the input, a progress arrow, four outputs. */
public class MiniCrusherScreen extends AbstractContainerScreen<MiniCrusherMenu> {
    public MiniCrusherScreen(MiniCrusherMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        FAGui.panel(g, x, y, imageWidth, imageHeight);
        for (int[] p : MiniCrusherMenu.POS) FAGui.slot(g, x + p[0], y + p[1]);
        // Charge bar under the battery, progress arrow between input and outputs
        int max = Math.max(1, menu.chargeMax());
        FAGui.bar(g, x + 38, y + 54, 4, 12, 0, 0);
        int h = Math.round(12f * menu.charge() / max);
        if (menu.chargeMax() > 0) g.fill(x + 38, y + 66 - h, x + 42, y + 66, 0xFF40E8D4);
        double frac = menu.needed() > 0 ? menu.progress() / (double) menu.needed() : 0;
        FAGui.bar(g, x + 84, y + 40, 26, 5, frac, FAGui.GOLD);
        FAGui.slots(g, x + 8, y + 84, 9, 3);
        FAGui.slots(g, x + 8, y + 142, 9, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        g.drawString(font, Component.translatable("gui.factoryascension.crusher_motor"), 36, 29, FAGui.TEXT_DIM, false);
        String speed = menu.speed() > 0 ? "x" + (menu.speed() % 1 == 0 ? String.valueOf((int) menu.speed()) : String.valueOf(menu.speed())) : "-";
        g.drawString(font, speed, 36, 39, menu.speed() > 0 ? FAGui.TEAL : 0xFFFF6060, false);
        g.drawString(font, menu.chargeMax() > 0 ? menu.charge() + "" : Component.translatable("gui.factoryascension.crusher_no_battery").getString(), 46, 56,
            menu.charge() > 0 ? FAGui.TEXT : 0xFFFF6060, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

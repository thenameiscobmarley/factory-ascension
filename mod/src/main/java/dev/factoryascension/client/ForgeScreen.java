package dev.factoryascension.client;

import dev.factoryascension.menu.ForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Essence Forge: gear slot, Liquid Essence gauge, and what it's doing (levels a second, levels so far). */
public class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    public ForgeScreen(ForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 93;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        int x = leftPos + 150, y = topPos + 20;
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 68) {
            g.renderTooltip(font, Component.translatable("gui.factoryascension.tank", menu.fluid(), menu.capacity()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        FAGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        FAGui.slot(g, leftPos + 80, topPos + 52);
        // Glowing ring around the gear slot while it works
        if (menu.levelsLastSecond() > 0) {
            int pulse = (int) (System.currentTimeMillis() / 120 % 8);
            g.fill(leftPos + 77, topPos + 49, leftPos + 99, topPos + 50, FAGui.TEAL);
            g.fill(leftPos + 77, topPos + 70, leftPos + 99, topPos + 71, FAGui.TEAL);
            g.fill(leftPos + 77 + pulse * 2, topPos + 47, leftPos + 81 + pulse * 2, topPos + 48, FAGui.VIOLET);
        }
        // Tank gauge
        int x = leftPos + 150, y = topPos + 20, h = 68;
        g.fill(x - 1, y - 1, x + 17, y + h + 1, FAGui.GOLD);
        g.fill(x, y, x + 16, y + h, FAGui.STEEL_DARK);
        int fill = (int) Math.round(h * Math.min(1.0, menu.fluid() / (double) menu.capacity()));
        if (fill > 0) {
            g.fill(x, y + h - fill, x + 16, y + h, 0xFF9B4DFF);
            g.fill(x, y + h - fill, x + 16, y + h - fill + 1, 0xFFD9B8FF);
        }
        FAGui.slots(g, leftPos + 8, topPos + 104, 9, 3);
        FAGui.slots(g, leftPos + 8, topPos + 162, 9, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        Component status = menu.levelsLastSecond() > 0
            ? Component.translatable("gui.factoryascension.forge_working", menu.levelsLastSecond())
            : Component.translatable("gui.factoryascension.forge_idle");
        g.drawString(font, status, 8, 22, menu.levelsLastSecond() > 0 ? FAGui.TEAL : FAGui.TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.factoryascension.forge_total", menu.totalLevels()), 8, 34, FAGui.TEXT_DIM, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

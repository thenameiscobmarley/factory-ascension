package dev.factoryascension.client;

import dev.factoryascension.menu.SmelterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Super Smelter: inputs, a flame and batch progress in the middle, outputs; speed, lanes and tier below. */
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
    private static final String[] TIERS = {"I", "II", "III", "IV"};

    public SmelterScreen(SmelterMenu menu, Inventory inv, Component title) {
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
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        FAGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        FAGui.slots(g, leftPos + 8, topPos + 22, 3, 3);
        FAGui.slots(g, leftPos + 116, topPos + 22, 3, 3);
        // Flame: taller the faster it spins
        int h = menu.rpm() <= 0 ? 0 : Math.min(18, 4 + menu.rpm() / 16);
        int fx = leftPos + 80, fy = topPos + 58;
        for (int i = 0; i < h; i++) {
            int w = Math.max(2, 8 - i / 3);
            int col = i < h / 3 ? 0xFFFF9020 : i < 2 * h / 3 ? 0xFFFFC040 : 0xFF40E8D4;
            g.fill(fx + 8 - w, fy - i, fx + 8 + w, fy - i + 1, col);
        }
        FAGui.bar(g, leftPos + 66, topPos + 66, 44, 5, menu.progress(), FAGui.GOLD);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        String tier = TIERS[Math.min(TIERS.length - 1, Math.max(0, menu.tier()))];
        g.drawString(font, Component.translatable("gui.factoryascension.smelter_tier", tier, menu.lanes()), 104, 5, FAGui.TEXT_DIM, false);
        Component line = menu.overstressed() ? Component.translatable("gui.factoryascension.smelter_stressed")
            : menu.rpm() <= 0 ? Component.translatable("gui.factoryascension.smelter_idle")
            : Component.translatable("gui.factoryascension.smelter_rpm", menu.rpm(), menu.smelted());
        g.drawString(font, line, 8, 80, menu.overstressed() ? 0xFFFF6060 : menu.rpm() > 0 ? FAGui.TEAL : FAGui.TEXT_DIM, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

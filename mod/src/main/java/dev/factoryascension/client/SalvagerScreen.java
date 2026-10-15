package dev.factoryascension.client;

import dev.factoryascension.menu.SalvagerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Salvager: loot row on top, cores row below, a grinder arrow between, and the Essence bank. */
public class SalvagerScreen extends AbstractContainerScreen<SalvagerMenu> {
    public SalvagerScreen(SalvagerMenu menu, Inventory inv, Component title) {
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
        FAGui.slots(g, leftPos + 8, topPos + 24, 9, 1);
        FAGui.slots(g, leftPos + 8, topPos + 66, 9, 1);
        // Grinder teeth between the rows, moving while there's loot to grind
        int shift = (int) (System.currentTimeMillis() / 150 % 6);
        for (int x = 8; x < 168; x += 6) {
            int col = (x / 6 + shift) % 2 == 0 ? FAGui.GOLD : FAGui.GOLD_DARK;
            g.fill(leftPos + x, topPos + 46, leftPos + x + 4, topPos + 48, col);
        }
        FAGui.bar(g, leftPos + 8, topPos + 52, 160, 4, menu.bank() - Math.floor(menu.bank()), FAGui.VIOLET);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        g.drawString(font, Component.translatable("gui.factoryascension.salvager_loot"), 110, 5, FAGui.TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.factoryascension.salvager_total", menu.salvaged(), menu.earned()), 8, 86, FAGui.TEAL, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

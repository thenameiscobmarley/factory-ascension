package dev.factoryascension.client;

import dev.factoryascension.blockentity.BinderBlockEntity;
import dev.factoryascension.menu.BinderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Card Binder: 54 card slots and a collection bar (different cards inside / every tome in the pack). */
public class BinderScreen extends AbstractContainerScreen<BinderMenu> {
    private final int total = Math.max(1, BinderBlockEntity.totalCards());

    public BinderScreen(BinderMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 232;
        inventoryLabelY = 139;
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
        FAGui.bar(g, leftPos + 8, topPos + 21, 160, 6, menu.uniqueCards() / (double) total, FAGui.VIOLET);
        FAGui.slots(g, leftPos + 8, topPos + 32, 9, 6);
        FAGui.slots(g, leftPos + 8, topPos + 150, 9, 3);
        FAGui.slots(g, leftPos + 8, topPos + 208, 9, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        String c = Component.translatable("gui.factoryascension.collection", menu.uniqueCards(), total).getString();
        g.drawString(font, c, imageWidth - 8 - font.width(c), 5, FAGui.TEXT_DIM, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

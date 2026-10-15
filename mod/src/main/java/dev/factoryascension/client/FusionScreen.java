package dev.factoryascension.client;

import dev.factoryascension.blockentity.FusionBlockEntity;
import dev.factoryascension.menu.FusionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Tome Fusion Table: tomes and a core on the left, the result on the right with a ghost preview, and a Fuse button. */
public class FusionScreen extends AbstractContainerScreen<FusionMenu> {
    private static final int BX = 92, BY = 62, BW = 42, BH = 16;

    public FusionScreen(FusionMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = 93;
    }

    private boolean canFuse() {
        FusionBlockEntity t = menu.table();
        return t != null && t.canFuse();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        FusionBlockEntity t = menu.table();
        int[] r = FusionMenu.POS[4];
        if (t != null && !t.preview().isEmpty() && t.items().getStackInSlot(FusionBlockEntity.RESULT).isEmpty()) {
            ItemStack p = t.preview();
            g.renderItem(p, leftPos + r[0], topPos + r[1]);
            g.fill(leftPos + r[0], topPos + r[1], leftPos + r[0] + 16, topPos + r[1] + 16, 0x80262A35);   // ghosted
            if (mouseX >= leftPos + r[0] && mouseX < leftPos + r[0] + 16 && mouseY >= topPos + r[1] && mouseY < topPos + r[1] + 16)
                g.renderTooltip(font, p, mouseX, mouseY);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        FAGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < 4; i++) FAGui.slot(g, leftPos + FusionMenu.POS[i][0], topPos + FusionMenu.POS[i][1]);
        int[] r = FusionMenu.POS[4];
        g.fill(leftPos + r[0] - 4, topPos + r[1] - 4, leftPos + r[0] + 20, topPos + r[1] + 20, canFuse() ? FAGui.VIOLET : FAGui.STEEL_DARK);
        FAGui.slot(g, leftPos + r[0], topPos + r[1]);
        // Arrow from the tomes to the result
        int glow = canFuse() ? FAGui.TEAL : FAGui.STEEL_LIGHT;
        g.fill(leftPos + 84, topPos + 47, leftPos + 124, topPos + 49, glow);
        g.fill(leftPos + 122, topPos + 44, leftPos + 124, topPos + 52, glow);
        // Fuse button
        boolean hover = mouseX >= leftPos + BX && mouseX < leftPos + BX + BW && mouseY >= topPos + BY && mouseY < topPos + BY + BH;
        int x = leftPos + BX, y = topPos + BY;
        g.fill(x, y, x + BW, y + BH, canFuse() ? FAGui.GOLD : FAGui.STEEL_DARK);
        g.fill(x + 1, y + 1, x + BW - 1, y + BH - 1, canFuse() ? (hover ? 0xFF6A4FA0 : 0xFF4A3A78) : FAGui.STEEL);
        g.drawCenteredString(font, Component.translatable("gui.factoryascension.fuse"), x + BW / 2, y + 4, canFuse() ? FAGui.TEXT : FAGui.TEXT_DIM);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        FusionBlockEntity t = menu.table();
        String status = t != null ? t.status() : "";
        if (status.isEmpty()) status = Component.translatable("gui.factoryascension.fusion_help").getString();
        int y = 82;
        for (var line : font.split(Component.literal(status), 160)) {
            g.drawString(font, line, 8, y, canFuse() ? FAGui.TEAL : FAGui.TEXT_DIM, false);
            y += 9;
            if (y > 90) break;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && canFuse() && mx >= leftPos + BX && mx < leftPos + BX + BW && my >= topPos + BY && my < topPos + BY + BH) {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}

package dev.factoryascension.client;

import dev.factoryascension.menu.VaultMenu;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** Ascension Vault: search box, Sort button, 6 rows of the vault with a scroll bar, and the player's inventory. */
public class VaultScreen extends AbstractContainerScreen<VaultMenu> {
    private static final int BAR_X = 174, BAR_Y = 32, BAR_H = VaultMenu.ROWS * 18 - 2;
    private EditBox search;
    private String lastSent = "";

    public VaultScreen(VaultMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 192;
        imageHeight = 232;
        inventoryLabelY = 139;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 8, topPos + 17, 112, 12, Component.translatable("gui.factoryascension.search"));
        search.setHint(Component.translatable("gui.factoryascension.search"));
        search.setMaxLength(50);
        search.setResponder(text -> {
            if (!text.equals(lastSent)) {
                lastSent = text;
                FANetwork.CHANNEL.sendToServer(new FANetwork.VaultSearch(menu.containerId, text));
            }
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("gui.factoryascension.sort"), b -> press(VaultMenu.BTN_SORT))
            .bounds(leftPos + 124, topPos + 15, 44, 16).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
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
        FAGui.slots(g, leftPos + 8, topPos + 32, 9, VaultMenu.ROWS);
        FAGui.slots(g, leftPos + 8, topPos + 150, 9, 3);
        FAGui.slots(g, leftPos + 8, topPos + 208, 9, 1);
        // Scroll bar
        int x = leftPos + BAR_X, y = topPos + BAR_Y;
        g.fill(x, y, x + 12, y + BAR_H, FAGui.STEEL_DARK);
        int rows = Math.max(menu.totalRows(), VaultMenu.ROWS);
        int thumbH = Math.max(10, BAR_H * VaultMenu.ROWS / rows);
        int maxRow = Math.max(1, rows - VaultMenu.ROWS);
        int thumbY = y + (BAR_H - thumbH) * Math.min(menu.firstRow(), maxRow) / maxRow;
        g.fill(x + 1, thumbY, x + 11, thumbY + thumbH, FAGui.GOLD);
        g.fill(x + 3, thumbY + 2, x + 9, thumbY + thumbH - 2, FAGui.TEAL);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        String space = menu.used() + " / " + menu.capacity();
        g.drawString(font, space, imageWidth - 8 - font.width(space), 5, FAGui.TEXT_DIM, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) press(VaultMenu.BTN_UP);
        else if (delta < 0) press(VaultMenu.BTN_DOWN);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos + BAR_X, y = topPos + BAR_Y;
        if (mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + BAR_H) {
            int rows = Math.max(menu.totalRows(), VaultMenu.ROWS);
            int row = (int) ((mouseY - y) / BAR_H * (rows - VaultMenu.ROWS + 1));
            press(VaultMenu.BTN_ROW + Math.max(0, row));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // While typing a search, keys go to the box (so E doesn't close the screen)
        if (search.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) return search.keyPressed(key, scan, mods) || true;
        return super.keyPressed(key, scan, mods);
    }
}

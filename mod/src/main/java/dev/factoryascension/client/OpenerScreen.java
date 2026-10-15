package dev.factoryascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.factoryascension.blockentity.OpenerBlockEntity;
import dev.factoryascension.menu.OpenerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Pack Opener: the pack slot, the latest pack's cards flipping over one by one, and the 27 output slots. */
public class OpenerScreen extends AbstractContainerScreen<OpenerMenu> {
    private static final int CARD_W = 24, CARD_H = 32, CARDS_X = 40, CARDS_Y = 20, STEP_MS = 220, FLIP_MS = 260;

    public OpenerScreen(OpenerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 226;
        inventoryLabelY = 133;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderCards(g, mouseX, mouseY);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        FAGui.panel(g, leftPos, topPos, imageWidth, imageHeight);
        FAGui.slot(g, leftPos + 8, topPos + 54);
        FAGui.slots(g, leftPos + 8, topPos + 80, 9, 3);
        FAGui.slots(g, leftPos + 8, topPos + 144, 9, 3);
        FAGui.slots(g, leftPos + 8, topPos + 202, 9, 1);
    }

    /** Each card starts face down and flips over in turn (squash to a line, then open up showing the tome). */
    private void renderCards(GuiGraphics g, int mouseX, int mouseY) {
        OpenerBlockEntity opener = menu.opener();
        if (opener == null) return;
        List<ItemStack> cards = opener.recent();
        long now = System.currentTimeMillis();
        for (int i = 0; i < cards.size(); i++) {
            int x = leftPos + CARDS_X + i * (CARD_W + 3), y = topPos + CARDS_Y;
            double t = (now - opener.revealedAt - i * (long) STEP_MS) / (double) FLIP_MS;
            boolean front = t >= 0.5;
            double squash = t <= 0 ? 1 : t >= 1 ? 1 : Math.abs(Math.cos(Math.PI * t));
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.translate(x + CARD_W / 2.0, 0, 0);
            pose.scale((float) Math.max(0.05, squash), 1, 1);
            pose.translate(-(x + CARD_W / 2.0), 0, 0);
            if (!front) {
                g.fill(x, y, x + CARD_W, y + CARD_H, FAGui.GOLD);
                g.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, 0xFF2A1E44);
                g.fill(x + 4, y + 4, x + CARD_W - 4, y + CARD_H - 4, FAGui.VIOLET);
                g.fill(x + 8, y + 12, x + CARD_W - 8, y + CARD_H - 12, FAGui.TEAL);
            } else {
                g.fill(x, y, x + CARD_W, y + CARD_H, FAGui.GOLD);
                g.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, 0xFFF4F0E4);
                g.renderItem(cards.get(i), x + 4, y + 8);
            }
            pose.popPose();
            if (front && mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H) {
                g.renderTooltip(font, cards.get(i), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        g.drawString(font, Component.translatable("gui.factoryascension.packs"), 8, 44, FAGui.TEXT_DIM, false);
        OpenerBlockEntity opener = menu.opener();
        String n = Component.translatable("gui.factoryascension.opened", opener == null ? 0 : opener.opened()).getString();
        g.drawString(font, n, imageWidth - 8 - font.width(n), 5, FAGui.TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.factoryascension.cards"), 8, 70, FAGui.TEXT_DIM, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

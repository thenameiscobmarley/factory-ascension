package dev.factoryascension.client;

import dev.factoryascension.FAConfig;
import dev.factoryascension.client.tarkov.TarkovScreen;
import dev.factoryascension.drugs.DrugBlocks;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Chem Lab: every recipe it knows, one row each: what goes in (with how many you have, green when it's enough),
 * what comes out, and MIX (shift: as many as you can). Hover anything for its tooltip.
 */
public class ChemLabScreen extends Screen {
    final BlockPos pos;
    private double scroll = 0;
    private final List<int[]> buttons = new ArrayList<>();   // x, y, w, h, recipe

    ChemLabScreen(BlockPos pos) {
        super(Component.literal("Chem Lab"));
        this.pos = pos;
    }

    public static void open(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ChemLabScreen(pos));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        var p = minecraft.player;
        if (p == null || p.distanceToSqr(pos.getCenter()) > 64) onClose();
    }

    int have(Item it) {
        int n = 0;
        for (ItemStack s : minecraft.player.getInventory().items) if (s.is(it)) n += s.getCount();
        return n;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int w = Math.min(width - 24, 400), h = Math.min(height - 24, 260);
        int x0 = (width - w) / 2, y0 = (height - h) / 2;
        TarkovScreen.shadowBox(g, x0, y0, w, h);
        g.fillGradient(x0, y0, x0 + w, y0 + h, 0xF01E1E1D, 0xF0101010);
        g.renderItem(new ItemStack(DrugBlocks.CHEM_LAB_ITEM.get()), x0 + 10, y0 + 8);
        g.drawString(font, TarkovScreen.uiBold("CHEM LAB"), x0 + 32, y0 + 9, 0xFFF2EFE6, false);
        g.drawString(font, TarkovScreen.ui("Shift-click MIX to make as many as you can"), x0 + 32, y0 + 20, 0xFF8C8A84, false);
        int top = y0 + 36, bottom = y0 + h - 8, rowH = 26;
        buttons.clear();
        ItemStack hover = ItemStack.EMPTY;
        g.enableScissor(x0 + 4, top, x0 + w - 4, bottom);
        int y = top - (int) scroll;
        boolean creative = minecraft.player.isCreative();
        for (int i = 0; i < FAConfig.chemRecipes.size(); i++, y += rowH + 3) {
            if (y + rowH < top || y > bottom) continue;
            FAConfig.ChemRecipe r = FAConfig.chemRecipes.get(i);
            Item out = DrugBlocks.ChemLab.item(r.output());
            if (out == null) continue;
            boolean can = true;
            g.fill(x0 + 8, y, x0 + w - 8, y + rowH, 0x1CFFFFFF);
            int x = x0 + 14;
            for (var in : r.inputs()) {
                Item it = DrugBlocks.ChemLab.item(in.getKey());
                if (it == null) continue;
                int have = have(it);
                boolean ok = creative || have >= in.getValue();
                can &= ok;
                ItemStack st = new ItemStack(it);
                g.renderItem(st, x, y + 5);
                if (mx >= x && mx < x + 16 && my >= y + 5 && my < y + 21 && my >= top && my < bottom) hover = st;
                String t = have + "/" + in.getValue();
                g.drawString(font, TarkovScreen.ui(t), x + 18, y + 9, ok ? 0xFF8FD46A : 0xFFE0604A, false);
                x += 22 + font.width(t) + 8;
            }
            g.drawString(font, TarkovScreen.ui("→"), x, y + 9, 0xFF8C8A84, false);
            x += 14;
            ItemStack o = new ItemStack(out, r.count());
            g.renderItem(o, x, y + 5);
            g.renderItemDecorations(font, o, x, y + 5);
            if (mx >= x && mx < x + 16 && my >= y + 5 && my < y + 21 && my >= top && my < bottom) hover = o;
            g.drawString(font, TarkovScreen.ui(o.getHoverName().getString()), x + 20, y + 9, 0xFFE6E3DA, false);
            int bw = 46, bx = x0 + w - 14 - bw, by = y + 5;
            boolean on = can && mx >= bx && mx < bx + bw && my >= by && my < by + 16;
            g.fill(bx, by, bx + bw, by + 16, can ? (on ? 0xFF3C5A30 : 0xFF2C4424) : 0xFF2A2020);
            g.renderOutline(bx, by, bw, 16, can ? 0xFF6FB04A : 0xFF7A3A30);
            g.drawCenteredString(font, TarkovScreen.uiBold("MIX"), bx + bw / 2, by + 4, can ? 0xFFF2EFE6 : 0xFF6A6864);
            if (can) buttons.add(new int[] {bx, by, bw, 16, i});
        }
        if (FAConfig.chemRecipes.isEmpty()) g.drawString(font, TarkovScreen.ui("No recipes (config: drugs.lab)"), x0 + 14, top + 6, 0xFF8C8A84, false);
        g.disableScissor();
        super.render(g, mx, my, partial);
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        int max = Math.max(0, FAConfig.chemRecipes.size() * 29 - (Math.min(height - 24, 260) - 44));
        scroll = Math.max(0, Math.min(max, scroll - d * 20));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int[] b : buttons) {
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.ChemMix(pos, b[4], hasShiftDown() ? 64 : 1));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}

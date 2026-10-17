package dev.factoryascension.client;

import dev.factoryascension.client.tarkov.TarkovScreen;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A trader (raid/Traders): their name and your standing with them, your wallet, and three tabs: BUY (their stock, the
 * locked ones greyed with the trust they need; barters show what they want), SELL (what you carry that they'd take, at
 * their price), TASKS (take a job, watch its progress, hand it in). The server sends a fresh view after every click.
 */
public class TraderScreen extends Screen {
    static CompoundTag view = new CompoundTag();
    public static int devTab = -1;
    private int tab = 0;
    private double scroll = 0;
    private final List<int[]> hits = new ArrayList<>();   // x, y, w, h, action kind, index
    private final List<int[]> tabs = new ArrayList<>();

    public TraderScreen() {
        super(Component.literal("Trader"));
    }

    public static void show(CompoundTag data) {
        view = data;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TraderScreen)) mc.setScreen(new TraderScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static String rubles(long n) {
        return String.format("%,d ₽", n).replace(',', ' ');
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        if (devTab >= 0) { tab = devTab; devTab = -1; }
        int w = Math.min(width - 24, 440), h = Math.min(height - 24, 280);
        int x0 = (width - w) / 2, y0 = (height - h) / 2;
        TarkovScreen.shadowBox(g, x0, y0, w, h);
        g.fillGradient(x0, y0, x0 + w, y0 + h, 0xF01E1E1D, 0xF0101010);
        g.fill(x0, y0, x0 + 3, y0 + h, TarkovScreen.ACCENT);
        g.drawString(font, TarkovScreen.uiBold(view.getString("Name")), x0 + 12, y0 + 9, 0xFFF2EFE6, false);
        g.drawString(font, TarkovScreen.ui(view.getString("About")), x0 + 12, y0 + 21, 0xFF8C8A84, false);
        String wallet = rubles(view.getLong("Rubles"));
        g.drawString(font, TarkovScreen.uiBold(wallet), x0 + w - 12 - font.width(TarkovScreen.uiBold(wallet)), y0 + 9, 0xFFE8C66A, false);
        String lv = "LOYALTY " + view.getInt("Level");
        g.drawString(font, TarkovScreen.ui(lv), x0 + w - 12 - font.width(TarkovScreen.ui(lv)), y0 + 21, 0xFF8FD46A, false);
        // Tabs
        tabs.clear();
        String[] names = {"BUY", "SELL", "TASKS"};
        int tx = x0 + 12;
        for (int i = 0; i < 3; i++) {
            int tw = font.width(TarkovScreen.uiBold(names[i])) + 16;
            g.fill(tx, y0 + 34, tx + tw, y0 + 48, i == tab ? 0x60C9A55A : 0x22FFFFFF);
            g.drawString(font, TarkovScreen.uiBold(names[i]), tx + 8, y0 + 37, i == tab ? 0xFFF2EFE6 : 0xFF8C8A84, false);
            tabs.add(new int[] {tx, y0 + 34, tw, 14, i});
            tx += tw + 4;
        }
        int top = y0 + 54, bottom = y0 + h - 8;
        hits.clear();
        g.enableScissor(x0 + 6, top, x0 + w - 6, bottom);
        int y = top - (int) scroll;
        int rowH = tab == 2 ? 34 : 22;
        ListTag list = view.getList(tab == 0 ? "Offers" : tab == 1 ? "Sell" : "Tasks", 10);
        for (int i = 0; i < list.size(); i++, y += rowH + 2) {
            if (y + rowH < top || y > bottom) continue;
            CompoundTag e = list.getCompound(i);
            boolean on = mx >= x0 + 10 && mx < x0 + w - 10 && my >= Math.max(top, y) && my < Math.min(bottom, y + rowH);
            g.fill(x0 + 10, y, x0 + w - 10, y + rowH, on ? 0x38FFFFFF : 0x1CFFFFFF);
            if (tab == 2) {
                int state = e.getInt("State"), prog = e.getInt("Prog"), count = e.getInt("Count");
                g.drawString(font, TarkovScreen.uiBold(e.getString("Title")), x0 + 16, y + 4, state == 2 ? 0xFF6A6864 : 0xFFF2EFE6, false);
                g.drawString(font, TarkovScreen.ui(e.getString("Desc") + "   (" + rubles(e.getInt("Reward")) + ")"), x0 + 16, y + 17, 0xFF8C8A84, false);
                String act = state == 0 ? "TAKE" : state == 2 ? "DONE" : prog >= count ? "HAND IN" : prog + " / " + count;
                int col = state == 0 ? TarkovScreen.ACCENT : state == 2 ? 0xFF6A6864 : prog >= count ? 0xFF8FD46A : 0xFFD6D4CC;
                g.drawString(font, TarkovScreen.uiBold(act), x0 + w - 16 - font.width(TarkovScreen.uiBold(act)), y + 11, col, false);
                if (state == 1 && count > 0) {
                    g.fill(x0 + 16, y + rowH - 3, x0 + w - 16, y + rowH - 1, 0xFF262626);
                    g.fill(x0 + 16, y + rowH - 3, x0 + 16 + (w - 32) * Math.min(prog, count) / count, y + rowH - 1, 0xFF8FD46A);
                }
                if (state == 0) hits.add(new int[] {x0 + 10, y, w - 20, rowH, FANetwork.TraderAction.TAKE, e.getInt("I")});
                else if (state == 1) hits.add(new int[] {x0 + 10, y, w - 20, rowH, FANetwork.TraderAction.HAND_IN, e.getInt("I")});
                continue;
            }
            ItemStack s = ItemStack.of(e.getCompound("Item"));
            g.renderItem(s, x0 + 13, y + 3);
            g.renderItemDecorations(font, s, x0 + 13, y + 3);
            boolean locked = tab == 0 && e.getInt("Lvl") > view.getInt("Level");
            g.drawString(font, TarkovScreen.ui(s.getHoverName().getString()), x0 + 34, y + 7, locked ? 0xFF6A6864 : 0xFFE6E3DA, false);
            String price;
            if (tab == 0) {
                String barter = e.getString("Barter");
                price = locked ? "LOYALTY " + e.getInt("Lvl") : (e.getInt("Price") > 0 ? rubles(e.getInt("Price")) : "") + (barter.isEmpty() ? "" : (e.getInt("Price") > 0 ? " + " : "") + barter);
            } else price = "+" + rubles(e.getInt("Price"));
            g.drawString(font, TarkovScreen.uiBold(price), x0 + w - 16 - font.width(TarkovScreen.uiBold(price)), y + 7, locked ? 0xFF6A6864 : tab == 1 ? 0xFF8FD46A : 0xFFE8C66A, false);
            if (!locked) hits.add(new int[] {x0 + 10, y, w - 20, rowH, tab == 0 ? FANetwork.TraderAction.BUY : FANetwork.TraderAction.SELL, tab == 0 ? e.getInt("I") : e.getInt("Slot")});
            if (on) {
                g.disableScissor();
                g.renderTooltip(font, s, mx, my);
                g.enableScissor(x0 + 6, top, x0 + w - 6, bottom);
            }
        }
        if (list.isEmpty()) g.drawString(font, TarkovScreen.ui(tab == 1 ? "Nothing you carry interests them." : "Nothing here."), x0 + 16, top + 6, 0xFF8C8A84, false);
        g.disableScissor();
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double d) {
        scroll = Math.max(0, scroll - d * 20);
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int[] t : tabs) if (mx >= t[0] && mx < t[0] + t[2] && my >= t[1] && my < t[1] + t[3]) {
            tab = t[4];
            scroll = 0;
            return true;
        }
        for (int[] r : hits) {
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                FANetwork.CHANNEL.sendToServer(new FANetwork.TraderAction(view.getInt("Id"), r[4], r[5]));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}

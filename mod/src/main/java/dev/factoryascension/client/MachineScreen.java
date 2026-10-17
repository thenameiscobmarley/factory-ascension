package dev.factoryascension.client;

import dev.factoryascension.client.tarkov.TarkovScreen;
import dev.factoryascension.network.FANetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A machine's panel (Quarry, Mob Grinder, Warp Hopper): its icon and name, and a row for everything about it (state,
 * speed, stress, output...), green when it's fine, red when something needs fixing. Live: refreshed every second.
 */
public class MachineScreen extends Screen {
    final BlockPos pos;
    String[] rows;
    private int age = 0;

    MachineScreen(BlockPos pos, String title, String[] rows) {
        super(Component.literal(title));
        this.pos = pos;
        this.rows = rows;
    }

    public static void show(BlockPos pos, String title, String[] rows) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MachineScreen m && m.pos.equals(pos)) m.rows = rows;
        else mc.setScreen(new MachineScreen(pos, title, rows));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        var p = minecraft.player;
        if (p == null || p.distanceToSqr(pos.getCenter()) > 100 || minecraft.level.getBlockState(pos).isAir()) { onClose(); return; }
        if (++age % 20 == 0) FANetwork.CHANNEL.sendToServer(new FANetwork.MachineAsk(pos));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int n = rows.length / 3;
        int w = Math.min(width - 32, 300), h = 44 + n * 16;
        int x0 = (width - w) / 2, y0 = (height - h) / 2;
        TarkovScreen.shadowBox(g, x0, y0, w, h);
        g.fillGradient(x0, y0, x0 + w, y0 + h, 0xF01E1E1D, 0xF0101010);
        g.fill(x0, y0, x0 + w, y0 + 1, 0x30FFFFFF);
        g.fill(x0, y0, x0 + 3, y0 + h, TarkovScreen.ACCENT);
        ItemStack icon = new ItemStack(minecraft.level.getBlockState(pos).getBlock());
        g.renderItem(icon, x0 + 10, y0 + 8);
        g.drawString(font, TarkovScreen.uiBold(title.getString().toUpperCase()), x0 + 32, y0 + 12, 0xFFF2EFE6, false);
        g.fill(x0 + 10, y0 + 30, x0 + w - 10, y0 + 31, 0x30FFFFFF);
        for (int i = 0; i < n; i++) {
            String label = rows[i * 3], value = rows[i * 3 + 1], mood = rows[i * 3 + 2];
            int y = y0 + 37 + i * 16;
            if (i % 2 == 0) g.fill(x0 + 8, y - 3, x0 + w - 8, y + 12, 0x12FFFFFF);
            g.drawString(font, TarkovScreen.ui(label.toUpperCase()), x0 + 14, y, 0xFF8C8A84, false);
            int col = mood.equals("ok") ? 0xFF8FD46A : mood.equals("bad") ? 0xFFE0604A : 0xFFE6E3DA;
            String v = value;
            while (font.width(v) > w - 110 && v.length() > 4) v = v.substring(0, v.length() - 2);
            if (!v.equals(value)) v = v + "…";
            g.drawString(font, TarkovScreen.ui(v), x0 + 100, y, col, false);
            if (mood.equals("ok") || mood.equals("bad")) g.fill(x0 + 92, y + 2, x0 + 96, y + 6, col);
        }
        super.render(g, mx, my, partial);
    }
}

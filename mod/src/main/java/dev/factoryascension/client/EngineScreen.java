package dev.factoryascension.client;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.menu.EngineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Modular Engine screens. The Core shows the whole engine (speed, power, modules, burners, the speed ladder); a Coil
 * shows how many coils are powered; a Gearbox the speed ladder; a Burner its fuel and flame. All say what to add next.
 */
public class EngineScreen extends AbstractContainerScreen<EngineMenu> {
    private static final int RED = 0xFFFF6060, GREEN = 0xFF7BE38F;

    public EngineScreen(EngineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = menu.kind == EngineMenu.BURNER ? 186 : 124;
        inventoryLabelY = 93;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    private ItemStack icon(int kind) {
        return new ItemStack(switch (kind) {
            case EngineMenu.COIL -> FactoryAscension.COIL.get();
            case EngineMenu.GEARBOX -> FactoryAscension.GEARBOX.get();
            case EngineMenu.BURNER -> FactoryAscension.BURNER.get();
            default -> FactoryAscension.ENGINE_CORE.get();
        });
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        FAGui.panel(g, x, y, imageWidth, imageHeight);
        g.renderItem(icon(menu.kind), x + imageWidth - 22, y + 2);
        if (menu.kind == EngineMenu.BURNER) {
            FAGui.slot(g, x + 80, y + 64);
            // Flame gauge: how much of the current fuel is left
            float f = menu.burnTotal() > 0 ? menu.burnTicks() / (float) menu.burnTotal() : 0;
            int h = Math.round(14 * f), fx = x + 82, fy = y + 46;
            g.fill(fx - 1, fy - 1, fx + 15, fy + 15, FAGui.STEEL_DARK);
            for (int i = 0; i < h; i++) {
                int w = Math.max(2, 7 - i / 2);
                g.fill(fx + 7 - w, fy + 13 - i, fx + 7 + w, fy + 14 - i, i < 5 ? 0xFFFF7020 : i < 10 ? 0xFFFFB040 : 0xFFFFE890);
            }
            FAGui.slots(g, x + 8, y + 104, 9, 3);
            FAGui.slots(g, x + 8, y + 162, 9, 1);
        }
        if (!menu.connected()) return;
        if (menu.kind == EngineMenu.CORE || menu.kind == EngineMenu.GEARBOX) ladder(g, x + 8, y + (menu.kind == EngineMenu.CORE ? 82 : 52));
        if (menu.kind == EngineMenu.CORE) {
            FAGui.bar(g, x + 8, y + 72, 160, 4, menu.modules() / (double) FAConfig.engineMaxModules, FAGui.TEAL);
            g.renderItem(icon(EngineMenu.COIL), x + 8, y + 48);
            g.renderItem(icon(EngineMenu.GEARBOX), x + 62, y + 48);
            g.renderItem(icon(EngineMenu.BURNER), x + 116, y + 48);
        }
        if (menu.kind == EngineMenu.COIL)
            FAGui.bar(g, x + 8, y + 58, 160, 6, menu.coils() > 0 ? menu.poweredCoils() / (double) menu.coils() : 0, 0xFFE89040);
    }

    /** The speed ladder: one box per speed, the engine's current one lit. */
    private void ladder(GuiGraphics g, int x, int y) {
        int n = FAConfig.engineRpm.length, w = 160 / n;
        int current = menu.rpm() > 0 ? Math.min(menu.gears(), n - 1) : -1;
        for (int i = 0; i < n; i++) {
            int bx = x + i * w;
            g.fill(bx, y, bx + w - 2, y + 12, i == current ? FAGui.GOLD : i < Math.min(menu.gears(), n - 1) ? FAGui.GOLD_DARK : FAGui.STEEL_DARK);
            String t = String.valueOf(FAConfig.engineRpm[i]);
            g.drawString(font, t, bx + (w - 2 - font.width(t)) / 2, y + 2, i == current ? 0xFF2A1608 : FAGui.TEXT_DIM, false);
        }
    }

    private void big(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(font, Component.literal(text).withStyle(net.minecraft.ChatFormatting.BOLD), x, y, color, false);
    }

    private Component status() {
        if (!menu.connected()) return Component.translatable("gui.factoryascension.engine_not_connected");
        if (menu.jammed()) return Component.translatable("gui.factoryascension.engine_jammed_short");
        if (menu.coils() == 0) return Component.translatable("gui.factoryascension.engine_need_coils");
        if (menu.lit() == 0) return Component.translatable("gui.factoryascension.engine_need_fuel");
        int needed = (menu.coils() + FAConfig.engineCoilsPerBurner - 1) / FAConfig.engineCoilsPerBurner;
        if (menu.burners() < needed) return Component.translatable("gui.factoryascension.engine_need_burners", needed - menu.burners());
        return Component.translatable("gui.factoryascension.engine_running");
    }

    private int statusColor() {
        if (!menu.connected() || menu.jammed()) return RED;
        return menu.rpm() > 0 && menu.poweredCoils() == menu.coils() ? GREEN : 0xFFFFD050;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        Component status = status();
        if (!menu.connected()) {
            for (var line : font.split(status, 160)) { g.drawString(font, line, 8, 24, RED, false); break; }
            int yy = 24;
            for (var line : font.split(Component.translatable("gui.factoryascension.engine_how"), 160)) g.drawString(font, line, 8, yy += 10, FAGui.TEXT_DIM, false);
            if (menu.kind == EngineMenu.BURNER) burnerLabels(g);
            return;
        }
        switch (menu.kind) {
            case EngineMenu.CORE -> {
                big(g, menu.rpm() + "", 8, 20, FAGui.TEAL);
                g.drawString(font, "RPM", 8 + font.width(menu.rpm() + "") + 4, 20, FAGui.TEXT_DIM, false);
                String su = String.format("%,d", menu.su());
                big(g, su, 168 - font.width(su) - 18, 20, FAGui.GOLD);
                g.drawString(font, "SU", 168 - 14, 27, FAGui.TEXT_DIM, false);
                g.drawString(font, "x" + menu.coils() + " (" + menu.poweredCoils() + " on)", 26, 52, FAGui.TEXT, false);
                g.drawString(font, "x" + menu.gears(), 80, 52, FAGui.TEXT, false);
                g.drawString(font, "x" + menu.burners() + " (" + menu.lit() + " lit)", 134, 52, FAGui.TEXT, false);
                g.drawString(font, Component.translatable("gui.factoryascension.engine_modules_bar", menu.modules(), FAConfig.engineMaxModules), 8, 64, FAGui.TEXT_DIM, false);
                g.drawString(font, status, 8, 100, statusColor(), false);
            }
            case EngineMenu.COIL -> {
                big(g, "+" + (int) FAConfig.engineSuPerCoil + " SU", 8, 20, FAGui.GOLD);
                g.drawString(font, Component.translatable("gui.factoryascension.coil_each"), 8, 40, FAGui.TEXT_DIM, false);
                g.drawString(font, Component.translatable("gui.factoryascension.coil_powered", menu.poweredCoils(), menu.coils()), 8, 68, FAGui.TEXT, false);
                g.drawString(font, Component.translatable("gui.factoryascension.engine_summary", menu.rpm(), String.format("%,d", menu.su())), 8, 82, FAGui.TEAL, false);
                g.drawString(font, status, 8, 100, statusColor(), false);
            }
            case EngineMenu.GEARBOX -> {
                big(g, menu.rpm() + " RPM", 8, 20, FAGui.TEAL);
                g.drawString(font, Component.translatable("gui.factoryascension.gearbox_count", menu.gears()), 8, 40, FAGui.TEXT_DIM, false);
                int n = FAConfig.engineRpm.length;
                Component next = menu.gears() < n - 1 ? Component.translatable("gui.factoryascension.gearbox_next", FAConfig.engineRpm[menu.gears() + 1])
                    : Component.translatable("gui.factoryascension.gearbox_top");
                g.drawString(font, next, 8, 70, FAGui.TEXT, false);
                g.drawString(font, Component.translatable("gui.factoryascension.gearbox_same_su", String.format("%,d", menu.su())), 8, 82, FAGui.TEXT_DIM, false);
                g.drawString(font, status, 8, 100, statusColor(), false);
            }
            default -> {
                burnerLabels(g);
                g.drawString(font, Component.translatable("gui.factoryascension.engine_summary", menu.rpm(), String.format("%,d", menu.su())), 8, 20, FAGui.TEAL, false);
                g.drawString(font, status, 8, 32, statusColor(), false);
            }
        }
    }

    private void burnerLabels(GuiGraphics g) {
        Component lit = menu.burnerLit() ? Component.translatable("gui.factoryascension.burner_lit") : Component.translatable("gui.factoryascension.burner_idle");
        g.drawString(font, lit, 104, 50, menu.burnerLit() ? 0xFFFFB040 : FAGui.TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.factoryascension.burner_left", menu.fuelSeconds() / 60, menu.fuelSeconds() % 60), 104, 66, FAGui.TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, FAGui.TEXT_DIM, false);
    }
}

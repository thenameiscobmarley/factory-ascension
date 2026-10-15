package dev.factoryascension.client;

import dev.factoryascension.FAConfig;
import dev.factoryascension.menu.WorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Ascension Workbench: gear on the left, the modifier under it, what Apply will do on the right, and the Legendary
 * row: ten stars (gold for each one the gear has), the next star's cost with green/red counts, and Star Up.
 */
public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private Button apply, star;

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 232;
        imageHeight = 232;
        inventoryLabelX = WorkbenchMenu.INV_X;
        inventoryLabelY = WorkbenchMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        apply = addRenderableWidget(Button.builder(Component.translatable("gui.factoryascension.bench_apply"), b -> press(WorkbenchMenu.BTN_APPLY))
            .bounds(leftPos + 52, topPos + 74, 54, 18).build());
        star = addRenderableWidget(Button.builder(Component.translatable("gui.factoryascension.bench_star"), b -> press(WorkbenchMenu.BTN_STAR))
            .bounds(leftPos + imageWidth - 70, topPos + 118, 62, 18).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private int stars() {
        ItemStack gear = menu.gear();
        CompoundTag tag = gear.getTag();
        return tag != null && tag.contains("FactoryAscension") ? tag.getCompound("FactoryAscension").getInt("legend") : 0;
    }

    private int count(String id) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        int n = 0;
        if (minecraft == null || minecraft.player == null || item == null) return 0;
        for (ItemStack s : minecraft.player.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        var bench = menu.bench();
        apply.active = bench != null && bench.canApply();
        int s = stars();
        boolean affordable = !menu.gear().isEmpty() && s < FAConfig.legendMaxStars;
        if (affordable && s < FAConfig.legendCosts.size())
            for (String[] c : FAConfig.legendCosts.get(s)) if (count(c[0]) < Integer.parseInt(c[1])) affordable = false;
        star.active = affordable;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        FAGui.panel(g, x, y, imageWidth, imageHeight);
        // Gear frame (gold), modifier slot, the arrow between them
        g.fill(x + WorkbenchMenu.GEAR_X - 4, y + WorkbenchMenu.GEAR_Y - 4, x + WorkbenchMenu.GEAR_X + 20, y + WorkbenchMenu.GEAR_Y + 20, FAGui.GOLD);
        FAGui.slot(g, x + WorkbenchMenu.GEAR_X, y + WorkbenchMenu.GEAR_Y);
        FAGui.slot(g, x + WorkbenchMenu.MOD_X, y + WorkbenchMenu.MOD_Y);
        for (int i = 0; i < 8; i++) g.fill(x + 33 - i / 2, y + 58 + i, x + 35 + i / 2, y + 59 + i, FAGui.TEAL);
        // The status box
        g.fill(x + 52, y + 22, x + imageWidth - 8, y + 70, FAGui.STEEL_DARK);
        // The Legendary row
        g.fill(x + 8, y + 98, x + imageWidth - 8, y + 140, 0xFF262A35);
        FAGui.slots(g, x + WorkbenchMenu.INV_X, y + WorkbenchMenu.INV_Y, 9, 3);
        FAGui.slots(g, x + WorkbenchMenu.INV_X, y + WorkbenchMenu.INV_Y + 58, 9, 1);
        // Big preview of the gear in the frame's corner
        ItemStack gear = menu.gear();
        if (!gear.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x + imageWidth - 44, y + 26, 0);
            g.pose().scale(2f, 2f, 1f);
            g.renderItem(gear, 0, 0);
            g.pose().popPose();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, FAGui.GOLD, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, FAGui.TEXT_DIM, false);
        var bench = menu.bench();
        String status = bench == null ? "" : bench.status();
        int yy = 26;
        List<FormattedCharSequence> lines = font.split(Component.literal(status), imageWidth - 52 - 8 - 44);
        for (int i = 0; i < Math.min(4, lines.size()); i++) {
            g.drawString(font, lines.get(i), 56, yy, bench != null && bench.canApply() ? FAGui.TEXT : 0xFFFF8080, false);
            yy += 10;
        }
        // Stars: ten across, gold for the ones the gear has
        int s = stars();
        String grade = s < FAConfig.legendGrades.size() ? FAConfig.legendGrades.get(s) : "";
        g.drawString(font, Component.translatable("gui.factoryascension.bench_legendary", grade, s, FAConfig.legendMaxStars), 12, 101, FAGui.GOLD, false);
        for (int i = 0; i < FAConfig.legendMaxStars; i++) {
            boolean on = i < s;
            g.drawString(font, on ? "★" : "☆", 12 + i * 11, 113, on ? (i >= 9 ? 0xFF6FFFF0 : i >= 6 ? 0xFFFF6060 : i >= 3 ? 0xFFE080FF : FAGui.GOLD) : 0xFF5B6274, false);
        }
        // Next star's cost: item icons with have / need
        if (s < FAConfig.legendMaxStars && s < FAConfig.legendCosts.size() && !menu.gear().isEmpty()) {
            int cx = 12;
            for (String[] c : FAConfig.legendCosts.get(s)) {
                var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(c[0]));
                if (item == null) continue;
                g.renderItem(new ItemStack(item), cx, 122);
                int need = Integer.parseInt(c[1]), have = count(c[0]);
                g.drawString(font, need + "", cx + 17, 127, have >= need ? 0xFF7BE38F : 0xFFFF6060, false);
                cx += 22 + font.width(need + "");
            }
        } else if (s >= FAConfig.legendMaxStars) {
            g.drawString(font, Component.translatable("gui.factoryascension.bench_max"), 12, 127, FAGui.TEAL, false);
        }
    }
}

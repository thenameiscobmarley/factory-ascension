package dev.factoryascension.vehicle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A dirt bike part: sneak + right-click a bike with it to fit it (the old one drops). */
public class BikePartItem extends Item {
    public final String kind, id;   // kind: engine, frame, gearbox

    public BikePartItem(String kind, String id, Properties p) {
        super(p.stacksTo(1));
        this.kind = kind;
        this.id = id;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        switch (kind) {
            case "engine" -> {
                var e = BikeParts.engine(id);
                tip.add(Component.literal(String.format("Top speed %.0f km/h, pull %.0f, %.0f kg, burns %.1f%%/min flat out",
                    e.top() * 72, e.power() * 1000, e.weight(), e.fuelUse() * 1200)).withStyle(ChatFormatting.GRAY));
                if (e.size() > 3) tip.add(Component.literal("Only fits a Long Frame").withStyle(ChatFormatting.GOLD));
            }
            case "frame" -> {
                var f = BikeParts.frame(id);
                tip.add(Component.literal(String.format("%.0f kg, grip x%.2f, steering x%.2f, takes engines up to size %d", f.weight(), f.grip(), f.turn(), f.maxEngine()))
                    .withStyle(ChatFormatting.GRAY));
            }
            default -> {
                var g = BikeParts.gearbox(id);
                tip.add(Component.literal(String.format("%d gears: top speed x%.2f, pull x%.2f", g.gears(), g.topMul(), g.accelMul())).withStyle(ChatFormatting.GRAY));
            }
        }
        tip.add(Component.literal("Sneak + right-click a dirt bike to fit it").withStyle(ChatFormatting.DARK_GRAY));
    }
}

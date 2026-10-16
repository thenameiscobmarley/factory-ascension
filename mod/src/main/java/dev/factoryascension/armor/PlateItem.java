package dev.factoryascension.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** One armor plate (armor/Plate). Its wear is its item damage; put it in a Plate Carrier. */
public class PlateItem extends Item {
    public final Plate plate;

    public PlateItem(Plate plate, Properties props) {
        super(props.durability(plate.durability));
        this.plate = plate;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(String.format("Class %.1f  %d/%d  %.1f kg", plate.protection, stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage(), plate.weight))
            .withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Hold a Plate Carrier, this in your other hand, right-click: it goes in").withStyle(ChatFormatting.DARK_GRAY));
    }
}

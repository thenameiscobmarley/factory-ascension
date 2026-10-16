package dev.factoryascension.npc;

import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A dog tag off someone you put down: their name, what they were, who killed them. */
public class DogTagItem extends Item {
    public DogTagItem(Properties props) {
        super(props);
    }

    public static ItemStack of(String name, OpType type, String killer, long when) {
        ItemStack s = new ItemStack(FactoryAscension.DOG_TAG.get());
        var t = s.getOrCreateTag();
        t.putString("Name", name);
        t.putString("Kind", type.name);
        t.putString("Faction", type.faction.name());
        if (!killer.isEmpty()) t.putString("Killer", killer);
        t.putLong("Day", when / 24000L + 1);
        return s;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("Name")) return Component.literal("Dog Tag: " + stack.getTag().getString("Name"));
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (!stack.hasTag()) return;
        var t = stack.getTag();
        tip.add(Component.literal(t.getString("Kind") + ", " + t.getString("Faction").toLowerCase()).withStyle(ChatFormatting.GRAY));
        if (t.contains("Killer")) tip.add(Component.literal("Killed by " + t.getString("Killer") + " on day " + t.getLong("Day")).withStyle(ChatFormatting.DARK_GRAY));
    }
}

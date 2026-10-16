package dev.factoryascension.vehicle;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A dirt bike, packed up: right-click the ground to put it down. Its parts, fuel and colour travel with it (NBT "Bike"). */
public class DirtBikeItem extends Item {
    public DirtBikeItem(Properties p) {
        super(p.stacksTo(1));
    }

    public static CompoundTag bike(ItemStack s) {
        CompoundTag t = s.getOrCreateTag().getCompound("Bike");
        if (!t.contains("Engine")) {
            t.putString("Engine", "engine_125");
            t.putString("Frame", "frame_standard");
            t.putString("Gearbox", "gearbox_4");
            t.putFloat("Fuel", 50);
            t.putFloat("FuelPower", 1);
            t.putFloat("FuelUse", 1);
            t.putInt("Colour", 0xE8661C);
        }
        return t;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        DirtBike b = new DirtBike(Vehicles.DIRT_BIKE.get(), level);
        var p = ctx.getClickLocation();
        b.moveTo(p.x, p.y + 0.1, p.z, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0, 0);
        b.readBike(bike(ctx.getItemInHand()));
        if (!level.noCollision(b, b.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(b);
        if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = bike(stack.copy());
        var s = BikeParts.stats(t.getString("Engine"), t.getString("Frame"), t.getString("Gearbox"), t.getFloat("FuelPower"), t.getFloat("FuelUse"));
        tip.add(Component.literal(BikeParts.engine(t.getString("Engine")).name() + ", " + BikeParts.frame(t.getString("Frame")).name() + ", "
            + BikeParts.gearbox(t.getString("Gearbox")).name()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(String.format("Top speed %.0f km/h, %.0f kg, fuel %.0f%%", s.top() * 72, s.mass() - 80, t.getFloat("Fuel"))).withStyle(ChatFormatting.GRAY));
    }
}

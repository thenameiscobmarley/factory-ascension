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

/** A quad bike or a pickup truck, packed up: right-click the ground to put it down (NBT "Car" travels with it). */
public class CarItem extends Item {
    final Car.Model model;

    public CarItem(Car.Model model, Properties p) {
        super(p.stacksTo(1));
        this.model = model;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Car c = new Car((model == Car.Model.QUAD ? Vehicles.QUAD : Vehicles.PICKUP).get(), level);
        var p = ctx.getClickLocation();
        c.moveTo(p.x, p.y + 0.1, p.z, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0, 0);
        CompoundTag t = ctx.getItemInHand().getOrCreateTag().getCompound("Car").copy();
        t.putInt("Model", model.ordinal());
        c.readCar(t);
        if (!level.noCollision(c, c.getBoundingBox())) return InteractionResult.FAIL;
        level.addFreshEntity(c);
        if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag t = stack.hasTag() ? stack.getTag().getCompound("Car") : new CompoundTag();
        tip.add(Component.literal(String.format("Top speed %.0f km/h, %.0f kg, %d seats", model.top * 72, model.mass, model.seats)).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(String.format("Fuel %.0f%%, condition %.0f%%", t.contains("Fuel") ? t.getFloat("Fuel") : 60f,
            100 * (1 - t.getFloat("Damage") / model.health))).withStyle(ChatFormatting.GRAY));
        if (model == Car.Model.PICKUP) tip.add(Component.literal("Mounted .50 BMG on the bed: " + t.getInt("Ammo") + " rounds").withStyle(ChatFormatting.GRAY));
    }
}

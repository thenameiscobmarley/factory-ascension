package dev.factoryascension.vehicle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * The bike workshop: right-click a dirt bike with a wrench and its garage screen opens (client/vehicle/WorkshopScreen):
 * the bike, what's fitted, how it rides, and every part you carry that would fit; click one to swap it in. A wrench
 * also repairs the quad and the truck.
 */
public final class Workshop {
    private Workshop() {
    }

    /** Set by the client: opens the screen. */
    public static Consumer<DirtBike> openClient = b -> { };

    public static class Wrench extends Item {
        public Wrench(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack s, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal("Right-click a dirt bike: the workshop (swap engine, frame, gearbox)").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Right-click the quad or the truck: repairs it").withStyle(ChatFormatting.GRAY));
        }
    }

    /** From the screen (FANetwork.VehicleAction): fit the part in inventory slot `slot` to bike `vehicle`. */
    public static void fit(ServerPlayer p, int vehicle, int slot) {
        if (!(p.level().getEntity(vehicle) instanceof DirtBike b) || b.distanceToSqr(p) > 49) return;
        if (!(p.getMainHandItem().getItem() instanceof Wrench) && !(p.getOffhandItem().getItem() instanceof Wrench)) return;
        var inv = p.getInventory();
        if (slot < 0 || slot >= inv.items.size()) return;
        ItemStack s = inv.items.get(slot);
        if (b.fitPart(p, s)) {
            ItemStack w = p.getMainHandItem().getItem() instanceof Wrench ? p.getMainHandItem() : p.getOffhandItem();
            w.hurtAndBreak(1, p, pl -> { });
        }
    }
}

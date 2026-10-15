package dev.factoryascension.menu;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.EngineModuleBlock;
import dev.factoryascension.blockentity.BurnerBlockEntity;
import dev.factoryascension.blockentity.EngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * The screen of any Modular Engine part: the Engine Core, a Coil, a Gearbox or a Burner. Each shows the whole engine
 * it belongs to (found through the modules it touches); the Burner also has its fuel slot and the player's inventory.
 */
public class EngineMenu extends AbstractContainerMenu {
    public static final int CORE = 0, COIL = 1, GEARBOX = 2, BURNER = 3;
    // Data: 0 connected, 1 jammed, 2 rpm, 3-4 SU (low/high 15 bits), 5 coils, 6 gearboxes, 7 burners, 8 lit burners,
    // 9 modules, 10 burn ticks left (this burner, /20 = seconds of the current fuel), 11 this burner's fuel total,
    // 12 seconds of fuel in this burner altogether, 13 this burner lit
    private static final int DATA = 14;
    public final int kind;
    private final BlockPos pos;
    private final Level level;
    private final ContainerData data;
    private BlockPos corePos = null;
    private int searchTimer = 0;

    public EngineMenu(int id, Inventory inv, BlockPos pos, int kind) {
        super(FactoryAscension.ENGINE_MENU.get(), id);
        this.kind = kind;
        this.pos = pos;
        this.level = inv.player.level();
        if (kind == BURNER) {
            ItemStackHandler fuel = level.getBlockEntity(pos) instanceof BurnerBlockEntity b ? b.fuel() : new ItemStackHandler(1);
            addSlot(new SlotItemHandler(fuel, 0, 80, 64));
            MenuUtil.addPlayerSlots(this::addSlot, inv, 8, 104);
        }
        data = level.isClientSide ? new SimpleContainerData(DATA) : new ContainerData() {
            public int get(int i) { return value(i); }
            public void set(int i, int v) { }
            public int getCount() { return DATA; }
        };
        addDataSlots(data);
    }

    /** The Engine Core this part belongs to: itself, or the first core found through touching modules. */
    private EngineBlockEntity core() {
        if (corePos != null && level.getBlockEntity(corePos) instanceof EngineBlockEntity e) return e;
        if (--searchTimer > 0) return null;
        searchTimer = 20;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(pos);
        seen.add(pos);
        while (!todo.isEmpty() && seen.size() <= FAConfig.engineMaxModules + 8) {
            BlockPos at = todo.poll();
            if (level.getBlockEntity(at) instanceof EngineBlockEntity e) {
                corePos = at;
                return e;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                Block b = level.getBlockState(n).getBlock();
                if (seen.add(n) && (b instanceof EngineModuleBlock || b == FactoryAscension.BURNER.get() || b == FactoryAscension.ENGINE_CORE.get())) todo.add(n);
            }
        }
        return null;
    }

    private int value(int i) {
        if (i >= 10) {
            if (!(level.getBlockEntity(pos) instanceof BurnerBlockEntity b)) return 0;
            return switch (i) {
                case 10 -> Math.min(32000, b.burnTicks());
                case 11 -> Math.min(32000, b.burnTotal());
                case 12 -> Math.min(32000, b.secondsLeft());
                default -> b.burning() ? 1 : 0;
            };
        }
        EngineBlockEntity e = core();
        if (e == null) return 0;
        int su = (int) Math.min(Integer.MAX_VALUE, e.su());
        return switch (i) {
            case 0 -> 1;
            case 1 -> e.jammed() ? 1 : 0;
            case 2 -> (int) e.rpm();
            case 3 -> su & 0x7FFF;
            case 4 -> (su >> 15) & 0x7FFF;
            case 5 -> e.coils();
            case 6 -> e.gears();
            case 7 -> e.burnerCount();
            case 8 -> e.litBurners();
            default -> e.modules();
        };
    }

    public boolean connected() { return data.get(0) != 0; }
    public boolean jammed() { return data.get(1) != 0; }
    public int rpm() { return data.get(2); }
    public int su() { return data.get(3) | (data.get(4) << 15); }
    public int coils() { return data.get(5); }
    public int gears() { return data.get(6); }
    public int burners() { return data.get(7); }
    public int lit() { return data.get(8); }
    public int modules() { return data.get(9); }
    public int burnTicks() { return data.get(10); }
    public int burnTotal() { return data.get(11); }
    public int fuelSeconds() { return data.get(12); }
    public boolean burnerLit() { return data.get(13) != 0; }
    /** Coils that have a lit Burner behind them. */
    public int poweredCoils() { return Math.min(coils(), lit() * FAConfig.engineCoilsPerBurner); }

    /** Opens the engine screen of the part at pos for a player. */
    public static void open(net.minecraft.server.level.ServerPlayer player, BlockPos pos, int kind, net.minecraft.network.chat.Component title) {
        net.minecraftforge.network.NetworkHooks.openScreen(player, new net.minecraft.world.SimpleMenuProvider((id, inv, p) -> new EngineMenu(id, inv, pos, kind), title),
            buf -> { buf.writeBlockPos(pos); buf.writeVarInt(kind); });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (kind != BURNER) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (BurnerBlockEntity.burnTime(stack) <= 0 || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        Block b = level.getBlockState(pos).getBlock();
        boolean part = b instanceof EngineModuleBlock || b == FactoryAscension.BURNER.get() || b == FactoryAscension.ENGINE_CORE.get();
        return part && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}

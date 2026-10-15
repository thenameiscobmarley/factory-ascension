package dev.factoryascension.blockentity;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.block.MachineBlock;
import dev.factoryascension.menu.MiniCrusherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

/**
 * Mini Crusher: one input, four outputs, a motor slot and a battery slot. With a motor and battery charge it runs the
 * input through Create's crushing recipe (or milling, if there's no crushing one), at the motor's speed, using battery
 * charge per item. Hoppers and funnels put input in from any side (batteries go to the battery slot) and take results
 * out of the outputs.
 */
public class MiniCrusherBlockEntity extends SyncedBlockEntity implements MenuProvider, MachineBlock.DropsContents, MachineBlock.Ticking {
    public static final int INPUT = 0, OUT0 = 1, OUTS = 4, MOTOR = 5, BATTERY = 6, SLOTS = 7;
    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == MOTOR) return isMotor(stack);
            if (slot == BATTERY) return isBattery(stack);
            if (slot == INPUT) return !isMotor(stack) && !isBattery(stack);
            return true;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == MOTOR || slot == BATTERY ? 1 : 64;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int progress = 0, needed = 0;
    private ProcessingRecipe<?> recipe;
    private ItemStack recipeFor = ItemStack.EMPTY;

    private final ContainerData data = new ContainerData() {
        public int get(int i) {
            ItemStack b = items.getStackInSlot(BATTERY);
            return switch (i) {
                case 0 -> progress;
                case 1 -> needed;
                case 2 -> isBattery(b) ? Math.min(32000, b.getMaxDamage() - b.getDamageValue()) : 0;
                case 3 -> isBattery(b) ? Math.min(32000, b.getMaxDamage()) : 0;
                case 4 -> Math.round(motorSpeed(items.getStackInSlot(MOTOR)) * 10);
                default -> 0;
            };
        }

        public void set(int i, int v) {
        }

        public int getCount() {
            return 5;
        }
    };

    // Automation: input (and batteries) in from any side, results out
    private final IItemHandler automation = new IItemHandler() {
        public int getSlots() { return SLOTS; }
        public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (isBattery(stack)) return slot == BATTERY ? items.insertItem(BATTERY, stack, simulate) : stack;
            return slot == INPUT ? items.insertItem(INPUT, stack, simulate) : stack;
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= OUT0 && slot < OUT0 + OUTS ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        public boolean isItemValid(int slot, ItemStack stack) { return (slot == INPUT || slot == BATTERY) && items.isItemValid(slot, stack); }
    };
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> automation);

    public MiniCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryAscension.MINI_CRUSHER_BE.get(), pos, state);
    }

    private static String id(ItemStack s) {
        var key = ForgeRegistries.ITEMS.getKey(s.getItem());
        return key == null ? "" : key.toString();
    }

    public static boolean isMotor(ItemStack s) {
        return !s.isEmpty() && FAConfig.motors.containsKey(id(s));
    }

    public static boolean isBattery(ItemStack s) {
        return !s.isEmpty() && FAConfig.batteries.contains(id(s)) && s.getMaxDamage() > 0;
    }

    public static float motorSpeed(ItemStack s) {
        return s.isEmpty() ? 0 : FAConfig.motors.getOrDefault(id(s), 0f);
    }

    public ItemStackHandler items() { return items; }
    public ContainerData data() { return data; }

    /** Create's crushing recipe for the input, or its milling one; cached until the input changes. */
    private ProcessingRecipe<?> recipeFor(ItemStack input) {
        if (input.isEmpty()) return null;
        if (recipe != null && ItemStack.isSameItemSameTags(input, recipeFor)) return recipe;
        ItemStackHandler one = new ItemStackHandler(1);
        one.setStackInSlot(0, input.copyWithCount(1));
        RecipeWrapper wrap = new RecipeWrapper(one);
        Optional<net.minecraft.world.item.crafting.Recipe<RecipeWrapper>> found = AllRecipeTypes.CRUSHING.<RecipeWrapper, net.minecraft.world.item.crafting.Recipe<RecipeWrapper>>find(wrap, level);
        if (found.isEmpty()) found = AllRecipeTypes.MILLING.<RecipeWrapper, net.minecraft.world.item.crafting.Recipe<RecipeWrapper>>find(wrap, level);
        recipe = found.filter(r -> r instanceof ProcessingRecipe<?>).map(r -> (ProcessingRecipe<?>) r).orElse(null);
        recipeFor = input.copyWithCount(1);
        return recipe;
    }

    @Override
    public void serverTick() {
        ItemStack input = items.getStackInSlot(INPUT);
        float speed = motorSpeed(items.getStackInSlot(MOTOR));
        ProcessingRecipe<?> r = recipeFor(input);
        ItemStack battery = items.getStackInSlot(BATTERY);
        boolean charged = FAConfig.crusherCharge == 0 || (isBattery(battery) && battery.getMaxDamage() - battery.getDamageValue() >= FAConfig.crusherCharge);
        if (r == null || speed <= 0 || !charged) {
            if (progress != 0 || (r == null && needed != 0)) {
                progress = 0;
                needed = r == null ? 0 : needed;
                setChanged();
            }
            return;
        }
        int duration = r.getProcessingDuration() > 0 ? r.getProcessingDuration() : FAConfig.crusherTicks;
        needed = Math.max(1, Math.round(duration / speed));
        if (++progress < needed) return;
        List<ItemStack> results = r.rollResults();
        // Only finish if everything fits; otherwise wait (a full output never loses items)
        ItemStackHandler test = new ItemStackHandler(OUTS);
        for (int i = 0; i < OUTS; i++) test.setStackInSlot(i, items.getStackInSlot(OUT0 + i).copy());
        for (ItemStack res : results) {
            ItemStack left = res.copy();
            for (int i = 0; i < OUTS && !left.isEmpty(); i++) left = test.insertItem(i, left, false);
            if (!left.isEmpty()) {
                progress = needed;
                return;
            }
        }
        for (int i = 0; i < OUTS; i++) items.setStackInSlot(OUT0 + i, test.getStackInSlot(i));
        items.extractItem(INPUT, 1, false);
        if (FAConfig.crusherCharge > 0) battery.setDamageValue(battery.getDamageValue() + FAConfig.crusherCharge);
        progress = 0;
        setChanged();
        if (level != null) level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.2f);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factoryascension.mini_crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MiniCrusherMenu(id, inv, this);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        progress = tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return new CompoundTag();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}

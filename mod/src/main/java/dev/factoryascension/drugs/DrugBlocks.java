package dev.factoryascension.drugs;

import dev.factoryascension.FAConfig;
import dev.factoryascension.FactoryAscension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The drug blocks: two crops (cannabis, coca: plant the seeds on farmland, harvest the leaves) and the Chem Lab, a bench
 * with glassware where leaves and other things are worked into the finished drugs. Right-click the lab: it makes the
 * first recipe (FAConfig.chemRecipes, from the pack's config) you have everything for.
 */
public final class DrugBlocks {
    private DrugBlocks() {
    }

    /** A crop whose seed is its own item. */
    public static class Crop extends CropBlock {
        final java.util.function.Supplier<Item> seed;

        public Crop(Properties p, java.util.function.Supplier<Item> seed) {
            super(p);
            this.seed = seed;
        }

        @Override
        protected ItemLike getBaseSeedId() {
            return seed.get();
        }
    }

    static BlockBehaviour.Properties crop() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(net.minecraft.world.level.block.SoundType.CROP)
            .pushReaction(PushReaction.DESTROY);
    }

    public static final RegistryObject<Item> CANNABIS_SEEDS = FactoryAscension.ITEMS.register("cannabis_seeds", () -> new ItemNameBlockItem(DrugBlocks.CANNABIS.get(), new Item.Properties()));
    public static final RegistryObject<Item> COCA_SEEDS = FactoryAscension.ITEMS.register("coca_seeds", () -> new ItemNameBlockItem(DrugBlocks.COCA.get(), new Item.Properties()));
    public static final RegistryObject<Block> CANNABIS = FactoryAscension.BLOCKS.register("cannabis", () -> new Crop(crop(), CANNABIS_SEEDS));
    public static final RegistryObject<Block> COCA = FactoryAscension.BLOCKS.register("coca", () -> new Crop(crop(), COCA_SEEDS));
    public static final RegistryObject<Item> CANNABIS_LEAF = FactoryAscension.ITEMS.register("cannabis_leaf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COCA_LEAF = FactoryAscension.ITEMS.register("coca_leaf", () -> new Item(new Item.Properties()));

    /** The Chem Lab. */
    public static class ChemLab extends Block {
        static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

        public ChemLab(Properties p) {
            super(p);
        }

        @Override
        public VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter l, BlockPos pos, CollisionContext c) {
            return SHAPE;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            for (FAConfig.ChemRecipe r : FAConfig.chemRecipes) {
                if (!has(player, r.inputs())) continue;
                if (!player.isCreative()) for (var in : r.inputs()) take(player, item(in.getKey()), in.getValue());
                Item out = item(r.output());
                if (out == null) continue;
                ItemStack s = new ItemStack(out, r.count());
                if (!player.getInventory().add(s)) player.drop(s, false);
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1, 1.2f);
                ((ServerLevel) level).sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.02);
                return InteractionResult.CONSUME;
            }
            StringBuilder b = new StringBuilder("Nothing to make. Recipes: ");
            for (FAConfig.ChemRecipe r : FAConfig.chemRecipes) {
                Item out = item(r.output());
                b.append(out == null ? r.output() : new ItemStack(out).getHoverName().getString()).append(" (");
                boolean first = true;
                for (var in : r.inputs()) {
                    Item it = item(in.getKey());
                    b.append(first ? "" : ", ").append(in.getValue()).append(" ").append(it == null ? in.getKey() : new ItemStack(it).getHoverName().getString());
                    first = false;
                }
                b.append(") ");
            }
            player.displayClientMessage(Component.literal(b.toString()).withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }

        static Item item(String id) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        }

        static boolean has(Player p, List<java.util.Map.Entry<String, Integer>> ins) {
            if (p.isCreative()) return true;
            for (var in : ins) {
                Item it = item(in.getKey());
                if (it == null) return false;
                int n = 0;
                for (ItemStack s : p.getInventory().items) if (s.is(it)) n += s.getCount();
                if (n < in.getValue()) return false;
            }
            return true;
        }

        static void take(Player p, Item it, int n) {
            for (ItemStack s : p.getInventory().items) {
                if (n <= 0) return;
                if (!s.is(it)) continue;
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
    }

    public static final RegistryObject<Block> CHEM_LAB = FactoryAscension.BLOCKS.register("chem_lab", () -> new ChemLab(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL).strength(2.5f).sound(net.minecraft.world.level.block.SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<Item> CHEM_LAB_ITEM = FactoryAscension.ITEMS.register("chem_lab", () -> new net.minecraft.world.item.BlockItem(CHEM_LAB.get(), new Item.Properties()));

    public static void init() {
    }
}

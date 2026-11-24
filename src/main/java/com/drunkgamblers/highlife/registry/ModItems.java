package com.drunkgamblers.highlife.registry;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.effect.Highs;
import com.drunkgamblers.highlife.item.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HighLife.MODID);

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build();
    }

    // --- Cannabis ---
    public static final DeferredItem<Item> CANNABIS_SEEDS = ITEMS.register("cannabis_seeds",
            () -> new ItemNameBlockItem(ModBlocks.CANNABIS.get(), new Item.Properties()));
    public static final DeferredItem<Item> CANNABIS_BUD = ITEMS.registerSimpleItem("cannabis_bud");
    public static final DeferredItem<Item> DRIED_CANNABIS = ITEMS.registerSimpleItem("dried_cannabis");
    public static final DeferredItem<Item> GROUND_CANNABIS = ITEMS.registerSimpleItem("ground_cannabis");
    public static final DeferredItem<Item> GRINDER = ITEMS.register("grinder", () -> new GrinderItem(new Item.Properties()));
    public static final DeferredItem<Item> JOINT = ITEMS.register("joint", () -> new JointItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> LIT_JOINT = ITEMS.register("lit_joint", () -> new LitJointItem(new Item.Properties()));
    public static final DeferredItem<Item> BONG = ITEMS.register("bong", () -> new BongItem(new Item.Properties()));
    public static final DeferredItem<Item> CANNABUTTER = ITEMS.registerSimpleItem("cannabutter");
    public static final DeferredItem<Item> POT_BROWNIE = ITEMS.register("pot_brownie",
            () -> new ConsumableItem(new Item.Properties().food(food(5, 0.6f)), false, null, "tooltip.highlife.pot_brownie",
                    e -> e.addEffect(new MobEffectInstance(ModEffects.EDIBLE_ONSET, 600, 0, false, false, true))));

    // --- Magic mushrooms ---
    public static final DeferredItem<Item> MAGIC_MUSHROOM = ITEMS.register("magic_mushroom",
            () -> new ConsumableItem.Placeable(ModBlocks.MAGIC_MUSHROOM.get(), new Item.Properties().food(food(1, 0.1f)),
                    "tooltip.highlife.magic_mushroom", e -> Highs.stack(e, ModEffects.TRIPPING, 1, 1800, 2)));
    public static final DeferredItem<Item> MUSHROOM_TEA = ITEMS.register("mushroom_tea",
            () -> new ConsumableItem(new Item.Properties().stacksTo(1).food(food(3, 0.3f)), true, () -> new ItemStack(Items.BOWL),
                    "tooltip.highlife.mushroom_tea", e -> Highs.stack(e, ModEffects.TRIPPING, 2, 3600, 2)));

    // --- Tobacco ---
    public static final DeferredItem<Item> TOBACCO_SEEDS = ITEMS.register("tobacco_seeds",
            () -> new ItemNameBlockItem(ModBlocks.TOBACCO.get(), new Item.Properties()));
    public static final DeferredItem<Item> TOBACCO_LEAF = ITEMS.registerSimpleItem("tobacco_leaf");
    public static final DeferredItem<Item> DRIED_TOBACCO = ITEMS.registerSimpleItem("dried_tobacco");

    // --- Coca ---
    public static final DeferredItem<Item> COCA_SEEDS = ITEMS.register("coca_seeds",
            () -> new ItemNameBlockItem(ModBlocks.COCA.get(), new Item.Properties()));
    public static final DeferredItem<Item> COCA_LEAF = ITEMS.registerSimpleItem("coca_leaf");
    public static final DeferredItem<Item> DRIED_COCA_LEAF = ITEMS.registerSimpleItem("dried_coca_leaf");
    public static final DeferredItem<Item> COCAINE = ITEMS.register("cocaine", () -> new CocaineItem(new Item.Properties()));

    // --- Salvia ---
    public static final DeferredItem<Item> SALVIA_SEEDS = ITEMS.register("salvia_seeds",
            () -> new ItemNameBlockItem(ModBlocks.SALVIA.get(), new Item.Properties()));
    public static final DeferredItem<Item> SALVIA_LEAF = ITEMS.registerSimpleItem("salvia_leaf");
    public static final DeferredItem<Item> DRIED_SALVIA = ITEMS.register("dried_salvia", () -> new TooltipItem(new Item.Properties()));

    // --- Party drugs ---
    public static final DeferredItem<Item> ERGOT = ITEMS.registerSimpleItem("ergot");
    public static final DeferredItem<Item> LSD_BLOTTER = ITEMS.register("lsd_blotter",
            () -> new DoseItem(new Item.Properties().stacksTo(64), UseAnim.EAT, 16, ModSounds.PILL.get(), 1.3f,
                    e -> Highs.stack(e, ModEffects.ACID, 1, 7200, 1)));
    public static final DeferredItem<Item> SPEED_PILLS = ITEMS.register("speed_pills",
            () -> new DoseItem(new Item.Properties().stacksTo(32), UseAnim.EAT, 12, ModSounds.PILL.get(), 1.0f, e -> {
                e.removeEffect(ModEffects.COMEDOWN);
                Highs.stack(e, ModEffects.SPEEDING, 1, 6000, 1);
            }));
    public static final DeferredItem<Item> ECSTASY_PILL = ITEMS.register("ecstasy_pill",
            () -> new DoseItem(new Item.Properties().stacksTo(32), UseAnim.EAT, 12, ModSounds.PILL.get(), 1.1f,
                    e -> Highs.stack(e, ModEffects.ROLLING, 1, 4800, 1)));
    public static final DeferredItem<Item> KETAMINE = ITEMS.register("ketamine",
            () -> new DoseItem(new Item.Properties().stacksTo(16), UseAnim.TOOT_HORN, 20, ModSounds.SNORT.get(), 0.9f,
                    e -> Highs.stack(e, ModEffects.K_HOLE, 1, 1800, 1)));
    public static final DeferredItem<Item> NITROUS_BALLOON = ITEMS.register("nitrous_balloon",
            () -> new DoseItem(new Item.Properties().stacksTo(16), UseAnim.TOOT_HORN, 24, ModSounds.BALLOON.get(), 1.0f,
                    e -> Highs.stack(e, ModEffects.GIGGLES, 1, 500, 2)));
    public static final DeferredItem<Item> DMT = ITEMS.register("dmt", () -> new TooltipItem(new Item.Properties().stacksTo(16)));

    private ModItems() {}
}

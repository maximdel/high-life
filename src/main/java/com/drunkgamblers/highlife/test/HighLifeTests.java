package com.drunkgamblers.highlife.test;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.compat.SmokeBreakCompat;
import com.drunkgamblers.highlife.registry.ModEffects;
import com.drunkgamblers.highlife.registry.ModItems;
import com.smokebreak.device.DeviceLogic;
import com.smokebreak.device.LiquidHolder;
import com.smokebreak.registry.ModLiquids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Optional;

/** Run with: gradlew runGameTestServer */
@GameTestHolder(HighLife.MODID)
@PrefixGameTestTemplate(false)
public class HighLifeTests {
    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public static void dataLoads(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        List<String> recipes = List.of("dried_cannabis_from_smelting", "ground_cannabis", "joint_from_cigarette", "joint_from_paper",
                "bong", "cannabutter", "pot_brownie", "mushroom_tea", "cigarettes_from_tobacco", "cocaine", "grinder",
                "cannabis_distillate_liquid", "dried_coca_leaf_from_smoking", "dried_tobacco_from_campfire_cooking",
                "dried_salvia_from_smelting", "salvia_seeds", "lsd_blotter", "speed_pills", "ecstasy_pill", "ketamine",
                "nitrous_balloon", "dmt");
        for (String r : recipes) {
            helper.assertTrue(level.getRecipeManager().byKey(HighLife.id(r)).isPresent(), "missing recipe " + r);
        }
        helper.assertTrue(access.registryOrThrow(ModLiquids.REGISTRY_KEY).containsKey(SmokeBreakCompat.CANNABIS_DISTILLATE.location()),
                "cannabis distillate liquid not registered in Smoke Break");
        helper.assertTrue(access.registryOrThrow(Registries.PLACED_FEATURE).containsKey(HighLife.id("magic_mushroom_patch")),
                "mushroom placed feature missing");
        helper.assertTrue(access.registryOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS).containsKey(HighLife.id("magic_mushrooms")),
                "biome modifier missing");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("smokebreak:cigarette")), "smokebreak cigarette missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void jointGetsYouHigh(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack lit = new ItemStack(ModItems.LIT_JOINT.get());
        ItemStack after = lit.getItem().finishUsingItem(lit, helper.getLevel(), player);
        MobEffectInstance high = player.getEffect(ModEffects.HIGH);
        helper.assertTrue(high != null && high.getAmplifier() == 0, "expected High I after one puff");
        helper.assertTrue(after.getDamageValue() == 1, "joint should lose a puff");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cannabisVapeGetsYouHigh(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String[] devices = {"nimbo_stride", "nimbo_paladin_80", "aerion_arc_3", "stylo", "lumo_pebble"};
        String[] parts = {"coil_stride_mesh_06", "coil_nimbo_n2", "cartridge_arc_06", "cartridge_stylo", "cartridge_pebble_12"};
        for (int i = 0; i < devices.length; i++) {
            ItemStack device = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("smokebreak:" + devices[i])));
            DeviceLogic.setInstalledPart(device, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("smokebreak:" + parts[i]))));
            DeviceLogic.onPartInstalled(device);
            Optional<LiquidHolder> holder = DeviceLogic.liquidHolder(device);
            if (holder.isEmpty()) holder = DeviceLogic.cartridgeHolder(device);
            if (holder.isEmpty()) continue;
            holder.get().set(SmokeBreakCompat.CANNABIS_DISTILLATE, holder.get().capacity());

            player.setItemInHand(InteractionHand.MAIN_HAND, device);
            SmokeBreakCompat.tickPlayer(player); // remember the full level
            var result = DeviceLogic.puff(device, 30, level.registryAccess(), level.getGameTime() + 1000);
            SmokeBreakCompat.tickPlayer(player);
            if (!result.success()) {
                HighLife.LOGGER.info("[test] {} puff failed: {}", devices[i], result.status());
                continue;
            }
            HighLife.LOGGER.info("[test] {} puff ok, consumed {}", devices[i], result.consumed());
            helper.assertTrue(player.hasEffect(ModEffects.HIGH), "real Smoke Break puff with distillate should get you high");
            helper.succeed();
            return;
        }
        helper.fail("no Smoke Break device could take a puff in the test");
    }

    private static void use(GameTestHelper helper, Player player, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        item.finishUsingItem(stack, helper.getLevel(), player);
    }

    @GameTest(template = TEMPLATE)
    public static void dosesGiveTheirEffects(GameTestHelper helper) {
        Object[][] cases = {
                {ModItems.LSD_BLOTTER, ModEffects.ACID}, {ModItems.SPEED_PILLS, ModEffects.SPEEDING},
                {ModItems.ECSTASY_PILL, ModEffects.ROLLING}, {ModItems.KETAMINE, ModEffects.K_HOLE},
                {ModItems.NITROUS_BALLOON, ModEffects.GIGGLES}, {ModItems.COCAINE, ModEffects.WIRED}};
        for (Object[] c : cases) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            @SuppressWarnings("unchecked") var item = ((java.util.function.Supplier<net.minecraft.world.item.Item>) c[0]).get();
            @SuppressWarnings("unchecked") var effect = (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) c[1];
            use(helper, player, item);
            helper.assertTrue(player.hasEffect(effect), item + " did not apply " + effect.getRegisteredName());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bongSmokesSalviaAndDmt(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bong = new ItemStack(ModItems.BONG.get());
        player.getInventory().add(new ItemStack(ModItems.DRIED_SALVIA.get()));
        bong.getItem().finishUsingItem(bong, helper.getLevel(), player);
        helper.assertTrue(player.hasEffect(ModEffects.SALVIA), "salvia hit failed");
        helper.assertTrue(!player.getInventory().contains(new ItemStack(ModItems.DRIED_SALVIA.get())), "salvia not used up");

        Player p2 = helper.makeMockPlayer(GameType.SURVIVAL);
        p2.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DMT.get()));
        p2.getInventory().add(new ItemStack(ModItems.GROUND_CANNABIS.get()));
        bong.getItem().finishUsingItem(bong, helper.getLevel(), p2);
        helper.assertTrue(p2.hasEffect(ModEffects.BREAKTHROUGH) && !p2.hasEffect(ModEffects.HIGH), "off-hand DMT should be smoked first");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stimulantsCrashIntoComedown(GameTestHelper helper) {
        var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, 1, 1, 1);
        pig.addEffect(new MobEffectInstance(ModEffects.SPEEDING, 5, 0));
        var cow = helper.spawn(net.minecraft.world.entity.EntityType.COW, 1, 1, 1);
        cow.addEffect(new MobEffectInstance(ModEffects.ROLLING, 5, 0));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(pig.hasEffect(ModEffects.COMEDOWN), "speed should end in a comedown");
            helper.assertTrue(cow.hasEffect(ModEffects.COMEDOWN), "ecstasy should end in a comedown");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void chatChangesWhileHigh(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        String plain = com.drunkgamblers.highlife.effect.ChatEffects.transform(player, "hello there");
        helper.assertTrue(plain.equals("hello there"), "sober chat changed: " + plain);
        player.addEffect(new MobEffectInstance(ModEffects.K_HOLE, 100, 0));
        String k = com.drunkgamblers.highlife.effect.ChatEffects.transform(player, "hello there");
        helper.assertTrue(k.startsWith("heeellooo"), "k-hole chat: " + k);
        player.removeEffect(ModEffects.K_HOLE);
        player.addEffect(new MobEffectInstance(ModEffects.SPEEDING, 100, 0));
        String s = com.drunkgamblers.highlife.effect.ChatEffects.transform(player, "hello there");
        helper.assertTrue(s.equals("HELLO THERE!!!"), "speed chat: " + s);
        HighLife.LOGGER.info("[test] chat: '{}' / '{}'", k, s);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void soundsAndMunchies(GameTestHelper helper) {
        for (var holder : com.drunkgamblers.highlife.registry.ModSounds.SOUNDS.getEntries()) {
            helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(holder.getId()), "sound not registered: " + holder.getId());
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(4);
        player.addEffect(new MobEffectInstance(ModEffects.HIGH, 200, 1));
        ItemStack bread = new ItemStack(net.minecraft.world.item.Items.BREAD);
        com.drunkgamblers.highlife.GameEvents.onUseFinish(
                new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, bread, 0, ItemStack.EMPTY));
        helper.assertTrue(player.getFoodData().getFoodLevel() == 6, "munchies should add 2 food at High II, got " + player.getFoodData().getFoodLevel());
        helper.succeed();
    }
}

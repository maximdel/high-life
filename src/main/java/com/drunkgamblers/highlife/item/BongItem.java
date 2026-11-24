package com.drunkgamblers.highlife.item;

import com.drunkgamblers.highlife.registry.ModSounds;
import com.drunkgamblers.highlife.effect.Highs;
import com.drunkgamblers.highlife.registry.ModEffects;
import com.drunkgamblers.highlife.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Glass bong. Packs whatever smokeable is in your off hand, otherwise the first of
 * Ground Cannabis, Dried Salvia, DMT found in your inventory.
 */
public class BongItem extends Item {
    private record Load(Supplier<Item> item, Consumer<LivingEntity> hit) {}

    private static final List<Load> LOADS = List.of(
            new Load(ModItems.GROUND_CANNABIS, e -> {
                Highs.addHigh(e, 2, 3600);
                if (e.getRandom().nextInt(3) == 0) Highs.cough(e);
            }),
            new Load(ModItems.DRIED_SALVIA, e -> {
                e.addEffect(new MobEffectInstance(ModEffects.SALVIA, 900, 0));
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false, false));
            }),
            new Load(ModItems.DMT, e -> {
                e.addEffect(new MobEffectInstance(ModEffects.BREAKTHROUGH, 600, 0));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
            }));

    public BongItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static Load loadFor(ItemStack s) {
        for (Load l : LOADS) if (s.is(l.item().get())) return l;
        return null;
    }

    /** The stack that will be smoked, or EMPTY. */
    private static ItemStack findLoad(Player player) {
        ItemStack off = player.getOffhandItem();
        if (loadFor(off) != null) return off;
        for (Load l : LOADS) {
            for (ItemStack s : player.getInventory().items) {
                if (s.is(l.item().get())) return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bong = player.getItemInHand(hand);
        if (findLoad(player).isEmpty()) {
            if (level.isClientSide) player.displayClientMessage(Component.translatable("message.highlife.bong_empty"), true);
            return InteractionResultHolder.fail(bong);
        }
        if (!level.isClientSide) Highs.sound(player, ModSounds.BONG_BUBBLE.get(), 1.0f, 1.0f);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(bong);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 45;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            ItemStack packed = findLoad(player);
            Load load = loadFor(packed);
            if (load == null) return stack;
            if (!player.getAbilities().instabuild) packed.shrink(1);
            Highs.sound(player, ModSounds.EXHALE.get(), 1.0f, 0.8f);
            Highs.exhale(player, 30, 0.35f);
            load.hit().accept(player);
            player.getCooldowns().addCooldown(this, 40);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.highlife.bong").withStyle(ChatFormatting.GRAY));
    }
}

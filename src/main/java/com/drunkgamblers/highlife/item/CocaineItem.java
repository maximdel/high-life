package com.drunkgamblers.highlife.item;

import com.drunkgamblers.highlife.registry.ModSounds;
import com.drunkgamblers.highlife.effect.Highs;
import com.drunkgamblers.highlife.registry.ModEffects;
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

/** Bag of coke: strong buff (Wired), harsh Comedown when it wears off, overdose if you push it. */
public class CocaineItem extends Item {
    public static final int MAX_WIRED = 2;

    public CocaineItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            Highs.sound(entity, ModSounds.SNORT.get(), 1.0f, 1.1f);
            MobEffectInstance cur = entity.getEffect(ModEffects.WIRED);
            if (cur != null && cur.getAmplifier() >= MAX_WIRED) {
                // overdose
                entity.hurt(entity.damageSources().magic(), 8.0f);
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1));
                if (entity instanceof Player p) p.displayClientMessage(Component.translatable("message.highlife.overdose"), true);
            }
            entity.removeEffect(ModEffects.COMEDOWN);
            Highs.stack(entity, ModEffects.WIRED, 1, 1200, MAX_WIRED);
            if (entity instanceof Player p) {
                p.getCooldowns().addCooldown(this, 40);
                if (!p.getAbilities().instabuild) stack.shrink(1);
            } else {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.highlife.cocaine").withStyle(ChatFormatting.GRAY));
    }
}

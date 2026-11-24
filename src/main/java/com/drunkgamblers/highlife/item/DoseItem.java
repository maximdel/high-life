package com.drunkgamblers.highlife.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/** A single-use dose (pill, tab, bump, balloon): hold right-click, get the effect, item is used up. */
public class DoseItem extends Item {
    private final UseAnim anim;
    private final int useTicks;
    private final SoundEvent sound;
    private final float pitch;
    private final Consumer<LivingEntity> effect;

    public DoseItem(Properties p, UseAnim anim, int useTicks, SoundEvent sound, float pitch, Consumer<LivingEntity> effect) {
        super(p);
        this.anim = anim;
        this.useTicks = useTicks;
        this.sound = sound;
        this.pitch = pitch;
        this.effect = effect;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return anim;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return useTicks;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), 1.0f, pitch);
            effect.accept(entity);
            if (entity instanceof Player p) {
                p.getCooldowns().addCooldown(this, 30);
                if (!p.getAbilities().instabuild) stack.shrink(1);
            } else {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}

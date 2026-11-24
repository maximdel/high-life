package com.drunkgamblers.highlife.item;

import com.drunkgamblers.highlife.registry.ModSounds;
import com.drunkgamblers.highlife.effect.Highs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Burning joint: hold right-click to take a hit. Each puff uses one point of durability. */
public class LitJointItem extends Item {
    public static final int PUFFS = 6;

    public LitJointItem(Properties properties) {
        super(properties.durability(PUFFS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) Highs.sound(player, ModSounds.INHALE.get(), 0.8f, 0.95f + level.random.nextFloat() * 0.1f);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            Highs.sound(entity, ModSounds.EXHALE.get(), 0.8f, 1.0f);
            Highs.exhale(entity, 8, 0.15f);
            Highs.addHigh(entity, 1, 2400);
            if (entity.getRandom().nextInt(8) == 0) Highs.cough(entity);
            if (entity instanceof Player p) p.getCooldowns().addCooldown(this, 20);
            int used = stack.getDamageValue() + 1;
            if (used >= stack.getMaxDamage()) {
                Highs.sound(entity, SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.6f);
                stack.shrink(1);
                return stack;
            }
            stack.setDamageValue(used);
        }
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // a thin wisp of smoke while held
        if (selected && level instanceof ServerLevel server && level.getGameTime() % 10 == 0) {
            var look = entity.getLookAngle();
            server.sendParticles(ParticleTypes.SMOKE, entity.getX() + look.x * 0.6 - look.z * 0.35, entity.getEyeY() - 0.35,
                    entity.getZ() + look.z * 0.6 + look.x * 0.35, 1, 0.01, 0.02, 0.01, 0.005);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("tooltip.highlife.puffs_left", left).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

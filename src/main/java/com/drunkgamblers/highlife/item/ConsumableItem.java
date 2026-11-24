package com.drunkgamblers.highlife.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Food/drink that applies a custom effect when finished, optionally leaving a container behind. */
public class ConsumableItem extends Item {
    private final Consumer<LivingEntity> onConsumed;
    private final boolean drink;
    private final Supplier<ItemStack> remainder;
    private final String tooltipKey;

    public ConsumableItem(Properties p, boolean drink, Supplier<ItemStack> remainder, String tooltipKey, Consumer<LivingEntity> onConsumed) {
        super(p);
        this.onConsumed = onConsumed;
        this.drink = drink;
        this.remainder = remainder;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide) onConsumed.accept(entity);
        return giveRemainder(result, entity, remainder);
    }

    static ItemStack giveRemainder(ItemStack result, LivingEntity entity, Supplier<ItemStack> remainder) {
        if (remainder == null || (entity instanceof Player p && p.getAbilities().instabuild)) return result;
        ItemStack leftover = remainder.get();
        if (result.isEmpty()) return leftover;
        if (entity instanceof Player p && !p.getInventory().add(leftover)) p.drop(leftover, false);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        if (tooltipKey != null) tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }

    /** Same idea for a placeable block that can also be eaten (the magic mushroom). */
    public static class Placeable extends BlockItem {
        private final Consumer<LivingEntity> onConsumed;
        private final String tooltipKey;

        public Placeable(Block block, Properties p, String tooltipKey, Consumer<LivingEntity> onConsumed) {
            super(block, p);
            this.onConsumed = onConsumed;
            this.tooltipKey = tooltipKey;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            if (!level.isClientSide) onConsumed.accept(entity);
            return result;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        }
    }
}

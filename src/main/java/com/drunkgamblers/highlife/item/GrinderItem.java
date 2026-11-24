package com.drunkgamblers.highlife.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Crafting tool: stays in the grid and loses one durability per grind. */
public class GrinderItem extends Item {
    public GrinderItem(Properties properties) {
        super(properties.durability(128));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        int damage = copy.getDamageValue() + 1;
        if (damage >= copy.getMaxDamage()) return ItemStack.EMPTY;
        copy.setDamageValue(damage);
        return copy;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.highlife.grinder").withStyle(ChatFormatting.GRAY));
    }
}

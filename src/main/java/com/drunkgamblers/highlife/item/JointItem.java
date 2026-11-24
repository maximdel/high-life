package com.drunkgamblers.highlife.item;

import com.drunkgamblers.highlife.registry.ModSounds;
import com.drunkgamblers.highlife.registry.ModItems;
import com.smokebreak.item.LighterItem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Unlit joint. Light it with a Smoke Break lighter (or flint and steel) held in the other hand. */
public class JointItem extends Item {
    public JointItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack joint = player.getItemInHand(hand);
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack igniter = player.getItemInHand(other);
        if (!LighterItem.canIgnite(igniter)) {
            if (level.isClientSide) player.displayClientMessage(Component.translatable("message.highlife.need_lighter"), true);
            return InteractionResultHolder.fail(joint);
        }
        if (!level.isClientSide) {
            LighterItem.ignite(igniter, player, other);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.LIGHTER_FLICK.get(), player.getSoundSource(), 1.0f, 1.0f);
            ItemStack lit = new ItemStack(ModItems.LIT_JOINT.get());
            if (joint.getCount() == 1) {
                return InteractionResultHolder.success(lit);
            }
            joint.shrink(1);
            if (!player.addItem(lit)) player.drop(lit, false);
        }
        return InteractionResultHolder.sidedSuccess(joint, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.highlife.joint").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

package com.drunkgamblers.highlife;

import com.drunkgamblers.highlife.compat.SmokeBreakCompat;
import com.drunkgamblers.highlife.effect.ChatEffects;
import com.drunkgamblers.highlife.effect.Highs;
import com.drunkgamblers.highlife.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class GameEvents {
    private static boolean is(MobEffectInstance inst, net.minecraft.core.Holder<MobEffect> effect) {
        return inst.getEffect().value() == effect.value();
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance inst = event.getEffectInstance();
        LivingEntity entity = event.getEntity();
        if (inst == null || entity.level().isClientSide) return;
        int amp = inst.getAmplifier();
        if (is(inst, ModEffects.EDIBLE_ONSET)) {
            Highs.addHigh(entity, 3, 4800); // the brownie kicks in all at once
        } else if (is(inst, ModEffects.WIRED)) {
            entity.addEffect(new MobEffectInstance(ModEffects.COMEDOWN, 1200 + 600 * amp, amp));
        } else if (is(inst, ModEffects.SPEEDING)) {
            entity.addEffect(new MobEffectInstance(ModEffects.COMEDOWN, 2400, amp));
        } else if (is(inst, ModEffects.ROLLING)) {
            entity.addEffect(new MobEffectInstance(ModEffects.COMEDOWN, 1200, 0));
            if (entity instanceof Player p) p.displayClientMessage(Component.translatable("message.highlife.tuesday"), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) SmokeBreakCompat.tickPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SmokeBreakCompat.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onSleep(CanPlayerSleepEvent event) {
        Player p = event.getEntity();
        if (p.hasEffect(ModEffects.SPEEDING) || p.hasEffect(ModEffects.WIRED)) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            p.displayClientMessage(Component.translatable("message.highlife.cant_sleep"), true);
        }
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String raw = event.getRawText();
        if (raw.startsWith("/")) return;
        String changed = ChatEffects.transform(event.getPlayer(), raw);
        if (!changed.equals(raw)) {
            event.setMessage(Component.literal(changed));
        }
    }

    /** Munchies: food fills you up more while you're High. */
    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        MobEffectInstance high = p.getEffect(ModEffects.HIGH);
        var food = event.getItem().get(net.minecraft.core.component.DataComponents.FOOD);
        if (high == null || food == null) return;
        p.getFoodData().eat(1 + high.getAmplifier(), 0.3f);
        if (p.getRandom().nextInt(3) == 0) p.displayClientMessage(Component.translatable("message.highlife.munchies"), true);
    }

    private GameEvents() {}
}

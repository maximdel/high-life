package com.drunkgamblers.highlife.effect;

import com.drunkgamblers.highlife.registry.ModSounds;
import com.drunkgamblers.highlife.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Helpers to stack effects, blow smoke and cough. */
public final class Highs {
    public static final int MAX_HIGH = 4;

    /** Adds {@code levels} to the current amplifier (capped) and keeps the longer duration. */
    public static void stack(LivingEntity e, Holder<MobEffect> effect, int levels, int duration, int maxAmp) {
        MobEffectInstance cur = e.getEffect(effect);
        int amp = cur == null ? levels - 1 : Math.min(cur.getAmplifier() + levels, maxAmp);
        int dur = cur == null ? duration : Math.max(cur.getDuration(), duration);
        e.removeEffect(effect); // so a lower duration/amp can be replaced cleanly
        e.addEffect(new MobEffectInstance(effect, dur, Math.min(amp, maxAmp), false, true, true));
    }

    public static void addHigh(LivingEntity e, int levels, int duration) {
        stack(e, ModEffects.HIGH, levels, duration, MAX_HIGH);
    }

    public static void exhale(LivingEntity e, int amount, float spread) {
        if (!(e.level() instanceof ServerLevel level)) return;
        Vec3 look = e.getLookAngle();
        Vec3 mouth = e.getEyePosition().add(look.scale(0.4)).add(0, -0.15, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x, mouth.y, mouth.z, 0, look.x, look.y + 0.15, look.z, 0.03);
        level.sendParticles(ParticleTypes.SMOKE, mouth.x, mouth.y, mouth.z, amount, spread, spread * 0.6, spread, 0.02);
        level.sendParticles(ParticleTypes.POOF, mouth.x + look.x * 0.5, mouth.y, mouth.z + look.z * 0.5, amount / 3, spread, spread * 0.5, spread, 0.01);
    }

    public static void sound(LivingEntity e, SoundEvent sound, float volume, float pitch) {
        e.level().playSound(null, e.getX(), e.getY(), e.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void cough(LivingEntity e) {
        sound(e, ModSounds.COUGH.get(), 0.9f, 0.9f + e.getRandom().nextFloat() * 0.2f);
        exhale(e, 12, 0.25f);
        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false, false));
    }

    private Highs() {}
}

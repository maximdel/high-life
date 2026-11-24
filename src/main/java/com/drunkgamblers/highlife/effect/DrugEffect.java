package com.drunkgamblers.highlife.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

/** One class for all of the mod's effects; behaviour per tick depends on the kind. */
public class DrugEffect extends MobEffect {
    public enum Kind { HIGH, TRIPPING, WIRED, COMEDOWN, ONSET, SALVIA, SPEEDING, ROLLING, ACID, K_HOLE, GIGGLES, BREAKTHROUGH }

    private final Kind kind;

    public DrugEffect(Kind kind, MobEffectCategory category, int color) {
        super(category, color);
        this.kind = kind;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return switch (kind) {
            case HIGH, COMEDOWN, BREAKTHROUGH -> duration % 40 == 0;
            case TRIPPING, ACID -> duration % 60 == 0;
            case SALVIA -> duration % 15 == 0;
            case ROLLING -> duration % 50 == 0;
            case K_HOLE -> duration % 20 == 0;
            case GIGGLES -> duration % 25 == 0;
            default -> false;
        };
    }

    private static void quiet(LivingEntity e, net.minecraft.core.Holder<MobEffect> effect, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(effect, ticks, amp, false, false, false));
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;
        var random = entity.getRandom();
        switch (kind) {
            case HIGH -> {
                // munchies + a little pain relief; too high gets you dizzy
                if (entity instanceof Player p) p.causeFoodExhaustion(0.8f * (amplifier + 1));
                if (random.nextInt(3) == 0) entity.heal(1.0f);
                if (amplifier >= 3 && random.nextInt(5) == 0) quiet(entity, MobEffects.CONFUSION, 160, 0);
            }
            case TRIPPING -> quiet(entity, MobEffects.NIGHT_VISION, 260, 0);
            case COMEDOWN -> {
                if (entity instanceof Player p) p.causeFoodExhaustion(0.5f * (amplifier + 1));
            }
            case SALVIA -> {
                // the world pulls you sideways
                double a = random.nextDouble() * Math.PI * 2;
                entity.push(Math.cos(a) * 0.35, 0.05, Math.sin(a) * 0.35);
                entity.hurtMarked = true;
                if (random.nextInt(4) == 0) quiet(entity, MobEffects.CONFUSION, 100, 0);
            }
            case ROLLING -> {
                // love everyone: hearts, healing, and nearby animals fall in love
                level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 2.1, entity.getZ(), 3, 0.6, 0.3, 0.6, 0.01);
                entity.heal(1.0f);
                Player lover = entity instanceof Player p ? p : null;
                for (Animal animal : level.getEntitiesOfClass(Animal.class, entity.getBoundingBox().inflate(6))) {
                    if (animal.getAge() == 0 && animal.canFallInLove() && random.nextInt(3) == 0) animal.setInLove(lover);
                }
            }
            case ACID -> {
                quiet(entity, MobEffects.NIGHT_VISION, 400, 0);
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(14), o -> o != entity)) {
                    quiet(other, MobEffects.GLOWING, 80, 0);
                }
            }
            case K_HOLE -> quiet(entity, MobEffects.SLOW_FALLING, 40, 0);
            case GIGGLES -> {
                if (random.nextInt(2) == 0) {
                    Highs.sound(entity, SoundEvents.WITCH_CELEBRATE, 0.9f, 1.5f + random.nextFloat() * 0.4f);
                }
            }
            case BREAKTHROUGH -> {
                quiet(entity, MobEffects.LEVITATION, 30, 0);
                level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 1, entity.getZ(), 6, 1.2, 1.2, 1.2, 0.02);
            }
            default -> {}
        }
        return true;
    }
}

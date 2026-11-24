package com.drunkgamblers.highlife.registry;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.effect.DrugEffect;
import com.drunkgamblers.highlife.effect.DrugEffect.Kind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HighLife.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> HIGH = EFFECTS.register("high",
            () -> new DrugEffect(Kind.HIGH, MobEffectCategory.NEUTRAL, 0x5DBB3F)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, HighLife.id("effect.high"), -0.04, Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> TRIPPING = EFFECTS.register("tripping",
            () -> new DrugEffect(Kind.TRIPPING, MobEffectCategory.NEUTRAL, 0xD15CFF));

    public static final DeferredHolder<MobEffect, MobEffect> WIRED = EFFECTS.register("wired",
            () -> new DrugEffect(Kind.WIRED, MobEffectCategory.BENEFICIAL, 0xF2F2F2)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, HighLife.id("effect.wired_speed"), 0.15, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, HighLife.id("effect.wired_mining"), 0.30, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, HighLife.id("effect.wired_attack"), 0.20, Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> COMEDOWN = EFFECTS.register("comedown",
            () -> new DrugEffect(Kind.COMEDOWN, MobEffectCategory.HARMFUL, 0x55566A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, HighLife.id("effect.comedown_speed"), -0.12, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, HighLife.id("effect.comedown_mining"), -0.25, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, HighLife.id("effect.comedown_damage"), -1.5, Operation.ADD_VALUE));

    /** Hidden timer from eating an edible; turns into a strong High when it runs out. */
    public static final DeferredHolder<MobEffect, MobEffect> EDIBLE_ONSET = EFFECTS.register("edible_onset",
            () -> new DrugEffect(Kind.ONSET, MobEffectCategory.NEUTRAL, 0x8D6E63));

    public static final DeferredHolder<MobEffect, MobEffect> SALVIA = EFFECTS.register("salvia",
            () -> new DrugEffect(Kind.SALVIA, MobEffectCategory.NEUTRAL, 0xE8B23A));

    public static final DeferredHolder<MobEffect, MobEffect> SPEEDING = EFFECTS.register("speeding",
            () -> new DrugEffect(Kind.SPEEDING, MobEffectCategory.BENEFICIAL, 0x4FD8FF)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, HighLife.id("effect.speeding_speed"), 0.25, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, HighLife.id("effect.speeding_mining"), 0.20, Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, HighLife.id("effect.speeding_attack"), 0.15, Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> ROLLING = EFFECTS.register("rolling",
            () -> new DrugEffect(Kind.ROLLING, MobEffectCategory.BENEFICIAL, 0xFF6EC7)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, HighLife.id("effect.rolling_jump"), 0.15, Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> ACID = EFFECTS.register("acid_trip",
            () -> new DrugEffect(Kind.ACID, MobEffectCategory.NEUTRAL, 0xFFE14D));

    public static final DeferredHolder<MobEffect, MobEffect> K_HOLE = EFFECTS.register("k_hole",
            () -> new DrugEffect(Kind.K_HOLE, MobEffectCategory.NEUTRAL, 0x6A4CB0)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, HighLife.id("effect.k_hole_speed"), -0.35, Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, MobEffect> GIGGLES = EFFECTS.register("giggles",
            () -> new DrugEffect(Kind.GIGGLES, MobEffectCategory.NEUTRAL, 0xBFE6FF));

    public static final DeferredHolder<MobEffect, MobEffect> BREAKTHROUGH = EFFECTS.register("breakthrough",
            () -> new DrugEffect(Kind.BREAKTHROUGH, MobEffectCategory.NEUTRAL, 0x9B5CFF));

    private ModEffects() {}
}

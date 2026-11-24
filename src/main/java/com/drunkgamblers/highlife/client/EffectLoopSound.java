package com.drunkgamblers.highlife.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.function.BooleanSupplier;

/** A sound "in your head": loops at the listener while {@code active} is true, then fades out. */
public class EffectLoopSound extends AbstractTickableSoundInstance {
    private final BooleanSupplier active;
    private final float targetVolume;
    private boolean fadingOut;

    public EffectLoopSound(SoundEvent sound, SoundSource source, float volume, BooleanSupplier active) {
        super(sound, source, RandomSource.create());
        this.active = active;
        this.targetVolume = volume;
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = this.y = this.z = 0;
        this.volume = 0.01f; // fade in
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        fadingOut = !active.getAsBoolean();
        if (fadingOut) {
            volume -= targetVolume / 40f; // ~2 s fade out
            if (volume <= 0.001f) stop();
        } else if (volume < targetVolume) {
            volume = Math.min(targetVolume, volume + targetVolume / 30f);
        }
    }

    public boolean isFadingOut() {
        return fadingOut;
    }
}

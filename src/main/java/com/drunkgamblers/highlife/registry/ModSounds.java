package com.drunkgamblers.highlife.registry;

import com.drunkgamblers.highlife.HighLife;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** All sounds are original, synthesized by tools/gen_sounds.py. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, HighLife.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(HighLife.id(name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_RAVE = reg("music.rave");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> INHALE = reg("smoke.inhale");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXHALE = reg("smoke.exhale");
    public static final DeferredHolder<SoundEvent, SoundEvent> COUGH = reg("smoke.cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONG_BUBBLE = reg("bong.bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHTER_FLICK = reg("lighter.flick");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNORT = reg("dose.snort");
    public static final DeferredHolder<SoundEvent, SoundEvent> PILL = reg("dose.pill");
    public static final DeferredHolder<SoundEvent, SoundEvent> BALLOON = reg("dose.balloon");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRIP_CHIME = reg("trip.chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRIP_WHISPER = reg("trip.whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRIP_WHOOSH = reg("trip.whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_ELF = reg("trip.machine_elf");
    public static final DeferredHolder<SoundEvent, SoundEvent> SALVIA_RUMBLE = reg("salvia.rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> K_HOLE_DRONE = reg("drone.k_hole");
    public static final DeferredHolder<SoundEvent, SoundEvent> NITROUS_WAH = reg("nitrous.wah");

    private ModSounds() {}
}

package com.drunkgamblers.highlife.client;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.registry.ModEffects;
import com.drunkgamblers.highlife.registry.ModSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Screen tints, camera sway and hallucinations. Client only. */
public final class ClientEffects {
    private static final ParticleOptions[] TRIP_PARTICLES = {
            ParticleTypes.END_ROD, ParticleTypes.NOTE, ParticleTypes.ENCHANT, ParticleTypes.GLOW, ParticleTypes.WAX_ON, ParticleTypes.SCRAPE };
    private static final float BEAT_TICKS = 20f * 60f / 128f; // 128 bpm

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientEffects::registerLayers);
        NeoForge.EVENT_BUS.addListener(ClientEffects::cameraAngles);
        NeoForge.EVENT_BUS.addListener(ClientEffects::fov);
        NeoForge.EVENT_BUS.addListener(ClientEffects::clientTick);
        ClientSelfTest.initIfEnabled();
    }

    private static int amp(LocalPlayer p, Holder<MobEffect> effect) {
        if (p == null) return -1;
        MobEffectInstance inst = p.getEffect(effect);
        return inst == null ? -1 : inst.getAmplifier();
    }

    private static int remaining(LocalPlayer p, Holder<MobEffect> effect) {
        MobEffectInstance inst = p.getEffect(effect);
        return inst == null ? 0 : inst.getDuration();
    }

    private static int a(float alpha) {
        return Mth.clamp((int) (alpha * 255), 0, 255) << 24;
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, HighLife.id("drug_overlay"), ClientEffects::renderOverlay);
    }

    private static void renderOverlay(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float t = p.tickCount + delta.getGameTimeDeltaPartialTick(true);

        int high = amp(p, ModEffects.HIGH);
        if (high >= 0) {
            float pulse = 0.5f + 0.5f * Mth.sin(t * 0.05f);
            int green = 0x2F8F2A;
            int edge = a(0.10f + 0.06f * high + 0.04f * pulse) | green;
            g.fillGradient(0, 0, w, h / 3, edge, green);
            g.fillGradient(0, h - h / 3, w, h, green, edge);
            g.fill(0, 0, w, h, a(0.03f + 0.015f * high) | green);
        }

        int trip = amp(p, ModEffects.TRIPPING);
        if (trip >= 0) rainbowBands(g, w, h, t, 0.10f + 0.06f * trip, 10, true);

        int acid = amp(p, ModEffects.ACID);
        if (acid >= 0) {
            // kaleidoscope: horizontal and vertical rainbow bands crossing
            rainbowBands(g, w, h, t, 0.09f + 0.05f * acid, 12, true);
            rainbowBands(g, w, h, -t * 1.3f, 0.07f + 0.05f * acid, 16, false);
        }

        int salvia = amp(p, ModEffects.SALVIA);
        if (salvia >= 0) {
            // the "zipper": sliding yellow/orange stripes
            int stripes = 14;
            float shift = (t * 0.6f) % (w / (float) stripes * 2);
            for (int i = -2; i < stripes + 2; i++) {
                int x0 = (int) (i * w / (float) stripes + shift);
                int col = (i & 1) == 0 ? 0xF2B33D : 0xC2571E;
                g.fill(x0, 0, x0 + w / stripes, h, a(0.22f) | col);
            }
        }

        int speed = amp(p, ModEffects.SPEEDING);
        if (speed >= 0) {
            int cyan = 0x7FE8FF;
            g.fillGradient(0, 0, w, h / 5, a(0.14f) | cyan, cyan);
            g.fillGradient(0, h - h / 5, w, h, cyan, a(0.14f) | cyan);
        }

        int rolling = amp(p, ModEffects.ROLLING);
        if (rolling >= 0) {
            float phase = (beatTime(p, delta.getGameTimeDeltaPartialTick(true)) % BEAT_TICKS) / BEAT_TICKS;
            float pulse = (1 - phase) * (1 - phase);
            g.fill(0, 0, w, h, a(0.05f + 0.16f * pulse) | 0xFF5CC8);
        }

        int giggles = amp(p, ModEffects.GIGGLES);
        if (giggles >= 0) {
            g.fill(0, 0, w, h, a(0.08f + 0.06f * Mth.sin(t * 0.9f)) | 0xBFE6FF);
        }

        int khole = amp(p, ModEffects.K_HOLE);
        if (khole >= 0) {
            int purple = 0x1B0F33;
            int dark = a(0.55f + 0.1f * khole) | purple;
            g.fillGradient(0, 0, w, h / 2, dark, purple);
            g.fillGradient(0, h / 2, w, h, purple, dark);
            g.fill(0, 0, w, h, a(0.18f) | purple);
        }

        if (amp(p, ModEffects.BREAKTHROUGH) >= 0) {
            int left = remaining(p, ModEffects.BREAKTHROUGH);
            tunnel(g, w, h, t);
            if (left > 520) g.fill(0, 0, w, h, a((left - 520) / 80f) | 0xFFFFFF); // white-out at the start
        }

        int down = amp(p, ModEffects.COMEDOWN);
        if (down >= 0) g.fill(0, 0, w, h, a(0.12f + 0.05f * down) | 0x30303A);
    }

    private static void rainbowBands(GuiGraphics g, int w, int h, float t, float alpha, int bands, boolean horizontal) {
        for (int i = 0; i < bands; i++) {
            float hue = (t * 0.006f + i / (float) bands + 0.08f * Mth.sin(t * 0.03f + i)) % 1.0f;
            if (hue < 0) hue += 1;
            int rgb = Mth.hsvToRgb(hue, 0.8f, 1.0f);
            if (horizontal) g.fill(0, i * h / bands, w, (i + 1) * h / bands, a(alpha) | rgb);
            else g.fill(i * w / bands, 0, (i + 1) * w / bands, h, a(alpha) | rgb);
        }
    }

    /** Concentric colour frames rushing towards the centre. */
    private static void tunnel(GuiGraphics g, int w, int h, float t) {
        int rings = 14;
        float step = Math.min(w, h) / (rings * 2f);
        float travel = (t * 0.25f) % 1f;
        for (int i = 0; i < rings; i++) {
            int inset = (int) ((i + travel) * step);
            int thick = Math.max(2, (int) (step * 0.6f));
            int rgb = Mth.hsvToRgb(((i * 0.08f) - t * 0.015f) % 1f + 1f, 0.9f, 1f);
            int c = a(0.30f) | rgb;
            g.fill(inset, inset, w - inset, inset + thick, c);
            g.fill(inset, h - inset - thick, w - inset, h - inset, c);
            g.fill(inset, inset + thick, inset + thick, h - inset - thick, c);
            g.fill(w - inset - thick, inset + thick, w - inset, h - inset - thick, c);
        }
    }

    private static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        float t = (float) (p.tickCount + event.getPartialTick());
        float roll = event.getRoll();
        int high = amp(p, ModEffects.HIGH);
        if (high >= 0) roll += Mth.sin(t * 0.04f) * 0.8f * (high + 1);
        int trip = Math.max(amp(p, ModEffects.TRIPPING), amp(p, ModEffects.ACID));
        if (trip >= 0) roll += Mth.sin(t * 0.07f) * 3.0f * (trip + 1);
        if (amp(p, ModEffects.SALVIA) >= 0) roll += Mth.sin(t * 0.045f) * 28f;
        if (amp(p, ModEffects.SPEEDING) >= 0 || amp(p, ModEffects.WIRED) >= 0) roll += (p.getRandom().nextFloat() - 0.5f) * 1.2f;
        if (amp(p, ModEffects.ROLLING) >= 0) roll += Mth.sin(t * (Mth.TWO_PI / BEAT_TICKS / 2)) * 1.5f;
        if (amp(p, ModEffects.BREAKTHROUGH) >= 0) roll += t * 0.6f % 360f;
        if (amp(p, ModEffects.K_HOLE) >= 0) {
            event.setYaw(event.getYaw() + Mth.sin(t * 0.02f) * 9f);
            event.setPitch(event.getPitch() + Mth.sin(t * 0.017f) * 5f);
        }
        event.setRoll(roll);
    }

    private static void fov(ViewportEvent.ComputeFov event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        float t = (float) (p.tickCount + event.getPartialTick());
        double f = 1.0;
        int trip = Math.max(amp(p, ModEffects.TRIPPING), amp(p, ModEffects.ACID));
        if (trip >= 0) f *= 1.0 + 0.06 * (trip + 1) * Mth.sin(t * 0.05f);
        if (amp(p, ModEffects.GIGGLES) >= 0) f *= 1.0 + 0.10 * Mth.sin(t * 0.9f);
        if (amp(p, ModEffects.SPEEDING) >= 0) f *= 1.08;
        if (amp(p, ModEffects.K_HOLE) >= 0) f *= 1.25 + 0.05 * Mth.sin(t * 0.03f);
        if (amp(p, ModEffects.BREAKTHROUGH) >= 0) f *= 1.3 + 0.15 * Mth.sin(t * 0.1f);
        if (amp(p, ModEffects.ROLLING) >= 0) {
            float phase = (beatTime(p, (float) event.getPartialTick()) % BEAT_TICKS) / BEAT_TICKS;
            f *= 1.0 + 0.03 * (1 - phase);
        }
        if (f != 1.0) event.setFOV(event.getFOV() * f);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            Hallucinations.clear();
            return;
        }
        if (mc.isPaused()) return;
        RandomSource r = p.getRandom();
        int acid = amp(p, ModEffects.ACID);
        int trip = Math.max(amp(p, ModEffects.TRIPPING), acid >= 0 ? acid + 1 : -1); // -1 = not tripping
        boolean dmt = amp(p, ModEffects.BREAKTHROUGH) >= 0;
        if (dmt) trip = 3;
        if (trip >= 0) {
            for (int i = 0; i < 2 + trip * 2; i++) {
                double x = p.getX() + (r.nextDouble() - 0.5) * 12;
                double y = p.getEyeY() + (r.nextDouble() - 0.4) * 5;
                double z = p.getZ() + (r.nextDouble() - 0.5) * 12;
                mc.level.addParticle(TRIP_PARTICLES[r.nextInt(TRIP_PARTICLES.length)], x, y, z,
                        (r.nextDouble() - 0.5) * 0.05, 0.02, (r.nextDouble() - 0.5) * 0.05);
            }
            // sounds only you can hear, from somewhere around you
            if (r.nextInt(Math.max(50, 320 - trip * 80)) == 0) {
                var sounds = dmt ? List.of(ModSounds.MACHINE_ELF, ModSounds.TRIP_CHIME)
                        : List.of(ModSounds.TRIP_CHIME, ModSounds.TRIP_WHISPER, ModSounds.TRIP_WHISPER, ModSounds.TRIP_WHOOSH);
                local(mc, p, sounds.get(r.nextInt(sounds.size())).get(), 0.9f, 0.85f + r.nextFloat() * 0.3f, 6);
            }
            if (dmt && r.nextInt(45) == 0) local(mc, p, ModSounds.MACHINE_ELF.get(), 0.7f, 0.8f + r.nextFloat() * 0.5f, 4);
        }
        if (amp(p, ModEffects.SALVIA) >= 0 && r.nextInt(70) == 0) {
            local(mc, p, ModSounds.SALVIA_RUMBLE.get(), 1.0f, 0.8f + r.nextFloat() * 0.3f, 2);
        }

        // heartbeat: racing on stimulants, slow and heavy on the comedown
        int heartEvery = amp(p, ModEffects.SPEEDING) >= 0 || amp(p, ModEffects.WIRED) >= 0 ? 9
                : amp(p, ModEffects.COMEDOWN) >= 0 ? 24 : 0;
        if (heartEvery > 0 && p.tickCount % heartEvery == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), heartEvery < 12 ? 1.1f : 0.85f,
                    heartEvery < 12 ? 0.55f : 0.4f));
        }

        // looping "in your head" audio
        boolean rolling = amp(p, ModEffects.ROLLING) >= 0;
        if (rolling && (rave == null || rave.isStopped() || rave.isFadingOut())) {
            rave = startLoop(mc, ModSounds.MUSIC_RAVE.get(), SoundSource.RECORDS, 0.8f, ModEffects.ROLLING);
            raveStartTick = p.tickCount;
        }
        if (amp(p, ModEffects.K_HOLE) >= 0 && (drone == null || drone.isStopped() || drone.isFadingOut())) {
            drone = startLoop(mc, ModSounds.K_HOLE_DRONE.get(), SoundSource.AMBIENT, 0.8f, ModEffects.K_HOLE);
        }
        if (amp(p, ModEffects.GIGGLES) >= 0 && (wah == null || wah.isStopped() || wah.isFadingOut())) {
            wah = startLoop(mc, ModSounds.NITROUS_WAH.get(), SoundSource.AMBIENT, 0.6f, ModEffects.GIGGLES);
        }

        Hallucinations.tick(mc, p, trip, dmt);
    }

    private static EffectLoopSound rave, drone, wah;
    private static int raveStartTick;

    private static EffectLoopSound startLoop(Minecraft mc, net.minecraft.sounds.SoundEvent sound, SoundSource source, float vol,
                                             Holder<MobEffect> effect) {
        EffectLoopSound s = new EffectLoopSound(sound, source, vol, () -> {
            LocalPlayer pl = Minecraft.getInstance().player;
            return pl != null && pl.hasEffect(effect);
        });
        mc.getSoundManager().play(s);
        return s;
    }

    /** Ticks since the rave track started, so the screen pulse lands on the kick drum. */
    static float beatTime(LocalPlayer p, float partial) {
        return rave != null && !rave.isStopped() ? p.tickCount - raveStartTick + partial : p.tickCount + partial;
    }

    private static void local(Minecraft mc, LocalPlayer p, net.minecraft.sounds.SoundEvent sound, float vol, float pitch, double spread) {
        RandomSource r = p.getRandom();
        double x = p.getX() + (r.nextDouble() - 0.5) * spread * 2, z = p.getZ() + (r.nextDouble() - 0.5) * spread * 2;
        mc.level.playLocalSound(x, p.getEyeY() + (r.nextDouble() - 0.5) * 2, z, sound, SoundSource.AMBIENT, vol, pitch, false);
    }

    private ClientEffects() {}
}

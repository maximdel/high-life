package com.drunkgamblers.highlife.client;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Fake mobs that exist only in the tripping player's client world: floating pigs, a rainbow
 * sheep, fish swimming through the air, a creeper behind you that is never really there, and
 * "machine elves" circling you on DMT. Nothing is sent to the server, nobody else sees them.
 * Any error disables the feature for the session instead of crashing the game.
 */
public final class Hallucinations {
    private enum Kind { FLOATING_PIG, JEB_SHEEP, AIR_FISH, PARROT_PARTY, CREEPER_SCARE, MACHINE_ELF }

    private static final class Fake {
        final Entity entity;
        final Kind kind;
        final Vec3 anchor;
        final double phase;
        int age;
        final int life;

        Fake(Entity entity, Kind kind, Vec3 anchor, double phase, int life) {
            this.entity = entity;
            this.kind = kind;
            this.anchor = anchor;
            this.phase = phase;
            this.life = life;
        }
    }

    private static final List<Fake> FAKES = new ArrayList<>();
    private static int nextId = -880_000;
    private static boolean disabled;
    private static ClientLevel level;

    public static void tick(Minecraft mc, LocalPlayer p, int trip, boolean dmt) {
        if (disabled) return;
        try {
            if (level != mc.level) { // changed dimension / world: old fakes are gone with the old level
                FAKES.clear();
                level = mc.level;
            }
            if (trip < 0) { // sobered up: everything fades away
                if (!FAKES.isEmpty()) clear();
                return;
            }
            update(p);
            RandomSource r = p.getRandom();
            int max = dmt ? 6 : 1 + trip;
            int chance = dmt ? 12 : Math.max(80, 400 - trip * 110); // 1 in N ticks
            if (trip >= 0 && FAKES.size() < max && r.nextInt(chance) == 0) {
                spawn(mc, p, dmt ? Kind.MACHINE_ELF : pick(r));
            }
        } catch (Throwable t) {
            disabled = true;
            HighLife.LOGGER.warn("High Life hallucinations disabled after an error (game is fine)", t);
            clear();
        }
    }

    private static Kind pick(RandomSource r) {
        Kind[] k = {Kind.FLOATING_PIG, Kind.JEB_SHEEP, Kind.AIR_FISH, Kind.AIR_FISH, Kind.PARROT_PARTY, Kind.CREEPER_SCARE};
        return k[r.nextInt(k.length)];
    }

    private static void spawn(Minecraft mc, LocalPlayer p, Kind kind) {
        EntityType<?> type = switch (kind) {
            case FLOATING_PIG -> EntityType.PIG;
            case JEB_SHEEP -> EntityType.SHEEP;
            case AIR_FISH -> EntityType.TROPICAL_FISH;
            case PARROT_PARTY -> EntityType.PARROT;
            case CREEPER_SCARE -> EntityType.CREEPER;
            case MACHINE_ELF -> p.getRandom().nextBoolean() ? EntityType.ALLAY : EntityType.VEX;
        };
        Entity e = type.create(mc.level);
        if (e == null) return;
        RandomSource r = p.getRandom();
        Vec3 look = p.getLookAngle();
        Vec3 anchor;
        int life;
        if (kind == Kind.CREEPER_SCARE) {
            anchor = p.position().subtract(look.x * 2.2, 0, look.z * 2.2); // right behind you
            life = 30;
        } else {
            double a = r.nextDouble() * Math.PI * 2, d = 3 + r.nextDouble() * 5;
            anchor = p.position().add(Math.cos(a) * d, 1.5 + r.nextDouble() * 2.5, Math.sin(a) * d);
            life = kind == Kind.MACHINE_ELF ? 160 + r.nextInt(80) : 200 + r.nextInt(200);
        }
        e.setId(nextId--);
        e.setNoGravity(true);
        e.setSilent(kind != Kind.CREEPER_SCARE && kind != Kind.PARROT_PARTY);
        e.setPos(anchor.x, anchor.y, anchor.z);
        if (kind == Kind.JEB_SHEEP) e.setCustomName(Component.literal("jeb_")); // vanilla rainbow wool
        if (e instanceof LivingEntity le) le.setInvulnerable(true);
        mc.level.addEntity(e);
        FAKES.add(new Fake(e, kind, anchor, r.nextDouble() * Math.PI * 2, life));
        poof(anchor);
        if (kind == Kind.CREEPER_SCARE) {
            mc.level.playLocalSound(anchor.x, anchor.y, anchor.z, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.0f, 0.5f, false);
        } else if (kind == Kind.MACHINE_ELF) {
            mc.level.playLocalSound(anchor.x, anchor.y, anchor.z, ModSounds.MACHINE_ELF.get(), SoundSource.AMBIENT, 0.8f, 1.0f, false);
        }
    }

    private static void update(LocalPlayer p) {
        Iterator<Fake> it = FAKES.iterator();
        while (it.hasNext()) {
            Fake f = it.next();
            f.age++;
            Entity e = f.entity;
            if (f.age >= f.life || e.isRemoved()) {
                vanish(f);
                it.remove();
                continue;
            }
            double t = f.age + f.phase * 20;
            Vec3 pos = switch (f.kind) {
                case FLOATING_PIG, JEB_SHEEP -> f.anchor.add(Math.sin(t * 0.05) * 1.5, Math.sin(t * 0.08) * 0.6, Math.cos(t * 0.05) * 1.5);
                case AIR_FISH -> f.anchor.add(Math.sin(t * 0.07) * 3, Math.sin(t * 0.13) * 0.8, Math.cos(t * 0.07) * 3);
                case PARROT_PARTY -> f.anchor.add(0, Math.abs(Math.sin(t * 0.3)) * 0.6, 0);
                case CREEPER_SCARE -> f.anchor;
                case MACHINE_ELF -> {
                    double a = t * 0.09 + f.phase, rad = 2.5 + Math.sin(t * 0.05) * 0.8;
                    yield p.position().add(Math.cos(a) * rad, 1.2 + Math.sin(t * 0.2) * 0.7, Math.sin(a) * rad);
                }
            };
            Vec3 old = e.position();
            e.setPos(pos.x, pos.y, pos.z);
            Vec3 d = pos.subtract(old);
            float yaw = f.kind == Kind.CREEPER_SCARE || d.horizontalDistanceSqr() < 1e-4
                    ? (float) (Mth.atan2(p.getZ() - pos.z, p.getX() - pos.x) * Mth.RAD_TO_DEG) - 90f
                    : (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            e.setYRot(yaw);
            if (e instanceof LivingEntity le) {
                le.yBodyRot = yaw;
                le.yHeadRot = yaw;
            }
        }
    }

    private static void poof(Vec3 at) {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null) return;
        for (int i = 0; i < 8; i++) {
            l.addParticle(ParticleTypes.WITCH, at.x + (Math.random() - 0.5), at.y + Math.random(), at.z + (Math.random() - 0.5), 0, 0.05, 0);
        }
    }

    private static void vanish(Fake f) {
        ClientLevel l = Minecraft.getInstance().level;
        if (l != null && l == level && !f.entity.isRemoved()) {
            poof(f.entity.position());
            l.removeEntity(f.entity.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    /** Self-test helper: spawn one of each kind right away. */
    static void debugSpawnAll(Minecraft mc, LocalPlayer p) {
        for (Kind k : Kind.values()) spawn(mc, p, k);
    }

    static int count() {
        return FAKES.size();
    }

    public static void clear() {
        for (Fake f : FAKES) vanish(f);
        FAKES.clear();
    }

    private Hallucinations() {}
}

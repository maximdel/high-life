package com.drunkgamblers.highlife.client;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Dev-only visual check, enabled with -Dhighlife.selftest=true (gradlew runSelftest).
 * Cycles through every effect in a singleplayer world, takes screenshots and quits.
 */
public final class ClientSelfTest {
    private record Phase(String name, Holder<MobEffect> effect, int amp) {}

    private static final List<Phase> PHASES = List.of(
            new Phase("high", ModEffects.HIGH, 2), new Phase("tripping", ModEffects.TRIPPING, 1),
            new Phase("acid", ModEffects.ACID, 1), new Phase("salvia", ModEffects.SALVIA, 0),
            new Phase("speeding", ModEffects.SPEEDING, 0), new Phase("rolling", ModEffects.ROLLING, 0),
            new Phase("k_hole", ModEffects.K_HOLE, 0), new Phase("giggles", ModEffects.GIGGLES, 0),
            new Phase("breakthrough", ModEffects.BREAKTHROUGH, 0), new Phase("comedown", ModEffects.COMEDOWN, 0));
    private static final int PHASE_TICKS = 120;
    private static int ticks = -100; // let the world settle first

    public static void initIfEnabled() {
        if (Boolean.getBoolean("highlife.selftest")) NeoForge.EVENT_BUS.addListener(ClientSelfTest::tick);
    }

    private static void server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) action.accept(sp);
        });
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        if (ticks < 0) return;
        int idx = ticks / PHASE_TICKS, at = ticks % PHASE_TICKS;
        if (idx >= PHASES.size()) {
            if (at == 20) {
                HighLife.LOGGER.info("[selftest] DONE, hallucinations alive: {}", Hallucinations.count());
                mc.stop();
            }
            return;
        }
        Phase ph = PHASES.get(idx);
        if (at == 0) {
            HighLife.LOGGER.info("[selftest] phase {}", ph.name());
            server(mc, sp -> {
                sp.removeAllEffects();
                sp.addEffect(new MobEffectInstance(ph.effect(), ph.name().equals("breakthrough") ? 600 : 2400, ph.amp()));
            });
        }
        if (at == 5 && (ph.name().equals("acid") || ph.name().equals("breakthrough"))) Hallucinations.debugSpawnAll(mc, mc.player);
        if (at == 25 && ph.name().equals("breakthrough")) shot(mc, "breakthrough_flash");
        if (at == 90) shot(mc, ph.name());
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "highlife_" + name + ".png", mc.getMainRenderTarget(),
                c -> HighLife.LOGGER.info("[selftest] screenshot {}", name));
    }

    private ClientSelfTest() {}
}

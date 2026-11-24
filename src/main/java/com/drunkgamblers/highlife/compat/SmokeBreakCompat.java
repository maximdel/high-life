package com.drunkgamblers.highlife.compat;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.effect.Highs;
import com.smokebreak.device.DeviceLogic;
import com.smokebreak.device.LiquidHolder;
import com.smokebreak.device.LiquidTank;
import com.smokebreak.liquid.LiquidContents;
import com.smokebreak.liquid.VapeLiquid;
import com.smokebreak.registry.ModLiquids;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Smoke Break's e-liquids are cosmetic, so we watch vapes in players' hands: when the
 * Cannabis Distillate level in a device drops, the player just took a puff and gets high.
 */
public final class SmokeBreakCompat {
    public static final ResourceKey<VapeLiquid> CANNABIS_DISTILLATE =
            ResourceKey.create(ModLiquids.REGISTRY_KEY, HighLife.id("cannabis_distillate"));

    private record Seen(int mainAmount, int offAmount, long lastHighTick) {}
    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    public static ItemStack distillateBottle() {
        return LiquidTank.createBottle(CANNABIS_DISTILLATE, LiquidTank.BOTTLE_CAPACITY);
    }

    /** Amount of cannabis distillate in a vape device, or -1 if it is not a vape holding it. */
    private static int distillateIn(ItemStack stack) {
        if (stack.isEmpty() || DeviceLogic.spec(stack).isEmpty()) return -1;
        Optional<LiquidHolder> holder = DeviceLogic.liquidHolder(stack);
        if (holder.isEmpty()) holder = DeviceLogic.cartridgeHolder(stack);
        return holder.flatMap(LiquidHolder::contents)
                .filter(c -> !c.isMix() && c.liquid().equals(CANNABIS_DISTILLATE))
                .map(LiquidContents::amount)
                .orElse(-1);
    }

    public static void tickPlayer(Player player) {
        int main = distillateIn(player.getItemInHand(InteractionHand.MAIN_HAND));
        int off = distillateIn(player.getItemInHand(InteractionHand.OFF_HAND));
        UUID id = player.getUUID();
        Seen before = SEEN.get(id);
        long now = player.level().getGameTime();
        if (main < 0 && off < 0) {
            if (before != null) SEEN.remove(id);
            return;
        }
        long lastHigh = before == null ? 0 : before.lastHighTick();
        if (before != null && now - lastHigh > 15
                && (dropped(before.mainAmount(), main) || dropped(before.offAmount(), off))) {
            Highs.addHigh(player, 1, 1800);
            lastHigh = now;
        }
        SEEN.put(id, new Seen(main, off, lastHigh));
    }

    private static boolean dropped(int before, int now) {
        return before > 0 && now >= 0 && now < before;
    }

    public static void forget(Player player) {
        SEEN.remove(player.getUUID());
    }

    private SmokeBreakCompat() {}
}

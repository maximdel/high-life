package com.drunkgamblers.highlife.registry;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.compat.SmokeBreakCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HighLife.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.highlife"))
            .icon(() -> new ItemStack(ModItems.JOINT.get()))
            .displayItems((params, out) -> {
                ModItems.ITEMS.getEntries().forEach(e -> out.accept(e.get()));
                out.accept(SmokeBreakCompat.distillateBottle());
            })
            .build());

    private ModTabs() {}
}

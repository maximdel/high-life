package com.drunkgamblers.highlife;

import com.drunkgamblers.highlife.client.ClientEffects;
import com.drunkgamblers.highlife.registry.ModBlocks;
import com.drunkgamblers.highlife.registry.ModEffects;
import com.drunkgamblers.highlife.registry.ModItems;
import com.drunkgamblers.highlife.registry.ModTabs;
import com.drunkgamblers.highlife.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(HighLife.MODID)
public class HighLife {
    public static final String MODID = "highlife";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public HighLife(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModTabs.TABS.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        NeoForge.EVENT_BUS.register(GameEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEffects.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

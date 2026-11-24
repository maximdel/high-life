package com.drunkgamblers.highlife.registry;

import com.drunkgamblers.highlife.HighLife;
import com.drunkgamblers.highlife.block.HerbCropBlock;
import com.drunkgamblers.highlife.block.MagicMushroomBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HighLife.MODID);

    public static final DeferredBlock<HerbCropBlock> CANNABIS = BLOCKS.register("cannabis",
            () -> new HerbCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), ModItems.CANNABIS_SEEDS));
    public static final DeferredBlock<HerbCropBlock> TOBACCO = BLOCKS.register("tobacco",
            () -> new HerbCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), ModItems.TOBACCO_SEEDS));
    public static final DeferredBlock<HerbCropBlock> COCA = BLOCKS.register("coca",
            () -> new HerbCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), ModItems.COCA_SEEDS));

    public static final DeferredBlock<HerbCropBlock> SALVIA = BLOCKS.register("salvia",
            () -> new HerbCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), ModItems.SALVIA_SEEDS));

    public static final DeferredBlock<MagicMushroomBlock> MAGIC_MUSHROOM = BLOCKS.register("magic_mushroom",
            () -> new MagicMushroomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).lightLevel(s -> 3)));

    private ModBlocks() {}
}

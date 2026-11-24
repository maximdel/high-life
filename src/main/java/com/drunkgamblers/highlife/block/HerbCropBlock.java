package com.drunkgamblers.highlife.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

/** A wheat-like crop (8 ages) whose seeds item is supplied lazily. */
public class HerbCropBlock extends CropBlock {
    private static final MapCodec<HerbCropBlock> CODEC = simpleCodec(p -> new HerbCropBlock(p, () -> Items.WHEAT_SEEDS));
    private final Supplier<? extends Item> seeds;

    public HerbCropBlock(Properties properties, Supplier<? extends Item> seeds) {
        super(properties);
        this.seeds = seeds;
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seeds.get();
    }
}

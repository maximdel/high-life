package com.drunkgamblers.highlife.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Small glowing mushroom: grows on dirt-like ground in shade and slowly spreads in the dark. */
public class MagicMushroomBlock extends BushBlock {
    public static final MapCodec<MagicMushroomBlock> CODEC = simpleCodec(MagicMushroomBlock::new);
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0);

    public MagicMushroomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL) || state.is(Blocks.MOSS_BLOCK);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        // like vanilla mushrooms: mycelium/podzol anywhere, other ground only in shade
        if (ground.is(Blocks.MYCELIUM) || ground.is(Blocks.PODZOL)) return true;
        return level.getRawBrightness(pos, 0) < 13 && mayPlaceOn(ground, level, below);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(25) != 0) return;
        int nearby = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4))) {
            if (level.getBlockState(p).is(this) && ++nearby >= 5) return;
        }
        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
        if (level.isEmptyBlock(target) && state.canSurvive(level, target)) {
            level.setBlock(target, state, 2);
        }
    }
}

package dev.ftbqauto.block;

import com.mojang.serialization.MapCodec;
import dev.ftbqauto.block.entity.RewardClaimerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 奖励领取器。
 *
 * ACTIVE 只表示"刚领到东西"，用来驱动正面贴图的发光变体。
 */
public class RewardClaimerBlock extends BoundBlock {

    public static final MapCodec<RewardClaimerBlock> CODEC = simpleCodec(RewardClaimerBlock::new);
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public RewardClaimerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RewardClaimerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, dev.ftbqauto.registry.ModBlockEntities.REWARD_CLAIMER.get(),
                RewardClaimerBlockEntity::serverTick);
    }
}

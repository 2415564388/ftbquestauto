package dev.ftbqauto.block;

import com.mojang.serialization.MapCodec;
import dev.ftbqauto.block.entity.QuestSubmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 任务提交器。
 *
 * POWERED 既驱动正面贴图的发光变体，也用来做<b>上升沿</b>检测：
 * 只有当信号从无到有时才提交一次（玩家选的行为），
 * 而不是高电平期间每 tick 反复尝试。
 */
public class QuestSubmitterBlock extends BoundBlock {

    public static final MapCodec<QuestSubmitterBlock> CODEC = simpleCodec(QuestSubmitterBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public QuestSubmitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuestSubmitterBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
            if (powered && level.getBlockEntity(pos) instanceof QuestSubmitterBlockEntity submitter) {
                submitter.submitAll();
            }
        }
    }

    /** 放置时如果脚下已经有信号，记下状态但<b>不</b>触发提交（避免放下去就吞东西）。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        }
    }
}

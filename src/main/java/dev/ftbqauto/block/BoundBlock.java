package dev.ftbqauto.block;

import dev.ftbqauto.block.entity.BoundContainerBlockEntity;
import dev.ftbqauto.item.QuestBinderItem;
import dev.ftbqauto.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 两个方块共用的交互逻辑。
 *
 * - 手里拿着 FTB 任务绑定器 → 走绑定流程
 * - 空手 / 杂项手持物 → 打开容器界面
 * - 手持方块且未潜行 → 交还给原版处理放置，别抢掉叠放操作
 */
public abstract class BoundBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected BoundBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.QUEST_BINDER.get())) {
            return QuestBinderItem.bindToBlock(level, pos, player, stack);
        }
        if (stack.getItem() instanceof BlockItem && !player.isSecondaryUseActive()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return openContainer(level, pos, player).consumesAction()
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return openContainer(level, pos, player);
    }

    protected InteractionResult openContainer(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof BoundContainerBlockEntity container) {
                serverPlayer.openMenu(container, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * 破坏时把绑定写进掉落物，拆了重放不用重新绑。
     * 走 getDrops 而不是自己 dropItem，是为了不绕过战利品表。
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BoundContainerBlockEntity bound
                && bound.getBoundId() != 0L) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("BoundId", bound.getBoundId());
            if (bound.getOwner() != null) {
                tag.putUUID("Owner", bound.getOwner());
            }
            if (!bound.getOwnerName().isEmpty()) {
                tag.putString("OwnerName", bound.getOwnerName());
            }
            for (ItemStack drop : drops) {
                if (drop.is(asItem())) {
                    BlockItem.setBlockEntityData(drop, bound.getType(), tag);
                }
            }
        }
        return drops;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof BoundContainerBlockEntity container) {
            net.minecraft.world.Containers.dropContents(level, pos, container);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BoundContainerBlockEntity container
                ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(container)
                : 0;
    }
}

package dev.ftbqauto.block.entity;

import dev.ftbqauto.block.RewardClaimerBlock;
import dev.ftbqauto.registry.ModBlockEntities;
import dev.ftbqauto.registry.ModMenus;
import dev.ftbqauto.util.QuestHelper;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 奖励领取器。
 *
 * 定期检查绑定任务是否已完成且还有未领取的奖励，有就领到自己的库存里。
 * 物品奖励进库存，其余（经验/命令/随机型…）由 FTB 的
 * {@code automatedClaimPost} 默认实现发给绑定玩家。
 */
public class RewardClaimerBlockEntity extends BoundContainerBlockEntity {

    /** 检查间隔（tick）。2 秒一次，足够及时又不至于每 tick 都去翻任务数据。 */
    private static final int CHECK_INTERVAL = 40;

    public RewardClaimerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REWARD_CLAIMER.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.ftbqauto.reward_claimer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(ModMenus.REWARD_CLAIMER.get(), id, inventory, this, 3);
    }

    @Override
    public boolean acceptsBinding(Level level, long id) {
        return QuestHelper.asQuest(level, id) != null;
    }

    @Override
    public Component bindingRejected() {
        return Component.translatable("gui.ftbqauto.needs_quest");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RewardClaimerBlockEntity be) {
        if (level.getGameTime() % CHECK_INTERVAL != 0L) {
            return;
        }

        boolean claimedSomething = false;

        if (be.isBound()) {
            Quest quest = QuestHelper.asQuest(level, be.getBoundId());
            TeamData data = quest == null ? null : QuestHelper.teamData(level, be.getOwner());

            if (quest != null && data != null
                    && QuestHelper.hasClaimableRewards(quest, data, be.getOwner())) {
                ServerPlayer player = QuestHelper.onlinePlayer(level, be.getOwner());
                int claimed = QuestHelper.claimRewards(quest, data, be.getOwner(), player, be, be, level.random);
                if (claimed > 0) {
                    be.setChanged();
                    claimedSomething = true;
                }
            }
        }

        // ACTIVE 状态最多亮一个检查周期，用来驱动正面贴图的发光变体
        if (state.getValue(RewardClaimerBlock.ACTIVE) != claimedSomething) {
            level.setBlock(pos, state.setValue(RewardClaimerBlock.ACTIVE, claimedSomething), 3);
        }
    }
}

package dev.ftbqauto.block.entity;

import dev.ftbqauto.registry.ModBlockEntities;
import dev.ftbqauto.registry.ModMenus;
import dev.ftbqauto.util.QuestHelper;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 任务提交器。
 *
 * 绑定一个任务（Quest），红石上升沿时把箱内所有能提交的物品一次性提交掉，
 * 覆盖该任务下的<b>所有</b>物品子任务。提交多少扣多少，剩下的留在箱里。
 */
public class QuestSubmitterBlockEntity extends BoundContainerBlockEntity {

    public QuestSubmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUEST_SUBMITTER.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.ftbqauto.quest_submitter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new ChestMenu(ModMenus.QUEST_SUBMITTER.get(), id, inventory, this, 3);
    }

    @Override
    public boolean acceptsBinding(Level level, long id) {
        return QuestHelper.asQuest(level, id) != null;
    }

    @Override
    public Component bindingRejected() {
        return Component.translatable("gui.ftbqauto.needs_quest");
    }

    /** 红石上升沿调用。 */
    public void submitAll() {
        if (level == null || level.isClientSide || !isBound()) {
            return;
        }

        Quest quest = QuestHelper.asQuest(level, getBoundId());
        if (quest == null) {
            return;
        }

        TeamData data = QuestHelper.teamData(level, getOwner());
        if (data == null) {
            return;
        }

        if (QuestHelper.submitToQuest(quest, data, this) > 0) {
            setChanged();
        }
    }
}

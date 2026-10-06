package dev.ftbqauto.util;

import dev.ftb.mods.ftbquests.api.FTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.BaseQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.AllTableReward;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.LootReward;
import dev.ftb.mods.ftbquests.quest.reward.RandomReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FTB Quests 的对接层，把附属模组要用的几个操作收在一处。
 *
 * 用到的 FTB 内部细节（已对照 2101.1.33 源码确认）：
 * - {@code ItemTask.insert(...)} 才是"从任意容器提交物品"的原语。
 *   {@code ItemTask.submitTask(...)} 扣的是<b>玩家背包</b>，方块用不了。
 * - {@code Reward.automatedClaimPre/Post} 是 FTB 给"方块自动领奖"预留的钩子
 *   （它自家的战利品箱开启器就走这条）。默认行为正好是我们要的：
 *   物品塞进传入的 items 列表（方块自己收），其余走 claim(player) 发给玩家。
 */
public final class QuestHelper {

    private QuestHelper() {
    }

    @Nullable
    public static BaseQuestFile file(Level level) {
        try {
            return FTBQuestsAPI.api().getQuestFile(level.isClientSide());
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static QuestObjectBase resolve(Level level, long id) {
        if (id == 0L) return null;
        BaseQuestFile f = file(level);
        return f == null ? null : f.getBase(id);
    }

    /**
     * 把绑定玩家的队伍进度取出来。
     * 绑定存的是玩家 UUID（玩家自己选的行为），队伍在运行时解析，
     * 这样玩家离线时方块照样能工作。
     */
    @Nullable
    public static TeamData teamData(Level level, @Nullable UUID playerId) {
        if (level.isClientSide() || playerId == null) return null;
        BaseQuestFile f = file(level);
        if (f == null) return null;
        return FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
                .map(team -> f.getNullableTeamData(team.getId()))
                .orElse(null);
    }

    @Nullable
    public static ServerPlayer onlinePlayer(Level level, @Nullable UUID playerId) {
        if (level.isClientSide() || playerId == null) return null;
        MinecraftServer server = level.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(playerId);
    }

    // ------------------------------------------------------------------
    // 提交
    // ------------------------------------------------------------------

    /**
     * 把一个容器里能提交的物品全提交给指定的物品任务。
     *
     * @return 实际消耗掉的物品总数（检测型任务恒为 0，因为不扣物品）
     */
    public static int submitContainer(ItemTask task, TeamData data, Container container) {
        // 任务还没解锁就别收东西 —— 否则物品被吞、进度记在一个锁着的任务上。
        // FTB 自己的任务屏幕也是靠这个判断过滤可绑定任务的。
        if (!data.canStartTasks(task.getQuest())) {
            return 0;
        }

        if (!task.consumesResources()) {
            // 检测型任务不消耗物品，进度含义是"你拥有多少个"。
            // 对方块的语义就是：把库存里匹配物品的总数报上去。
            long total = 0L;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && task.test(stack)) {
                    total += stack.getCount();
                }
            }
            long capped = Math.min(total, task.getMaxProgress());
            if (capped > data.getProgress(task)) {
                data.setProgress(task, capped);
            }
            return 0;
        }

        int consumed = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            // insert() 会自己加进度，并返回扣减后的余量（没动过就返回同一个引用）
            ItemStack remainder = task.insert(data, stack, false);
            if (remainder != stack) {
                consumed += stack.getCount() - remainder.getCount();
                container.setItem(i, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            }
            if (data.isCompleted(task)) break;
        }
        return consumed;
    }

    /**
     * 把一个容器里的物品提交给任务下<b>所有</b>物品子任务。
     *
     * 顺序遍历即可：前一个子任务消耗掉的量会从容器里扣掉，
     * 下一个子任务看到的自然就是剩下的。
     */
    public static int submitToQuest(Quest quest, TeamData data, Container container) {
        int consumed = 0;
        for (Task task : quest.getTasksAsList()) {
            if (task instanceof ItemTask itemTask) {
                consumed += submitContainer(itemTask, data, container);
            }
        }
        return consumed;
    }

    // ------------------------------------------------------------------
    // 领奖
    // ------------------------------------------------------------------

    /** 任务是否已完成、且还有没领的奖励。 */
    public static boolean hasClaimableRewards(Quest quest, TeamData data, UUID ownerId) {
        if (!data.isCompleted(quest)) return false;
        for (Reward reward : quest.getRewards()) {
            if (!data.isRewardClaimed(ownerId, reward)) return true;
        }
        return false;
    }

    /** 嵌套解析的最大层数，防"表里套表"绕不出来。 */
    private static final int MAX_RESOLVE_DEPTH = 4;

    /**
     * 一条奖励展开后的结果：要进箱子的物品，以及处理完之后需要调
     * {@code automatedClaimPost} 的奖励（真正发放经验/命令等的地方）。
     */
    private record ClaimPlan(List<ItemStack> items, List<Reward> posters) {
        static final ClaimPlan EMPTY = new ClaimPlan(List.of(), List.of());

        boolean isEmpty() {
            return items.isEmpty() && posters.isEmpty();
        }
    }

    /**
     * 把任务下所有未领取的奖励领掉。
     *
     * @return 本次领掉的奖励条数
     */
    public static int claimRewards(Quest quest, TeamData data, UUID ownerId,
                                   @Nullable ServerPlayer player, BlockEntity source,
                                   Container container, RandomSource random) {
        int claimed = 0;
        for (Reward reward : quest.getRewards()) {
            if (data.isRewardClaimed(ownerId, reward)) continue;

            // 选择型奖励的 Reward.claim() 是个空方法（必须玩家在界面里点选）。
            // 要是在这里标记成已领取，奖励就凭空消失了 —— 跳过，留给他手动领。
            // 用 instanceof 而不是 getType() == RewardTypes.CHOICE，
            // 这样服务端完全不用碰 RewardTypes 那个引用了 Icon 的类。
            if (reward instanceof ChoiceReward) continue;

            if (claimOne(reward, data, ownerId, player, source, container, random)) {
                claimed++;
            }
        }
        return claimed;
    }

    private static boolean claimOne(Reward reward, TeamData data, UUID ownerId,
                                    @Nullable ServerPlayer player, BlockEntity source,
                                    Container container, RandomSource random) {
        ClaimPlan plan = plan(reward, player, source, ownerId, random, 0);

        // 展开不出任何东西（比如随机奖励但玩家离线，摇不了），留着下次
        if (plan.isEmpty()) return false;

        // 先确认物品塞得下再标记已领取，否则奖励会被吞掉
        if (!fitsAll(container, plan.items())) return false;
        if (!data.markRewardAsClaimed(ownerId, reward, System.currentTimeMillis())) return false;

        for (ItemStack stack : plan.items()) {
            ItemStack leftover = insert(container, stack);
            if (!leftover.isEmpty() && player != null) {
                // 前面模拟过，理论上到不了这里；真到了就丢给玩家，总比消失强
                player.getInventory().placeItemBackInInventory(leftover);
            }
        }

        // 物品奖励的 post 是空实现（东西已经进箱子了，不会重复发）；
        // 其余的默认实现是 claim(player)，正好把经验/命令等发给绑定玩家。
        for (Reward poster : plan.posters()) {
            poster.automatedClaimPost(source, ownerId, player);
        }
        return true;
    }

    /**
     * 展开一条奖励会产出什么。
     *
     * <b>纯查询，没有副作用</b> —— {@code automatedClaimPre} 只是往 items 里塞东西，
     * {@code automatedClaimPost} 才是真正发放。所以可以放心地"先算一遍、塞不下就整条放弃"。
     */
    private static ClaimPlan plan(Reward reward, @Nullable ServerPlayer player, BlockEntity source,
                                  UUID ownerId, RandomSource random, int depth) {
        List<ItemStack> items = new ArrayList<>();
        if (reward.automatedClaimPre(source, items, random, ownerId, player)) {
            return new ClaimPlan(items, List.of(reward));
        }

        // 走到这里说明方块吸收不了这条奖励。目前只有随机奖励/战利品箱/整表奖励会返回 false
        // （FTB 硬编码的）—— 它们本身不是奖励，而是"抽奖容器"。
        // 必须把摇出来的具体奖励接过来逐个展开，否则 RandomReward.claim() 会把物品
        // 直接塞进玩家背包（这正是"奖励进了玩家背包"的原因）。
        if (depth >= MAX_RESOLVE_DEPTH) return ClaimPlan.EMPTY;

        List<ItemStack> allItems = new ArrayList<>();
        List<Reward> allPosters = new ArrayList<>();
        for (Reward concrete : resolveTable(reward, player)) {
            ClaimPlan sub = plan(concrete, player, source, ownerId, random, depth + 1);
            allItems.addAll(sub.items());
            allPosters.addAll(sub.posters());
        }
        return new ClaimPlan(allItems, allPosters);
    }

    /** 把"抽奖容器"类奖励摇成具体奖励：整表奖励全取，随机/战利品箱摇一次。 */
    private static List<Reward> resolveTable(Reward reward, @Nullable ServerPlayer player) {
        if (!(reward instanceof RandomReward container)) {
            return List.of();
        }

        RewardTable table = container.getTable();
        if (table == null) {
            return List.of();
        }

        if (reward instanceof AllTableReward) {
            return table.getWeightedRewards().stream().map(WeightedReward::getReward).toList();
        }

        // 随机型和战利品箱型都需要玩家才能摇 —— FTB 自己用的是 player.getRandom()，
        // 这里保持一致，抽出来的结果和原版行为同分布。
        if (player == null) {
            return List.of();
        }

        boolean loot = reward instanceof LootReward;
        return table.generateWeightedRandomRewards(player.getRandom(), 1, loot)
                .stream().map(WeightedReward::getReward).toList();
    }

    // ------------------------------------------------------------------
    // 容器读写（带模拟，用于"塞得下才领"）
    // ------------------------------------------------------------------

    private static boolean fitsAll(Container container, List<ItemStack> toAdd) {
        if (toAdd.isEmpty()) return true;

        int slots = container.getContainerSize();
        List<ItemStack> snapshot = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            snapshot.add(container.getItem(i).copy());
        }

        for (ItemStack stack : toAdd) {
            if (!mergeInto(container, snapshot, stack.copy()).isEmpty()) return false;
        }
        return true;
    }

    /** 真正写入容器，返回没塞下的余量。 */
    public static ItemStack insert(Container container, ItemStack stack) {
        int slots = container.getContainerSize();
        List<ItemStack> current = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            current.add(container.getItem(i));
        }
        ItemStack leftover = mergeInto(container, current, stack.copy());
        for (int i = 0; i < slots; i++) {
            container.setItem(i, current.get(i));
        }
        return leftover;
    }

    /**
     * 把 stack 并进 slots（原地修改）。返回没塞下的余量。
     * 传入的 slots 可以是真实槽位列表，也可以是快照（用于模拟）。
     */
    private static ItemStack mergeInto(Container container, List<ItemStack> slots, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        // 先并进同类已有堆
        for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
            ItemStack existing = slots.get(i);
            if (existing.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
            if (!container.canPlaceItem(i, stack)) continue;

            int limit = Math.min(container.getMaxStackSize(), existing.getMaxStackSize());
            int space = limit - existing.getCount();
            if (space <= 0) continue;

            int moved = Math.min(space, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
        }

        // 再找空槽
        for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
            if (!slots.get(i).isEmpty()) continue;
            if (!container.canPlaceItem(i, stack)) continue;

            int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
            int moved = Math.min(limit, stack.getCount());
            slots.set(i, stack.copyWithCount(moved));
            stack.shrink(moved);
        }

        return stack;
    }

    /** 任务/任务的显示名，取不到就返回 null。 */
    @Nullable
    public static String displayName(Level level, long id) {
        QuestObjectBase obj = resolve(level, id);
        return obj == null ? null : obj.getTitle().getString();
    }

    @Nullable
    public static Task asTask(Level level, long id) {
        return resolve(level, id) instanceof Task task ? task : null;
    }

    @Nullable
    public static Quest asQuest(Level level, long id) {
        return resolve(level, id) instanceof Quest quest ? quest : null;
    }
}

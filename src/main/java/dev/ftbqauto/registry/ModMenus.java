package dev.ftbqauto.registry;

import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.block.entity.QuestSubmitterBlockEntity;
import dev.ftbqauto.block.entity.RewardClaimerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 两个方块都用原版的三行箱子界面 —— 布局玩家最熟悉，
 * 也省掉了一整套自定义 Menu / Screen / GUI 贴图。
 * 绑定信息通过容器标题显示（见各 BlockEntity#getDisplayName）。
 */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FTBQuestAutomation.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> QUEST_SUBMITTER =
            MENUS.register("quest_submitter", () -> IMenuTypeExtension.create((id, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level().getBlockEntity(pos);
                Container container = be instanceof QuestSubmitterBlockEntity submitter
                        ? submitter : new SimpleContainer(QuestSubmitterBlockEntity.SIZE);
                return new ChestMenu(ModMenus.QUEST_SUBMITTER.get(), id, inv, container, 3);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> REWARD_CLAIMER =
            MENUS.register("reward_claimer", () -> IMenuTypeExtension.create((id, inv, data) -> {
                BlockPos pos = data.readBlockPos();
                BlockEntity be = inv.player.level().getBlockEntity(pos);
                Container container = be instanceof RewardClaimerBlockEntity claimer
                        ? claimer : new SimpleContainer(RewardClaimerBlockEntity.SIZE);
                return new ChestMenu(ModMenus.REWARD_CLAIMER.get(), id, inv, container, 3);
            }));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}

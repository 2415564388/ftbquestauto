package dev.ftbqauto.registry;

import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.item.QuestBinderItem;
import dev.ftbqauto.item.TooltipBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FTBQuestAutomation.MOD_ID);

    public static final DeferredItem<BlockItem> QUEST_SUBMITTER = ITEMS.registerItem(
            "quest_submitter",
            properties -> new TooltipBlockItem(ModBlocks.QUEST_SUBMITTER.get(), properties,
                    "tooltip.ftbqauto.quest_submitter.1",
                    "tooltip.ftbqauto.quest_submitter.2",
                    "tooltip.ftbqauto.quest_submitter.3"),
            new Item.Properties());

    public static final DeferredItem<BlockItem> REWARD_CLAIMER = ITEMS.registerItem(
            "reward_claimer",
            properties -> new TooltipBlockItem(ModBlocks.REWARD_CLAIMER.get(), properties,
                    "tooltip.ftbqauto.reward_claimer.1",
                    "tooltip.ftbqauto.reward_claimer.2",
                    "tooltip.ftbqauto.reward_claimer.3"),
            new Item.Properties());

    public static final DeferredItem<QuestBinderItem> QUEST_BINDER = ITEMS.registerItem(
            "quest_binder",
            QuestBinderItem::new,
            new Item.Properties().stacksTo(1));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}

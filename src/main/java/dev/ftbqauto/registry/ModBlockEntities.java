package dev.ftbqauto.registry;

import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.block.entity.QuestSubmitterBlockEntity;
import dev.ftbqauto.block.entity.RewardClaimerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FTBQuestAutomation.MOD_ID);

    // 第三个参数是 DataFixer 类型，自定义方块实体传 null 即可
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QuestSubmitterBlockEntity>> QUEST_SUBMITTER =
            BLOCK_ENTITIES.register("quest_submitter", () -> new BlockEntityType<>(
                    QuestSubmitterBlockEntity::new,
                    Set.of(ModBlocks.QUEST_SUBMITTER.get()),
                    null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RewardClaimerBlockEntity>> REWARD_CLAIMER =
            BLOCK_ENTITIES.register("reward_claimer", () -> new BlockEntityType<>(
                    RewardClaimerBlockEntity::new,
                    Set.of(ModBlocks.REWARD_CLAIMER.get()),
                    null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}

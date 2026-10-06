package dev.ftbqauto.registry;

import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.block.QuestSubmitterBlock;
import dev.ftbqauto.block.RewardClaimerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FTBQuestAutomation.MOD_ID);

    public static final DeferredBlock<QuestSubmitterBlock> QUEST_SUBMITTER = BLOCKS.registerBlock(
            "quest_submitter",
            QuestSubmitterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<RewardClaimerBlock> REWARD_CLAIMER = BLOCKS.registerBlock(
            "reward_claimer",
            RewardClaimerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}

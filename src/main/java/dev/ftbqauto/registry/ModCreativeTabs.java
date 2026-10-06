package dev.ftbqauto.registry;

import dev.ftbqauto.FTBQuestAutomation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FTBQuestAutomation.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ftbqauto"))
                    .icon(() -> new ItemStack(ModItems.QUEST_BINDER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.QUEST_SUBMITTER.get());
                        output.accept(ModItems.REWARD_CLAIMER.get());
                        output.accept(ModItems.QUEST_BINDER.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}

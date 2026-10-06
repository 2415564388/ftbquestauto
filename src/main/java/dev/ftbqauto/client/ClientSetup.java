package dev.ftbqauto.client;

import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.registry.ModBlockEntities;
import dev.ftbqauto.registry.ModMenus;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 两个方块都用原版的三行箱子界面。
 * 布局不用自己画、贴图不用自己做，玩家也一看就懂。
 */
@EventBusSubscriber(modid = FTBQuestAutomation.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.QUEST_SUBMITTER.get(), ContainerScreen::new);
        event.register(ModMenus.REWARD_CLAIMER.get(), ContainerScreen::new);
    }

    /** 方块正面的任务图标 + 进度，效果参考 FTB 的任务显示器。 */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.QUEST_SUBMITTER.get(), BoundBlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.REWARD_CLAIMER.get(), BoundBlockRenderer::new);
    }
}

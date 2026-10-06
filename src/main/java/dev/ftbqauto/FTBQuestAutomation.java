package dev.ftbqauto;

import com.mojang.logging.LogUtils;
import dev.ftbqauto.registry.ModBlockEntities;
import dev.ftbqauto.registry.ModBlocks;
import dev.ftbqauto.registry.ModCapabilities;
import dev.ftbqauto.registry.ModCreativeTabs;
import dev.ftbqauto.registry.ModItems;
import dev.ftbqauto.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * FTB 任务自动化。
 *
 * 三个内容：
 * - 任务提交器：绑定一个物品类任务的 ID，红石上升沿把箱内物品提交给该任务
 * - 奖励领取器：绑定一个任务的 ID，任务完成后自动把奖励领进自己的库存
 * - FTB 任务绑定器：给上面两个方块绑定任务 ID
 */
@Mod(FTBQuestAutomation.MOD_ID)
public class FTBQuestAutomation {
    public static final String MOD_ID = "ftbqauto";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBQuestAutomation(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        modBus.addListener(ModCapabilities::register);
    }
}

package dev.ftbqauto.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * 把两个方块暴露成标准的物品处理器。
 *
 * <b>这一步不能省。</b>光实现 {@code WorldlyContainer} 只够原版漏斗用，
 * 机械动力的漏斗/溜槽、其他物流模组走的都是 NeoForge 的
 * {@code Capabilities.ItemHandler.BLOCK}。不注册这个能力，
 * 管道和漏斗就会认为方块里没东西。
 */
public final class ModCapabilities {

    private ModCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // InvWrapper 会把 Container 包装成 IItemHandler，
        // 六面都给同一个实例 —— 我们的 WorldlyContainer 本来就允许任意面进出。
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.QUEST_SUBMITTER.get(),
                (blockEntity, side) -> new InvWrapper(blockEntity));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.REWARD_CLAIMER.get(),
                (blockEntity, side) -> new InvWrapper(blockEntity));
    }
}

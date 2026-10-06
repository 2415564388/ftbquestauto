package dev.ftbqauto.integration;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.block.BoundBlock;
import dev.ftbqauto.block.entity.BoundContainerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * 玉（Jade）联动：看向方块时显示绑定的任务标题。
 *
 * 只依赖 Jade 的 API 做编译，运行时没装 Jade 的话这个类不会被加载
 * （靠 {@code @WailaPlugin} 由 Jade 自己扫出来）。
 */
@WailaPlugin(FTBQuestAutomation.MOD_ID)
public class JadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BoundQuestProvider.INSTANCE, BoundBlock.class);
    }

    public enum BoundQuestProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(FTBQuestAutomation.MOD_ID, "bound_quest");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof BoundContainerBlockEntity blockEntity)) {
                return;
            }

            long id = blockEntity.getBoundId();
            if (id == 0L) {
                tooltip.add(Component.translatable("jade.ftbqauto.unbound").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }

            // 绑定的 ID 会随方块实体同步到客户端，所以这里能直接解析出标题。
            // 解析不出来就退化成问号 —— 比如任务书还没加载完。
            Component name = null;
            if (ClientQuestFile.exists()) {
                QuestObjectBase bound = ClientQuestFile.INSTANCE.getBase(id);
                if (bound != null) {
                    name = bound.getTitle();
                }
            }

            tooltip.add(Component.translatable("jade.ftbqauto.bound",
                    name != null
                            ? name.copy().withStyle(ChatFormatting.AQUA)
                            : Component.literal("?").withStyle(ChatFormatting.RED)));

            // 归属是按玩家算的（进度记在该玩家的队伍上、奖励也发给该玩家），
            // 所以把绑的是谁显示出来很有必要。
            String ownerName = blockEntity.getOwnerName();
            tooltip.add(Component.translatable("jade.ftbqauto.owner",
                    Component.literal(ownerName.isEmpty() ? "?" : ownerName)
                            .withStyle(ChatFormatting.GOLD)));
        }
    }
}

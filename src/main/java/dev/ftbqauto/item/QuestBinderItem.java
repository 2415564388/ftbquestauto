package dev.ftbqauto.item;

import dev.ftbqauto.block.entity.BoundContainerBlockEntity;
import dev.ftbqauto.client.BinderClientHooks;
import dev.ftbqauto.net.BindBlockPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * FTB 任务绑定器。
 *
 * - 右键空气：打开 FTB 原版的任务搜索界面（就是任务界面里 Ctrl+F 那个），选中即绑定到本物品
 * - 右键方块：把本物品上绑的任务写到方块上
 * - 潜行右键空气：清空绑定
 *
 * 绑定值存在物品的 CustomData 里，除了 id 还缓存一份标题，这样 tooltip 不必区分客户端/服务端。
 */
public class QuestBinderItem extends Item {

    private static final String KEY_ID = "BoundQuestId";
    private static final String KEY_TITLE = "BoundQuestTitle";

    public QuestBinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide) {
                clearBound(stack);
                player.displayClientMessage(Component.translatable("gui.ftbqauto.binder_cleared")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.success(stack);
        }

        // 打开 FTB 的搜索界面是纯客户端的事。
        // 这个分支只会在客户端执行，所以专用服务端永远不会去加载 BinderClientHooks。
        if (level.isClientSide) {
            BinderClientHooks.openSearchForItem();
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * 被方块调用：手持绑定器右键了任务提交器 / 奖励领取器。
     *
     * 绑定器上有任务就直接绑；没有的话就把搜索界面打开，选完直接绑到这个方块上。
     */
    public static ItemInteractionResult bindToBlock(Level level, BlockPos pos, Player player, ItemStack binder) {
        if (!(level.getBlockEntity(pos) instanceof BoundContainerBlockEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        long id = getBoundId(binder);

        if (level.isClientSide) {
            if (id == 0L) {
                BinderClientHooks.openSearchForBlock(pos);
            } else {
                PacketDistributor.sendToServer(new BindBlockPayload(pos, id));
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------
    // 绑定值读写
    // ------------------------------------------------------------------

    public static long getBoundId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0L : data.copyTag().getLong(KEY_ID);
    }

    public static String getBoundTitle(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString(KEY_TITLE);
    }

    public static void setBound(ItemStack stack, long id, String title) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(KEY_ID, id);
            tag.putString(KEY_TITLE, title);
        });
    }

    public static void clearBound(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(KEY_ID);
            tag.remove(KEY_TITLE);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        long id = getBoundId(stack);

        if (id == 0L) {
            tooltip.add(Component.translatable("tooltip.ftbqauto.binder.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            String title = getBoundTitle(stack);
            tooltip.add(Component.translatable("tooltip.ftbqauto.binder.bound",
                    Component.literal(title.isEmpty() ? "?" : title).withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.literal("ID: " + Long.toHexString(id).toUpperCase())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable("tooltip.ftbqauto.binder.hint").withStyle(ChatFormatting.GRAY));
        // 绑定器不携带玩家身份，归属是"右键方块的那一刻"决定的
        tooltip.add(Component.translatable("tooltip.ftbqauto.binder.owner_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}

package dev.ftbqauto.net;

import dev.ftbqauto.FTBQuestAutomation;
import dev.ftbqauto.block.entity.BoundContainerBlockEntity;
import dev.ftbqauto.item.QuestBinderItem;
import dev.ftbqauto.util.QuestHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = FTBQuestAutomation.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    /** 玩家离方块多远就不认这次绑定请求（防作弊）。 */
    private static final double MAX_BIND_DISTANCE_SQ = 64.0D;

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BindBlockPayload.TYPE, BindBlockPayload.STREAM_CODEC, ModNetwork::handleBindBlock);
        registrar.playToServer(SetBinderPayload.TYPE, SetBinderPayload.STREAM_CODEC, ModNetwork::handleSetBinder);
    }

    private static void handleBindBlock(BindBlockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Level level = player.level();
            if (!level.isLoaded(payload.pos())) {
                return;
            }
            if (player.distanceToSqr(payload.pos().getCenter()) > MAX_BIND_DISTANCE_SQ) {
                return;
            }
            if (!(level.getBlockEntity(payload.pos()) instanceof BoundContainerBlockEntity be)) {
                return;
            }

            if (!(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem()
                    instanceof QuestBinderItem)
                    && !(player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND).getItem()
                    instanceof QuestBinderItem)) {
                return;
            }

            if (!be.acceptsBinding(level, payload.id())) {
                player.displayClientMessage(be.bindingRejected().copy().withStyle(ChatFormatting.RED), true);
                return;
            }

            // 归属按"谁拿绑定器右键"算。名字一起存下来，客户端显示时不用查档案缓存。
            be.setBinding(player.getUUID(), player.getGameProfile().getName(), payload.id());

            String title = QuestHelper.displayName(level, payload.id());
            player.displayClientMessage(Component.translatable("gui.ftbqauto.bound",
                    title == null ? Component.literal("?").withStyle(ChatFormatting.RED)
                            : Component.literal(title).withStyle(ChatFormatting.AQUA)), true);
        });
    }

    private static void handleSetBinder(SetBinderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = findBinder(player);
            if (stack.isEmpty()) {
                return;
            }
            // 客户端选出来的 id 必须真能解析到对象，别让伪造包写进垃圾数据
            if (QuestHelper.resolve(player.level(), payload.id()) == null) {
                return;
            }

            QuestBinderItem.setBound(stack, payload.id(), payload.title());
            player.displayClientMessage(Component.translatable("gui.ftbqauto.binder_set",
                    Component.literal(payload.title()).withStyle(ChatFormatting.AQUA)), true);
        });
    }

    private static ItemStack findBinder(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof QuestBinderItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof QuestBinderItem ? off : ItemStack.EMPTY;
    }
}

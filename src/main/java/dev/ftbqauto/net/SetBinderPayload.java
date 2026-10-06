package dev.ftbqauto.net;

import dev.ftbqauto.FTBQuestAutomation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：把选中的任务写进手里的绑定器。 */
public record SetBinderPayload(long id, String title) implements CustomPacketPayload {

    public static final Type<SetBinderPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FTBQuestAutomation.MOD_ID, "set_binder"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetBinderPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, SetBinderPayload::id,
                    ByteBufCodecs.STRING_UTF8, SetBinderPayload::title,
                    SetBinderPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

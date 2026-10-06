package dev.ftbqauto.net;

import dev.ftbqauto.FTBQuestAutomation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：把任务 ID 绑定到某个方块上。 */
public record BindBlockPayload(BlockPos pos, long id) implements CustomPacketPayload {

    public static final Type<BindBlockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(FTBQuestAutomation.MOD_ID, "bind_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BindBlockPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BindBlockPayload::pos,
                    ByteBufCodecs.VAR_LONG, BindBlockPayload::id,
                    BindBlockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

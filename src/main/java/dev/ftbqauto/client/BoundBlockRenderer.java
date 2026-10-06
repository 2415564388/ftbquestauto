package dev.ftbqauto.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftbqauto.block.BoundBlock;
import dev.ftbqauto.block.entity.BoundContainerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * 在方块正面画绑定的任务：上方任务标题，中间任务图标。观感对标 FTB 的任务显示器。
 *
 * <h2>坐标系</h2>
 * <pre>
 *   translate(0.5, 0.5, 0.5)                      // 挪到方块中心
 *   mulPose(YP, -FACING.toYRot())                 // 转到正面朝外
 *   translate(0, 0, FACE_OFFSET)                  // 沿法线推出去一点
 * </pre>
 * 这个坐标系里：局部 +X = 观察者右手边，局部 +Y = <b>世界向上</b>，局部 +Z = 朝观察者。
 *
 * 它和原版两处的约定完全一致，可以直接照搬它们的做法：
 * <ul>
 *   <li><b>物品</b>：物品展示框 {@code ItemFrameRenderer}。它的 {@code direction} 指向墙内，
 *       所以旋转是 {@code 180 - direction.toYRot()}，而 {@code yRot == toYRot()}，
 *       展开正好是 {@code -FACING.toYRot()} —— 和这里一样。
 *       它用 {@code ItemDisplayContext.FIXED} + {@code scale(0.5f)}，
 *       因为渲染时的局部 +Y 已经是世界向上，<b>不需要再翻</b>。</li>
 *   <li><b>文字</b>：告示牌 {@code SignRenderer#translateSignText}。它同样用
 *       {@code -FACING.toYRot()}，画字前是 {@code scale(f, -f, f)} ——
 *       <b>负的 Y</b>。因为字体是以"Y 向下"为前提生成的，
 *       而这里的局部 +Y 是世界向上，不翻的话文字会上下颠倒并跑出方块顶边。</li>
 * </ul>
 *
 * 正面贴图是一整块纯黑面板（见 tools/gen_textures.js 的 blackFace），当屏幕底板用。
 */
public class BoundBlockRenderer<T extends BoundContainerBlockEntity> implements BlockEntityRenderer<T> {

    /** 标题字号占面的比例（按 9px 字高换算，和任务显示器一致）。 */
    private static final float TITLE_SIZE = 0.15F;

    /** 标题上沿的位置。因为文字带负 Y 缩放，是从这里<b>往下</b>排的。 */
    private static final float TITLE_Y = 0.38F;

    /** 图标中心的位置。给上面那行标题让出地方，所以略微偏下。 */
    private static final float ICON_Y = -0.08F;

    /** 图标缩放。取物品展示框的值（0.5）。 */
    private static final float ICON_SCALE = 0.5F;

    /** 沿法线推出去的距离，稍微离面一点避免 z-fighting。 */
    private static final float FACE_OFFSET = 0.502F;

    public BoundBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!ClientQuestFile.exists()) {
            return;
        }
        long id = be.getBoundId();
        if (id == 0L) {
            return;
        }
        if (!(ClientQuestFile.INSTANCE.getBase(id) instanceof Quest quest)) {
            return;
        }

        Direction facing = be.getBlockState().getValue(BoundBlock.FACING);

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(0.0D, 0.0D, FACE_OFFSET);

        drawTitle(quest.getTitle(), pose, buffer);
        drawIcon(quest.getIcon(), pose, buffer, be);

        pose.popPose();
    }

    // ------------------------------------------------------------------

    /** 图标。局部 +Y 已经是世界向上，所以 FIXED 直接画就是正的。 */
    private void drawIcon(Icon icon, PoseStack pose, MultiBufferSource buffer, T be) {
        ItemStack stack = iconStack(icon);
        if (stack.isEmpty()) {
            return;
        }

        pose.pushPose();
        pose.translate(0.0D, ICON_Y, 0.0D);
        pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffer, be.getLevel(), 0);
        pose.popPose();
    }

    /**
     * 标题。负 Y 缩放让文字正过来（照搬告示牌），
     * 缩放系数则照搬任务显示器：标题太长自动缩小，保证不超出方块正面。
     */
    private void drawTitle(Component text, PoseStack pose, MultiBufferSource buffer) {
        Font font = Minecraft.getInstance().font;
        int length = font.width(text);

        float scale = TITLE_SIZE / 9.0F;
        float width = length * scale;
        if (width > 1.0F) {
            scale /= width;
            width = 1.0F;
        }
        if (width > 0.9F) {
            scale *= 0.9F;
        }

        pose.pushPose();
        pose.translate(0.0D, TITLE_Y, 0.01D);
        pose.scale(scale, -scale, scale);

        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, -length / 2.0F, 0.0F, 0xFFFFFFFF, false, matrix, buffer,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        pose.popPose();
    }

    /** 把 Icon 拆成能直接渲染的 ItemStack。非物品图标（自定义图片等）暂不处理。 */
    private static ItemStack iconStack(@Nullable Icon icon) {
        if (icon == null) {
            return ItemStack.EMPTY;
        }
        if (icon instanceof IconAnimation animation && !animation.list.isEmpty()) {
            icon = animation.list.get((int) (System.currentTimeMillis() / 1000L % animation.list.size()));
        }
        return icon instanceof ItemIcon itemIcon ? itemIcon.getStack() : ItemStack.EMPTY;
    }
}

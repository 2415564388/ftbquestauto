package dev.ftbqauto.client;

import dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.QuestObjectType;
import dev.ftb.mods.ftbquests.util.ConfigQuestObject;
import dev.ftbqauto.net.BindBlockPayload;
import dev.ftbqauto.net.SetBinderPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 打开 FTB 原版的任务搜索界面。
 *
 * 复用的就是任务界面里 Ctrl+F 打开的那个屏幕
 * （{@code SelectQuestObjectScreen}）—— 它本身就是为"让外部选一个任务对象"设计的：
 * 构造函数 public、不依赖 QuestScreen 父屏幕，点击结果的回调由调用方传入。
 * FTB 自己那句"点完跳转到任务详情"只是它的一个调用点而已。
 *
 * 注意：这个类不在 FTB 的稳定 API 包（dev.ftb.mods.ftbquests.api）里，
 * 将来大版本升级 FTB 时需要重新验证。
 *
 * 本类只在客户端加载，专用服务端不会碰它。
 */
public final class BinderClientHooks {

    private BinderClientHooks() {
    }

    /** 右键空气：挑一个任务绑到绑定器物品上。 */
    public static void openSearchForItem() {
        open((id, title) -> PacketDistributor.sendToServer(new SetBinderPayload(id, title)));
    }

    /**
     * 手里绑定器还没绑东西时右键了方块 —— 直接把界面开出来，
     * 选完就绑到这个方块上，省一步"先绑物品再绑方块"。
     */
    public static void openSearchForBlock(BlockPos pos) {
        open((id, title) -> PacketDistributor.sendToServer(new BindBlockPayload(pos, id)));
    }

    private static void open(BiConsumer<Long, String> onPicked) {
        // 两个方块现在都绑任务（Quest），所以只列任务，不再混着子任务。
        Predicate<QuestObjectBase> filter = QuestObjectType.QUEST;
        ConfigQuestObject<QuestObjectBase> config = new ConfigQuestObject<>(filter);

        AtomicReference<SelectQuestObjectScreen<QuestObjectBase>> ref = new AtomicReference<>();

        SelectQuestObjectScreen<QuestObjectBase> screen = new SelectQuestObjectScreen<>(config, accepted -> {
            // 右上角那个 X 走的是 doCancel() → callback.save(false)，
            // 它自己不关界面 —— FTB 拿这个屏幕当配置弹窗，指望父界面在回调里接管。
            // 我们没父界面，所以必须自己关，否则点 X 毫无反应、只能按 ESC。
            // 用 execute 推迟到当前事件处理完再关，避免在点击回调里拆自己。
            Minecraft.getInstance().execute(() -> {
                SelectQuestObjectScreen<?> s = ref.get();
                if (s != null) {
                    s.closeGui(true);
                }
            });

            if (!accepted) {
                return;
            }
            QuestObjectBase picked = config.getValue();
            if (picked == null) {
                return;
            }
            onPicked.accept(picked.id, picked.getTitle().getString());
        });

        ref.set(screen);
        screen.openGui();
    }
}

package dev.ftbqauto.block.entity;

import dev.ftbqauto.util.QuestHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 两个方块共用的容器基类。
 *
 * 27 格（正好对上原版三行箱子界面），能被漏斗/管道正常存取。
 * 绑定信息 = 任务 ID + 绑定时的玩家 UUID（队伍在运行时由玩家解析出来）。
 */
public abstract class BoundContainerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SIZE = 27;
    private static final int[] ALL_SLOTS = IntStream.range(0, SIZE).toArray();

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    @Nullable
    private UUID owner;

    /**
     * 绑定玩家的名字，只用于显示。
     * 一起存下来是为了让客户端（玉）和物品提示不用去查档案缓存 ——
     * 玩家离线时客户端根本查不到别人的名字。
     */
    private String ownerName = "";

    private long boundId;

    protected BoundContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    // ------------------------------------------------------------------
    // 绑定
    // ------------------------------------------------------------------

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    /** 绑定玩家的名字，可能为空（老存档或档案缺失）。 */
    public String getOwnerName() {
        return ownerName;
    }

    public long getBoundId() {
        return boundId;
    }

    public boolean isBound() {
        return boundId != 0L && owner != null;
    }

    public void setBinding(@Nullable UUID owner, String ownerName, long boundId) {
        this.owner = owner;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.boundId = boundId;
        setChanged();
        syncToClient();
    }

    /** 这个方块是否接受把这个 id 绑上来。 */
    public abstract boolean acceptsBinding(Level level, long id);

    /** 类型不匹配时的提示语。 */
    public abstract Component bindingRejected();

    protected void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------
    // 持久化与同步
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // 注意：BaseContainerBlockEntity 的 super 只存 lockKey 和 CustomName，
        // <b>不存物品</b>。每个容器子类都得自己调 ContainerHelper（原版
        // DispenserBlockEntity 就是这么写的）。漏掉这两行的话，
        // 方块里的东西永远不会写进 NBT，区块一存一读就没了。
        ContainerHelper.saveAllItems(tag, items, registries);

        tag.putLong("BoundId", boundId);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (!ownerName.isEmpty()) {
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // loadAllItems 是往现成的列表里按槽位覆盖，不会清空，
        // 所以先换成一张干净的列表，避免残留上一次加载的旧数据。
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);

        boundId = tag.getLong("BoundId");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 容器标题带上绑定的任务名，开箱子时一眼能看出绑了什么。 */
    @Override
    public Component getDisplayName() {
        Component base = super.getDisplayName();
        if (level == null || level.isClientSide) {
            return base;
        }
        if (boundId == 0L) {
            return base.copy().append(Component.translatable("gui.ftbqauto.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        String title = QuestHelper.displayName(level, boundId);
        if (title == null) {
            return base.copy().append(Component.translatable("gui.ftbqauto.dangling")
                    .withStyle(ChatFormatting.RED));
        }
        return base.copy().append(Component.literal(" · " + title).withStyle(ChatFormatting.AQUA));
    }
}

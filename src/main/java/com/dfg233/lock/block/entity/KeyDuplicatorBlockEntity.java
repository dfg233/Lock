package com.dfg233.lock.block.entity;

import com.dfg233.lock.item.ModItems;
import com.dfg233.lock.item.custom.key.KeyItem;
import com.dfg233.lock.item.custom.lock.MechanicalLockItem;
import com.dfg233.lock.screen.KeyDuplicatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 钥匙复制台方块实体
 * 管理3个槽位：模板槽（锁/钥匙）、材料槽（空白钥匙）、输出槽（复制的钥匙）
 */
public class KeyDuplicatorBlockEntity extends BlockEntity implements MenuProvider {

    // 槽位索引
    public static final int TEMPLATE_SLOT = 0;  // 模板槽：放锁或钥匙
    public static final int MATERIAL_SLOT = 1;  // 材料槽：放空白钥匙
    public static final int OUTPUT_SLOT = 2;    // 输出槽：产出的钥匙

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // 当模板槽或材料槽改变时，尝试合成
            if (slot == TEMPLATE_SLOT || slot == MATERIAL_SLOT) {
                tryCraft();
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case TEMPLATE_SLOT -> isValidTemplate(stack);  // 锁或已有钥匙
                case MATERIAL_SLOT -> isValidMaterial(stack);  // 空白钥匙
                case OUTPUT_SLOT -> false;  // 输出槽不允许放入
                default -> super.isItemValid(slot, stack);
            };
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 当从输出槽真正取出物品时（非模拟），消耗材料
            if (slot == OUTPUT_SLOT && !simulate) {
                // 先获取要取出的物品
                ItemStack result = super.extractItem(slot, amount, true); // 先模拟获取
                if (!result.isEmpty()) {
                    // 真正取出并消耗材料
                    ItemStack actualResult = super.extractItem(slot, amount, false);
                    consumeMaterials();
                    return actualResult;
                }
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;

    public KeyDuplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEY_DUPLICATOR.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 0;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.lock.key_duplicator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new KeyDuplicatorMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    /**
     * 检查是否为有效的模板物品（锁或已有钥匙）
     */
    private boolean isValidTemplate(ItemStack stack) {
        if (stack.isEmpty()) return true;

        // 1. 检查是否为已绑定的钥匙（有UUID的KeyItem）
        if (stack.getItem() instanceof KeyItem keyItem) {
            // 钥匙必须有UUID（已绑定锁）
            return KeyItem.getLockId(stack) != null;
        }

        // 2. 检查是否为已绑定的锁（MechanicalLockItem）
        if (stack.getItem() instanceof MechanicalLockItem) {
            // 锁必须有LockData且包含lockId
            if (stack.hasTag() && stack.getTag().contains("LockData")) {
                CompoundTag lockData = stack.getTag().getCompound("LockData");
                return lockData.hasUUID("lockId");
            }
            return false;
        }

        return false;
    }

    /**
     * 检查是否为有效的材料（空白钥匙）
     */
    private boolean isValidMaterial(ItemStack stack) {
        if (stack.isEmpty()) return true;
        // 必须是钥匙，且没有UUID（空白钥匙）
        if (stack.getItem() instanceof KeyItem keyItem) {
            return KeyItem.getLockId(stack) == null;
        }
        return false;
    }

    /**
     * 尝试合成钥匙
     */
    private void tryCraft() {
        ItemStack template = itemHandler.getStackInSlot(TEMPLATE_SLOT);
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);

        // 清空输出槽
        if (!output.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, ItemStack.EMPTY);
        }

        // 检查是否有有效的模板和材料
        if (template.isEmpty() || material.isEmpty()) {
            return;
        }

        // 获取模板的UUID
        UUID lockId = getTemplateLockId(template);
        if (lockId == null) {
            return;
        }

        // 获取钥匙类型
        String keyType = getTemplateKeyType(template);
        if (keyType == null) {
            return;
        }

        // 创建输出钥匙
        ItemStack result = material.copy();
        result.setCount(1);
        KeyItem.bindToLock(result, lockId);

        itemHandler.setStackInSlot(OUTPUT_SLOT, result);
    }

    /**
     * 从模板获取锁ID
     */
    private UUID getTemplateLockId(ItemStack template) {
        // 1. 从钥匙获取UUID
        if (template.getItem() instanceof KeyItem) {
            return KeyItem.getLockId(template);
        }

        // 2. 从锁物品获取UUID
        if (template.getItem() instanceof MechanicalLockItem) {
            if (template.hasTag() && template.getTag().contains("LockData")) {
                CompoundTag lockData = template.getTag().getCompound("LockData");
                if (lockData.hasUUID("lockId")) {
                    return lockData.getUUID("lockId");
                }
            }
        }

        return null;
    }

    /**
     * 从模板获取钥匙类型
     */
    private String getTemplateKeyType(ItemStack template) {
        if (template.getItem() instanceof KeyItem keyItem) {
            return keyItem.getKeyType();
        }
        // TODO: 如果是锁物品，返回对应的钥匙类型
        return "mechanical"; // 默认类型
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * 消耗合成材料
     * 当玩家从输出槽取出钥匙时调用
     */
    private void consumeMaterials() {
        // 消耗一个空白钥匙
        ItemStack material = itemHandler.getStackInSlot(MATERIAL_SLOT);
        if (!material.isEmpty()) {
            material.shrink(1);
            itemHandler.setStackInSlot(MATERIAL_SLOT, material);
        }

        // 模板（锁/钥匙）不消耗，保持不动
        // 如果需要消耗模板，取消下面的注释：
        // ItemStack template = itemHandler.getStackInSlot(TEMPLATE_SLOT);
        // if (!template.isEmpty()) {
        //     template.shrink(1);
        //     itemHandler.setStackInSlot(TEMPLATE_SLOT, template);
        // }

        // 清空输出槽（会在extractItem中自动处理）
        // 重新尝试合成（如果材料足够）
        tryCraft();
    }
}

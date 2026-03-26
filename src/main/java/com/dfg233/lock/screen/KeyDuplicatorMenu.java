package com.dfg233.lock.screen;

import com.dfg233.lock.block.entity.KeyDuplicatorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 钥匙复制台菜单
 * 3个槽位：模板槽、材料槽、输出槽
 */
public class KeyDuplicatorMenu extends AbstractContainerMenu {

    public final KeyDuplicatorBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // 槽位索引
    public static final int TEMPLATE_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    // GUI 位置常量
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 84;
    private static final int HOTBAR_Y = 142;

    public KeyDuplicatorMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(0));
    }

    public KeyDuplicatorMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.KEY_DUPLICATOR.get(), id);
        checkContainerSize(inv, 3);
        blockEntity = (KeyDuplicatorBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            // 模板槽：上方中间
            this.addSlot(new SlotItemHandler(handler, TEMPLATE_SLOT, 44, 22));
            // 材料槽：模板槽右侧
            this.addSlot(new SlotItemHandler(handler, MATERIAL_SLOT, 80, 22));
            // 输出槽：下方
            this.addSlot(new SlotItemHandler(handler, OUTPUT_SLOT, 116, 40) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;  // 输出槽不允许放入物品
                }
            });
        });

        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9,
                        INVENTORY_START_X + l * 18, INVENTORY_START_Y + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, INVENTORY_START_X + i * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 36) {
            // 从玩家背包移动到方块槽位
            if (!moveItemStackTo(sourceStack, 36, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 39) {
            // 从方块槽位移动到玩家背包
            if (!moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyOfSourceStack;
    }
}

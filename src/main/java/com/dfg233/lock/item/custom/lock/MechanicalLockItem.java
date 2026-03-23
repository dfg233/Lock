package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.data.LockData;
import com.dfg233.lock.data.LockLevelData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.item.ModItems;
import com.dfg233.lock.item.custom.key.KeyItem;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import com.dfg233.lock.tags.ModBlockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dfg233.lock.Utils.LockUtils.getActualLockPos;

public class MechanicalLockItem extends Item {
    public MechanicalLockItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // 获取实际点击的锁位置（处理双门等逻辑）
        BlockPos pos = getActualLockPos(level, context.getClickedPos());
        Player player = context.getPlayer();
        ItemStack lockItemStack = context.getItemInHand();

        // 1. 基础判断：方块是否允许上锁
        if (!level.getBlockState(pos).is(ModBlockTags.LOCKABLE)) {
            if (!level.isClientSide() && player != null) {
                player.displayClientMessage(Component.translatable("message.lock.not_lockable").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            LockLevelData worldData = LockLevelData.get(level);
            if (worldData != null && worldData.getLock(pos) == null) {

                // 2. 数据恢复逻辑：检查物品 NBT 中是否有存过的锁数据
                LockData data = new LockData();
                boolean isNewLock = true;

                if (lockItemStack.hasTag() && lockItemStack.getTag().contains("LockData")) {
                    // 如果是从方块上拆下来的旧锁，恢复其原有的 UUID 和属性
                    data.readFromNBT(lockItemStack.getTag().getCompound("LockData"));
                    isNewLock = false;
                } else {
                    // 如果是合成的新锁，初始化默认属性
                    data.setLockType("mechanical");
                    data.setKeyType("mechanical");
                }

                AbstractLock lock = AbstractLock.create(data);

                if (lock != null) {
                    // 3. 安装锁：如果是新锁会生成新 UUID，旧锁则保持不变
                    lock.onInstalled(player);
                    worldData.addLock(pos, data);

                    // 4. 发放钥匙逻辑：仅当这是一把从未绑定过的新锁时，才给玩家发钥匙
                    if (isNewLock && player != null) {
                        ItemStack keyStack = new ItemStack(ModItems.MECHANICAL_KEY.get());
                        KeyItem.bindToLock(keyStack, data.getLockId());

                        if (!player.getInventory().add(keyStack)) {
                            player.drop(keyStack, false);
                        }
                        player.displayClientMessage(Component.translatable("message.lock.key_received").withStyle(ChatFormatting.GREEN), true);
                    }

                    // 5. 同步数据到客户端
                    CompoundTag nbt = new CompoundTag();
                    data.writeToNBT(nbt);
                    ModMessages.sendToClients(new S2CSyncLockPacket(pos, nbt));

                    // 消耗物品并提示
                    lockItemStack.shrink(1);
                    if (player != null) player.displayClientMessage(Component.translatable("message.lock.success").withStyle(ChatFormatting.GREEN), true);

                    return InteractionResult.SUCCESS;
                }
            } else {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.lock.already_locked").withStyle(ChatFormatting.YELLOW), true);
                }
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pIsAdvanced) {
        // 1. 显示锁的类型 (固定为机械)
        pTooltip.add(Component.translatable("tooltip.lock.lock_type")
                .append(": ")
                .append(Component.translatable("tooltip.lock.type.mechanical").withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        // 2. 检查是否有绑定的 ID 数据
        CompoundTag tag = pStack.getTag();
        if (tag != null && tag.contains("LockData")) {
            CompoundTag lockData = tag.getCompound("LockData");
            if (lockData.hasUUID("lockId")) {
                pTooltip.add(Component.translatable("tooltip.lock.lock_id")
                        .append(": ")
                        .append(Component.literal(lockData.getUUID("lockId").toString().substring(0, 16)).withStyle(ChatFormatting.AQUA))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            // 3. 如果没有 LockData，说明是新合成的空白锁
            pTooltip.add(Component.translatable("tooltip.lock.lock_blank")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.data.LockData;
import com.dfg233.lock.data.LockLevelData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import com.dfg233.lock.tags.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MechanicalLockItem extends Item {
    public MechanicalLockItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = LockLevelData.LockUtils.getActualLockPos(level, context.getClickedPos());
        Player player = context.getPlayer();

        // 1. 先进行基础判断
        boolean isLockable = level.getBlockState(pos).is(ModBlockTags.LOCKABLE);

        // 2. 如果不可上锁，仅在服务端发送一次提示消息
        if (!isLockable) {
            if (!level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.translatable("message.lock.not_lockable"));
            }
            // 注意：这里客户端需要返回 FAIL 或 PASS，服务端返回 FAIL，以确保逻辑同步
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide()) {
            LockLevelData worldData = LockLevelData.get(level);
            if (worldData != null && worldData.getLock(pos) == null) {
                LockData data = new LockData();
                data.setLockType("mechanical");
                AbstractLock lock = AbstractLock.create(data);

                if (lock != null) {
                    lock.onInstalled(player);
                    worldData.addLock(pos, data);

                    CompoundTag nbt = new CompoundTag();
                    data.writeToNBT(nbt);
                    ModMessages.sendToClients(new S2CSyncLockPacket(pos, nbt));

                    context.getItemInHand().shrink(1);
                    if (player != null) player.sendSystemMessage(Component.translatable("message.lock.success"));
                    return InteractionResult.SUCCESS;
                }
            }else {
                // 如果已经有锁，也只在服务端提示一次
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.lock.already_exists"));
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
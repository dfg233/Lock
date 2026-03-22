package com.dfg233.lock.item;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.custom.lock.MechanicalLock;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import com.dfg233.lock.sounds.PlayLockedSound;
import com.dfg233.lock.sounds.PlayUnLockSound;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public abstract class AbstractLock {
    protected final LockData lockData;

    public AbstractLock(LockData data) {
        this.lockData = data;
    }

    public final InteractionResult tryInteract(Player player, Level level, BlockPos pos, ItemStack stack) {
        KeyData keyData = getKeyDataFromStack(stack);
        boolean isMatchingKey = keyData != null && onVerify(player, keyData) && lockData.getKeyType().equals(keyData.getKeyType());

        // 逻辑 A：使用匹配的钥匙点击（切换开关状态）
        if (isMatchingKey) {
            if (!level.isClientSide()) {
                boolean nextState = !lockData.isLocked();
                lockData.setLocked(nextState);

                if (nextState) {
                    PlayLockedSound.play(level, pos, this.lockData);
                } else {
                    PlayUnLockSound.play(level, pos, this.lockData);
                }

                Component message = nextState ?
                        Component.translatable("message.lock.locked").withStyle(ChatFormatting.RED) :
                        Component.translatable("message.lock.unlocked").withStyle(ChatFormatting.GREEN);
                player.displayClientMessage(message, true);

                syncToClients(level, pos);
            }
            // 返回 SUCCESS，消费掉这次点击，不打开箱子界面
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 逻辑 B：非钥匙交互
        if (lockData.isLocked()) {
            // 锁定时拦截一切交互
            if (!level.isClientSide()) {
                onVerifyFailed(level, pos);
            }
            return InteractionResult.FAIL;
        }

        // 未锁定时放行，让原版箱子正常打开
        return InteractionResult.PASS;
    }

    public static AbstractLock create(LockData data) {
        if ("mechanical".equals(data.getLockType())) {
            return new MechanicalLock(data);
        }
        return null;
    }

    public void onInstalled(Player player) {
        if (lockData.getLockId() == null) {
            lockData.setLockId(UUID.randomUUID());
        }
        lockData.setLocked(true);
    }

    public final void syncToClients(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            CompoundTag nbt = new CompoundTag();
            lockData.writeToNBT(nbt);
            ModMessages.sendToClients(new S2CSyncLockPacket(pos, nbt));
        }
    }

    // 去掉了关于门的 canLock 判断，箱子默认永远可以上锁
    protected boolean canLock(Level level, BlockPos pos) {
        return true;
    }

    protected abstract boolean onVerify(Player player, KeyData keyData);
    protected void onStateChanged(Level level, BlockPos pos, boolean locked) {}
    public abstract ItemStack getAsStack();
    protected abstract void onVerifyFailed(Level level, BlockPos pos);
    protected abstract KeyData getKeyDataFromStack(ItemStack stack);
}
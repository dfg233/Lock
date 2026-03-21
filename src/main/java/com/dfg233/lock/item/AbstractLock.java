package com.dfg233.lock.item;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.custom.lock.MechanicalLock;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public abstract class AbstractLock {
    protected final LockData lockData;

    //构造函数
    public AbstractLock(LockData data) {
        this.lockData = data;
    }
    /**
     * 核心逻辑：尝试交互（上锁、开锁、检查）
     * 返回值代表是否成功执行了操作
     */
    public final InteractionResult tryInteract(Player player, Level level, BlockPos pos, ItemStack stack) {
        KeyData keyData = getKeyDataFromStack(stack);

        if (onVerify(player, keyData)) {
            // 验证通过：切换状态
            boolean nextState = !lockData.isLocked();
            lockData.setLocked(nextState);

            // 触发状态改变钩子（播放声音等）
            onStateChanged(level, pos, nextState);

            // 核心：全自动同步给所有客户端
            syncToClients(level, pos);

            return InteractionResult.SUCCESS;
        } else {
            // 验证失败
            onVerifyFailed(level, pos);
            return InteractionResult.FAIL;
        }
    }

    public static AbstractLock create(LockData data) {
        String type = data.getLockType();
        if ("mechanical".equals(type)) {
            return new MechanicalLock(data);
        }
        // 其他类型的锁
        return null;
    }

    /**
     * 当锁被安装到方块上时触发
     */
    public void onInstalled(Player player) {
        if (lockData.getLockId() == null) {
            lockData.setLockId(UUID.randomUUID());
        }
        lockData.setLocked(true);
    }

    /**
     * 将当前数据同步到所有客户端
     */
    public final void syncToClients(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            CompoundTag nbt = new CompoundTag();
            lockData.writeToNBT(nbt);
            ModMessages.sendToClients(new S2CSyncLockPacket(pos, nbt));
        }
    }

    // --- 以下是交给子类实现的“钩子” ---
    protected abstract boolean onVerify(Player player, KeyData keyData);
    protected abstract void onStateChanged(Level level, BlockPos pos, boolean locked);
    protected abstract void onVerifyFailed(Level level, BlockPos pos);
    protected abstract KeyData getKeyDataFromStack(ItemStack stack);


}

package com.dfg233.lock.item;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Properties;

public abstract class AbstractLock {
    private LockData lockData;
    public AbstractLock(Properties properties) {
        this.lockData = new LockData();
    }

    //匹配锁和钥匙
    public final boolean isMatch(KeyData keyData) {
        return keyData.getKeyId().equals(lockData.getLockId());
    }

    //设置锁的状态
    public final void setLocked(boolean locked) {
        this.lockData.setLocked(locked);
        onStateChanged(locked);
    }

    //状态改变时的形式
    protected void onStateChanged(boolean locked) {
    }

    public abstract boolean handleInteraction(Player player, KeyData keyData, Level level, BlockPos pos);

    //保存和加载锁的数据
    public void save(CompoundTag tag) {
        this.lockData.writeToNBT(tag);
    }
    public void load(CompoundTag tag) {
        this.lockData.readFromNBT(tag);
    }
}

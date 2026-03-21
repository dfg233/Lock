package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.sounds.PlayLockedSound;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class MechanicalLock extends AbstractLock {
    public MechanicalLock(LockData data) {
        super(data);
    }
    @Override
    protected boolean onVerify(Player player, KeyData keyData) {
        return Objects.equals(keyData.getKeyId(), this.lockData.getLockId());
    }

    @Override
    protected void onStateChanged(Level level, BlockPos pos, boolean locked) {
        //仅在服务端播放声音（防止声音重叠或在客户端重复触发）
        if (!level.isClientSide()) {
            PlayLockedSound.play(level, pos, this.lockData);
        }
    }

    @Override
    protected void onVerifyFailed(Level level, BlockPos pos) {
        PlayLockedSound.play(level, pos, this.lockData);
    }

    @Override
    protected KeyData getKeyDataFromStack(ItemStack stack) {
        KeyData keyData = new KeyData();
        if (stack.hasTag()) {
            keyData.readFromNBT(stack.getTag());
        }
        return keyData;
    }
}

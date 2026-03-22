package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.item.ModItems;
import com.dfg233.lock.item.custom.key.KeyItem;
import com.dfg233.lock.sounds.PlayLockedSound;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MechanicalLock extends AbstractLock {
    public MechanicalLock(LockData data) {
        super(data);
    }

    @Override
    protected KeyData getKeyDataFromStack(ItemStack stack) {
        if (stack.getItem() instanceof KeyItem keyItem) {
            // 封装钥匙的 UUID 和 类型
            return new KeyData(KeyItem.getLockId(stack), keyItem.getKeyType());
        }
        return null;
    }

    @Override
    protected boolean onVerify(Player player, KeyData keyData) {
        if (keyData == null) return false;

        // 核心验证：UUID 是否匹配
        return lockData.getLockId() != null && lockData.getLockId().equals(keyData.getLockId());
    }

    @Override
    protected void onVerifyFailed(Level level, BlockPos pos) {
        // 验证失败（钥匙不对或尝试点击锁定的箱子）调用失败音效类
        PlayLockedSound.play(level, pos, this.lockData);
    }

    @Override
    public ItemStack getAsStack() {
        // 返回机械锁物品
        return new ItemStack(ModItems.MECHANICAL_LOCK.get());
    }
}

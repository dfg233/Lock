package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.item.ModItems;
import com.dfg233.lock.item.custom.key.KeyItem;
import com.dfg233.lock.sounds.PlayLockedSound;
import com.dfg233.lock.sounds.PlayUnLockSound;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

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

    @Override
    protected void onStateChanged(Level level, BlockPos pos, boolean locked) {
        super.onStateChanged(level, pos, locked);
        if (locked) {
            PlayLockedSound.play(level, pos, this.lockData);
        }else {
            PlayUnLockSound.play(level, pos, this.lockData);
        }
    }

    @Override
    protected boolean canLock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // 如果是门，必须处于关闭状态才能上锁
        if (state.getBlock() instanceof DoorBlock) {
            // 检查门的 OPEN 属性，如果门是开着的则不能上锁
            if (state.hasProperty(BlockStateProperties.OPEN)) {
                return !state.getValue(BlockStateProperties.OPEN);
            }
        }
        // 其他方块默认可以上锁
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected BakedModel getCustomModel(boolean isLocked) {
        if (isLocked) {
            // 如果有闭锁状态的特殊模型，在这里返回
            // return Minecraft.getInstance().getModelManager().getModel(CLOSED_MODEL_LOCATION);
        }
        // 默认回退到父类逻辑（物品模型）
        return super.getCustomModel(isLocked);
    }
}

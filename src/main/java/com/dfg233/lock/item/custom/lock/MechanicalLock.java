package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.item.ModItems;
import com.dfg233.lock.item.custom.key.KeyItem;
import com.dfg233.lock.sounds.PlayLockedSound;
import com.dfg233.lock.sounds.PlayUnLockSound;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
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
        if (!isLocked) {
            // 如果有开锁状态的特殊模型，在这里返回
            // return Minecraft.getInstance().getModelManager().getModel(CLOSED_MODEL_LOCATION);
        }
        // 默认回退到父类逻辑（物品模型）
        return super.getCustomModel(isLocked);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void applyRenderPosition(PoseStack poseStack, BlockState state) {
        // 1. 执行标准初始化（平移-旋转-平移）
        //    旋转轴设置在方块中心水平位置 (0.5, y, 0.5)
        //    旋转后原点回到方块西南角 (0, 0, 0)
        initRenderPosition(poseStack, state);

        // 2. 根据方块类型应用不同的默认位置
        if (state.getBlock() instanceof DoorBlock) {
            // ==================== 门 (Door) 默认位置 ====================
            // 判断铰链位置，将锁放在门把手侧
            boolean isLeftHinge = state.hasProperty(DoorBlock.HINGE)
                    && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;

            // 左铰链：门轴在左，把手在右 -> 锁放在右侧 (X=0.25)
            // 右铰链：门轴在右，把手在左 -> 锁放在左侧 (X=0.25)
            float handleX = isLeftHinge ? -0.25f : 0.25f;

            // 位置：门把手侧，上半部分高度，门外表面
            poseStack.translate(handleX, 0.5f, -0.5f);  // (左右, 上下高度, 前后突出)
//            poseStack.scale(0.5f, 0.5f, 0.5f);         // 适当缩小

            // 3. 适配门的开闭状态
            // 门打开时，锁需要跟随门旋转
            applyDoorOpenRotation(poseStack, state, isLeftHinge);

        } else if (state.getBlock() instanceof ChestBlock) {
            // ==================== 箱子 (Chest) 默认位置 ====================
            // 箱子是1格高，渲染在正面中央
            poseStack.translate(0.0f, 0.0f, 0.48f);   // (中心, 中心高度, 前面突出)
//            poseStack.scale(0.4f, 0.4f, 0.4f);       // 比门上的锁稍小

        } else {
            // ==================== 其他方块默认位置 ====================
            // 默认渲染在方块正面中心
//            poseStack.translate(0.5f, 0.5f, 0.1f);   // (中心, 中心高度, 前面突出)
//            poseStack.scale(0.4f, 0.4f, 0.4f);
        }
    }

    /**
     * 应用门打开时的旋转
     * 当门打开时，锁需要跟随门一起旋转
     *
     * @param poseStack 变换栈
     * @param state 门方块状态
     * @param isLeftHinge 是否为左铰链门
     */
    @OnlyIn(Dist.CLIENT)
    private void applyDoorOpenRotation(PoseStack poseStack, BlockState state, boolean isLeftHinge) {
        // 检查门是否处于打开状态
        if (!state.hasProperty(BlockStateProperties.OPEN)) {
            return;
        }

        boolean isOpen = state.getValue(BlockStateProperties.OPEN);
        if (!isOpen) {
            return;  // 门关闭时不需要额外旋转
        }

        //使模型回到方块中心
        float handleX = isLeftHinge ? 0.25f : -0.25f;
        poseStack.translate(handleX, 0.0f, 0.5f);

        poseStack.translate(0.5, 0.0, 0.5);
        float rotationAngle = isLeftHinge ? 90.0f : -90.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationAngle));
        poseStack.translate(-0.5, 0.0, -0.5);

        //移动到门把手位置
        handleX = isLeftHinge ? -0.25f : 0.25f;
        poseStack.translate(handleX, 0.0, 0.3);

    }
}

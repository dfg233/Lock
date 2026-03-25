package com.dfg233.lock.item;

import com.dfg233.lock.client.ClientLockCache;
import com.dfg233.lock.data.KeyData;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.custom.lock.MechanicalLock;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public abstract class AbstractLock {
    protected final LockData lockData;

    public AbstractLock(LockData data) {
        this.lockData = data;
    }

    public final InteractionResult tryInteract(Player player, Level level, BlockPos pos, ItemStack stack) {
        KeyData keyData = getKeyDataFromStack(stack);
        // 验证钥匙是否匹配
        boolean isMatchingKey = keyData != null && onVerify(player, keyData) && lockData.getKeyType().equals(keyData.getKeyType());

        // 逻辑分支 A：使用钥匙交互
        if (keyData != null) {
            if (isMatchingKey) {
                if (lockData.isLocked()) {
                    // 解锁逻辑
                    lockData.setLocked(false);
                    ClientLockCache.updateStatus(pos, lockData);
                    onStateChanged(level, pos, false);
                    syncToClients(level, pos);
                    player.displayClientMessage(Component.translatable("message.lock.unlocked").withStyle(ChatFormatting.GREEN),true);
                    return InteractionResult.sidedSuccess(level.isClientSide());
                } else {
                    // 上锁逻辑
                    // 检查当前方块状态是否允许上锁（如门必须关闭才能上锁）
                    if (!canLock(level, pos)) {
                        if (!level.isClientSide()) {
                            player.displayClientMessage(Component.translatable("message.lock.cannot_lock_state").withStyle(ChatFormatting.YELLOW), true);
                            onVerifyFailed(level, pos); // 播放失败音效
                        }
                        // 返回 SUCCESS 表示交互已被处理（已提示用户），但不上锁
                        return InteractionResult.sidedSuccess(level.isClientSide());
                    }
                    // 注意：这里返回 SUCCESS 会触发 ModEvents 的拦截，从而阻止门被关上
                    ClientLockCache.updateStatus(pos, lockData);
                    lockData.setLocked(true);
                    onStateChanged(level, pos, true);
                    syncToClients(level, pos);
                    player.displayClientMessage(Component.translatable("message.lock.locked").withStyle(ChatFormatting.RED),true);
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            } else {
                // 钥匙不匹配，视为交互失败，不许打开
                if (!level.isClientSide()) {
                    onVerifyFailed(level, pos);
                }
                return InteractionResult.FAIL;
            }
        }

        // 逻辑分支 B：非钥匙（普通物品或空手）交互
        if (lockData.isLocked()) {
            // 已锁定状态下拦截所有交互
            if (!level.isClientSide()) {
                onVerifyFailed(level, pos);
            }
            return InteractionResult.FAIL;
        }

        // 【关键】未锁定且没拿钥匙，返回 PASS。
        // 这允许玩家在没上锁时正常开关门、打开箱子。
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

    /**
     * 检查当前方块状态是否允许上锁
     * 子类必须实现此方法以定义上锁条件
     * @param level 世界
     * @param pos 方块位置
     * @return 如果允许上锁返回true
     */
    protected abstract boolean canLock(Level level, BlockPos pos);

    /**
     * 客户端调用：自定义渲染逻辑
     * @param poseStack 变换栈
     * @param buffer 渲染缓冲
     * @param pos 方块坐标
     * @param state 方块状态
     * @param packedLight 环境光照
     */
    public void render(PoseStack poseStack, MultiBufferSource buffer, BlockPos pos, BlockState state, int packedLight) {
        // 默认不渲染，由子类实现具体模型
    }

    protected abstract boolean onVerify(Player player, KeyData keyData);
    protected void onStateChanged(Level level, BlockPos pos, boolean locked) {
    }
    public abstract ItemStack getAsStack();
    protected abstract void onVerifyFailed(Level level, BlockPos pos);
    protected abstract KeyData getKeyDataFromStack(ItemStack stack);

    /**
     * 检查是否可以拆卸此锁
     * 子类可以重写以添加自定义拆卸条件（如需要特定工具、特定权限等）
     * @param player 尝试拆卸的玩家
     * @param stack 玩家手持物品
     * @return 如果可以拆卸返回true
     */
    public boolean canRemove(Player player, ItemStack stack) {
        // 默认逻辑：潜行 + 空手 + 锁处于开启状态
        return player.isShiftKeyDown() && stack.isEmpty() && !lockData.isLocked();
    }

    /**
     * 执行拆卸操作，返回拆卸后的锁物品
     * 子类可以重写以自定义掉落物（如损坏的锁、不同物品等）
     * @return 包含锁数据的物品堆
     */
    public ItemStack onRemove() {
        ItemStack dropStack = getAsStack();
        CompoundTag nbt = new CompoundTag();
        lockData.writeToNBT(nbt);
        dropStack.getOrCreateTag().put("LockData", nbt);
        return dropStack;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack poseStack, MultiBufferSource buffer, Level level, BlockPos pos, BlockState state, int packedLight) {
        // 1. 获取模型（默认逻辑：子类可根据 isLocked 返回不同模型）
        BakedModel model = getCustomModel(lockData.isLocked());
        if (model == null) return;

        poseStack.pushPose();

        // 2. 默认变换：根据方块朝向自动贴合表面
        applyDefaultTransforms(poseStack, state);

        // 3. 执行渲染
        VertexConsumer vertexConsumer = buffer.getBuffer(ItemBlockRenderTypes.getRenderType(getAsStack(), true));
        Minecraft.getInstance().getItemRenderer().renderModelLists(model, getAsStack(), packedLight, OverlayTexture.NO_OVERLAY, poseStack, vertexConsumer);

        poseStack.popPose();
    }

    /**
     * 子类重写：返回具体的 BakedModel。若不渲染则返回 null。
     * 默认使用物品本身的物理模型。
     */
    @OnlyIn(Dist.CLIENT)
    protected BakedModel getCustomModel(boolean isLocked) {
        return Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(this.getAsStack());
    }

    /**
     * 默认变换：将锁放置在方块面向玩家的那一面的中心，稍微外移避免深度冲突
     */
    @OnlyIn(Dist.CLIENT)
    protected void applyDefaultTransforms(PoseStack poseStack, BlockState state) {
        poseStack.translate(0.5, 0.5, 0.5);

        // 处理水平朝向
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            float angle = -dir.toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        }

        // 稍微往方块表面外挪一点 (0.5 + 0.01)，缩小模型
        poseStack.translate(0, 0, -0.51);
        poseStack.scale(0.4f, 0.4f, 0.4f);
    }
}
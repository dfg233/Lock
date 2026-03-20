package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.capability.LockCapabilityProvider;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.tags.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

/**
 * 机械锁物品类
 * 负责将锁的数据（LockData）安装到方块的 Capability 中
 */
public class MechanicalLockItem extends Item {
    public MechanicalLockItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        // 仅在服务端执行逻辑，防止客户端与服务端数据不同步（数据同步的权威在服务端）
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);

            // 检查该位置是否存在方块实体（如箱子、熔炉等）
            if (be != null) {
                // 关键：在这里检查 Tag。只有符合标签的方块才允许执行安装逻辑
                if (!be.getBlockState().is(ModBlockTags.LOCKABLE)) {
                    if (player != null) {
                        player.sendSystemMessage(Component.translatable("message.lock.not_lockable"));
                    }
                    return InteractionResult.FAIL;
                }

                // 尝试从方块实体中获取“锁”的能力（Capability）
                be.getCapability(LockCapabilityProvider.LOCK_CAP).ifPresent(cap -> {
                    LockData data = cap.getLockData();
                    if (data.getLockId() == null) {
                        data.setLockId(UUID.randomUUID()); // 必须生成 ID！
                        data.setLocked(true);
                        data.setLockType("mechanical");

                        stack.shrink(1);
                        be.setChanged(); // 必须通知系统存档

                        if (player != null) {
                            player.sendSystemMessage(Component.translatable("message.lock.success"));
                        }
                    } else {
                        if (player != null) {
                            player.sendSystemMessage(Component.translatable("message.lock.already_locked"));
                        }
                    }
                });

                /* * 核心步骤：标记方块实体已更新。
                 * 如果不调用此方法，Minecraft 可能会认为方块没变化而不保存新数据到硬盘。
                 */
                be.setChanged();
            }
        }

        // 返回操作结果：在服务端返回 SUCCESS，客户端返回 CONSUME，并触发手部摆动动画
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
package com.dfg233.lock.item.custom.lock;

import com.dfg233.lock.capability.LockCapabilityProvider;
import com.dfg233.lock.data.LockData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        BlockEntity be = level.getBlockEntity(pos);

        if (!level.isClientSide()) {
            if (be != null && be.getBlockState().is(ModBlockTags.LOCKABLE)) {
                be.getCapability(LockCapabilityProvider.LOCK_CAP).ifPresent(cap -> {
                    if (!cap.hasLock()) {
                        // 1. 设置基础数据
                        LockData data = cap.getLockData();
                        data.setLockType("mechanical"); // 标记这现在是一把机械锁

                        // 2. 利用工厂创建逻辑实例
                        AbstractLock lock = AbstractLock.create(data);

                        // 3. 执行安装逻辑
                        if (lock != null) {
                            lock.onInstalled(player);

                            // 关键：构建 NBT 并发送同步包
                            CompoundTag nbt = new CompoundTag();
                            data.writeToNBT(nbt);
                            ModMessages.sendToClients(new S2CSyncLockPacket(pos, nbt));


                            stack.shrink(1); // 消耗锁物品
                            be.setChanged(); // 存档
                            if (player != null) {
                                player.sendSystemMessage(Component.translatable("message.lock.success"));
                            }
                        }
                    }
                });
            }else {
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("message.lock.not_lockable"));
                }
            }
        }
        // 返回操作结果：在服务端返回 SUCCESS，客户端返回 CONSUME，并触发手部摆动动画
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
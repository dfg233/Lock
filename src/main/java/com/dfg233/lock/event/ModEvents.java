package com.dfg233.lock.event;

import com.dfg233.lock.Lock;
import com.dfg233.lock.client.ClientLockCache;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.data.LockLevelData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.dfg233.lock.Utils.LockUtils.getActualLockPos;

@Mod.EventBusSubscriber(modid = Lock.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockPos actualPos = getActualLockPos(level, pos); // 获取实际锁坐标（如门下半部分）
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // --- 1. 前置拦截 (解决动画闪烁与非法交互) ---
        boolean isKey = stack.getItem() instanceof com.dfg233.lock.item.custom.key.KeyItem;
        boolean isLockedClient = level.isClientSide() && ClientLockCache.isLocked(actualPos);

        // 终极拦截条件：只要是锁定的，或者玩家正拿着钥匙准备操作
        // 这样可以确保 DoorBlock#use 根本没机会执行
        if (isLockedClient || isKey) {
            // 强制取消事件并告知系统：此交互已由插件成功处理，不要执行原版方块逻辑
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));

            // 如果是客户端且没有拿钥匙（只是普通点击），到这里就可以结束了
            if (level.isClientSide() && !isKey) {
                return;
            }
        }

        if (!isLockedClient && stack.isEmpty() &&player.isShiftKeyDown()) {
            event.setCanceled(true);
            event.setResult(Event.Result.DENY);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));

            if (level.isClientSide()) {
                return;
            }
        }

        // --- 2. 业务处理 (服务端逻辑核心) ---
        // 注意：由于上面可能 Canceled 了事件，但 Forge 依然允许在服务端运行后续代码
        // 我们需要通过 LockLevelData 进行实际的逻辑校验
        LockLevelData worldData = LockLevelData.get(level);
        if (worldData != null) {
            LockData data = worldData.getLock(actualPos);
            if (data != null) {

                // 条件：潜行 + 空手 + 锁处于开启状态
                if (player.isShiftKeyDown() && stack.isEmpty() && !data.isLocked()) {
                    if (!level.isClientSide()) {
                        AbstractLock lock = AbstractLock.create(data);
                        if (lock != null) {
                            // 1. 生成带有原有 NBT 数据的锁物品
                            ItemStack dropStack = lock.getAsStack();
                            CompoundTag nbt = new CompoundTag();
                            data.writeToNBT(nbt);
                            dropStack.getOrCreateTag().put("LockData", nbt);

                            // 2. 核心修改：尝试放入玩家背包
                            // 如果背包满了，addItem 会返回 false，此时再执行掉落逻辑作为兜底
                            if (!player.getInventory().add(dropStack)) {
                                Block.popResource(level, actualPos, dropStack);
                            }

                            // 3. 从世界中移除并通知客户端
                            worldData.removeLock(actualPos);
                            ModMessages.sendToClients(new S2CSyncLockPacket(actualPos, new CompoundTag()));

                            // 4. 反馈
                            player.displayClientMessage(Component.translatable("message.lock.removed").withStyle(ChatFormatting.YELLOW), true);
                        }
                    }

                    // 拦截后续交互逻辑
                    event.setCanceled(true);
                    event.setResult(Event.Result.DENY);
                    event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
                }

                AbstractLock lock = AbstractLock.create(data);
                if (lock != null) {
                    // 执行开/上锁逻辑 (tryInteract 内部应处理好 syncToClients 和音效)
                    InteractionResult result = lock.tryInteract(player, level, actualPos, stack);

                    if (!level.isClientSide() && result == InteractionResult.FAIL) {
                        // 判断是否拿着钥匙物品
                        boolean isHoldingKey = stack.getItem() instanceof com.dfg233.lock.item.custom.key.KeyItem;

                        if (isHoldingKey) {
                            // 场景 1：拿着钥匙但失败了 -> 提示钥匙错误
                            player.displayClientMessage(
                                    Component.translatable("message.lock.wrong_key").withStyle(ChatFormatting.DARK_RED),
                                    true
                            );
                        } else if (data.isLocked()) {
                            // 场景 2：没拿钥匙且方块已锁定 -> 提示已上锁
                            player.displayClientMessage(
                                    Component.translatable("message.lock.is_locked").withStyle(ChatFormatting.RED),
                                    true
                            );
                        }
                    }

                    // 确保结果同步：如果业务逻辑判定为成功或失败（非PASS），再次强制取消事件
                    if (result != InteractionResult.PASS) {
                        event.setCanceled(true);
                        event.setResult(Event.Result.DENY);
                        event.setCancellationResult(result);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos clickedPos = event.getPos();
        BlockPos actualPos = getActualLockPos(level, clickedPos);

        if (!level.isClientSide()) {
            LockLevelData worldData = LockLevelData.get(level);
            LockData data = worldData.getLock(actualPos);

            if (data != null) {
                // 1. 使用工厂方法创建对应的锁逻辑对象
                AbstractLock lock = AbstractLock.create(data);
                if (lock != null) {
                    // 2. 动态获取对应的物品堆叠（不再硬编码 Mechanical）
                    ItemStack dropStack = lock.getAsStack();

                    // 3. 将现有的 NBT 数据（UUID、锁定状态、配置等）完整写入物品
                    CompoundTag nbt = new CompoundTag();
                    data.writeToNBT(nbt);
                    dropStack.getOrCreateTag().put("LockData", nbt);

                    // 4. 从世界数据中移除并同步
                    worldData.removeLock(actualPos);
                    ModMessages.sendToClients(new S2CSyncLockPacket(actualPos, new CompoundTag()));

                    // 5. 在破坏位置弹出对应的锁物品
                    Block.popResource(level, clickedPos, dropStack);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;

        BlockPos placedPos = event.getPos();
        // 4. 防御性检测：当玩家放置新箱子试图与有锁的箱子合并时
        event.getPlacedBlock().getBlock();
        BlockPos actualPos = getActualLockPos(level, placedPos);

        // 如果放置的位置不是主坐标，说明合并到了一个已存在的箱子上
        if (!placedPos.equals(actualPos)) {
            LockLevelData worldData = LockLevelData.get(level);
            if (worldData != null && worldData.getLock(actualPos) != null) {
                if (event.getEntity() instanceof Player player) {
                    // 提示玩家合并后的区域已受锁保护
                    player.displayClientMessage(Component.translatable("message.lock.merged_protection").withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }
}
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
        BlockPos actualPos = getActualLockPos(level, event.getPos());
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // --- 关键修复：客户端拦截 ---
        if (level.isClientSide()) {
            // 如果客户端缓存显示该位置已锁定，且玩家没拿钥匙，直接取消事件
            // 这会阻止客户端的“开门预测”动画
            if (ClientLockCache.isLocked(actualPos)) {
                // 如果玩家拿的是钥匙且 UUID 匹配（这里可以简化判断，只要锁着就先拦截）
                // 如果你的逻辑允许“未开锁前不能点”，这里直接 cancel
                event.setCanceled(true);
                event.setResult(Event.Result.DENY);
                return;
            }
            return;
        }

// --- 服务端逻辑 ---
        LockLevelData worldData = LockLevelData.get(level);
        LockData data = worldData.getLock(actualPos);

        if (data != null) {
            AbstractLock lock = AbstractLock.create(data);
            if (lock != null) {
                InteractionResult result = lock.tryInteract(player, level, actualPos, stack);

                // 如果交互失败（即锁着且钥匙不对），必须取消事件
                if (result == InteractionResult.FAIL) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                } else if (result == InteractionResult.SUCCESS) {
                    // 如果开关锁成功，也要取消事件，防止门在开关锁的同时自己跳动
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
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
                    player.displayClientMessage(Component.translatable("message.lock.merged_protection").withStyle(ChatFormatting.RED), true);
                }
            }
        }
    }
}
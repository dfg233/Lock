package com.dfg233.lock.event;

import com.dfg233.lock.Lock;
import com.dfg233.lock.client.ClientLockCache;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.data.LockLevelData;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.item.ModItems;
import com.dfg233.lock.network.ModMessages;
import com.dfg233.lock.network.S2CSyncLockPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lock.MODID)
public class ModEvents {

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos actualPos = LockLevelData.LockUtils.getActualLockPos(level, event.getPos());
        Player player = event.getEntity();

        // 客户端拦截：解决双重音效的关键
        if (level.isClientSide()) {
            if (ClientLockCache.isLocked(actualPos)) {
                // 如果本地缓存显示已锁定，且玩家没拿钥匙（这里可以进一步细化判断）
                // 强制拦截，阻止客户端播放音效和动画
                event.setCanceled(true);
            }
            return;
        }

        // 服务端逻辑
        LockLevelData worldData = LockLevelData.get(level);
        LockData data = worldData.getLock(actualPos);

        if (data != null && data.isLocked()) {
            AbstractLock lock = AbstractLock.create(data);
            if (lock != null) {
                if (lock.tryInteract(player, level, actualPos, event.getItemStack()) == InteractionResult.FAIL) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos actualPos = LockLevelData.LockUtils.getActualLockPos(level, event.getPos());

        if (!level.isClientSide()) {
            LockLevelData worldData = LockLevelData.get(level);
            if (worldData != null && worldData.getLock(actualPos) != null) {
                worldData.removeLock(actualPos);
                ModMessages.sendToClients(new S2CSyncLockPacket(actualPos, new CompoundTag()));
                Block.popResource(level, actualPos, new ItemStack(ModItems.MECHANICAL_LOCK.get()));
            }
        }
    }
}
package com.dfg233.lock.event;

import com.dfg233.lock.Lock;
import com.dfg233.lock.capability.LockCapabilityProvider;
import com.dfg233.lock.sounds.PlayFailureSound;
import com.dfg233.lock.tags.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lock.MODID)
public class ModEvents {
    /**
     * 当方块实体被创建并附加能力时触发此事件。
     * 该方法用于将自定义的 {@link LockCapabilityProvider} 能力提供者附加到所有方块实体上。
     * 这使得方块实体能够存储和管理与“锁定”相关的数据，从而实现方块的锁定功能。
     * 能力键为 "lockable"，通过该键可以访问方块的锁定状态。
     *
     * @param event AttachCapabilitiesEvent<BlockEntity> 事件对象，包含要附加能力的方块实体。
     */
    @SubscribeEvent
    public static void onBlockEntityRegistry(AttachCapabilitiesEvent<BlockEntity> event) {
//        if(event.getObject().getBlockState().is(ModBlockTags.LOCKABLE)){
            event.addCapability(ResourceLocation.fromNamespaceAndPath(Lock.MODID, "lockable"), new LockCapabilityProvider());
//        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        // 注意：这里去掉了 !isClientSide 的限制，让客户端也能同步拦截动画
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            be.getCapability(LockCapabilityProvider.LOCK_CAP).ifPresent(cap -> {
                System.out.println("检测交互 - 是否有锁: " + cap.hasLock() + " | 是否锁定: " + cap.getLockData().isLocked());
                // 打印日志：区分一下是哪个端在运行
                String side = level.isClientSide() ? "客户端" : "服务端";
                System.out.println(side + "检测 - 是否锁定: " + cap.getLockData().isLocked());
                if (cap.getLockData().isLocked()) {
                    // 1. 拦截交互
                    event.setCanceled(true);

                    // 2. 仅在服务端播放声音（防止声音重叠或在客户端重复触发）
                    if (!level.isClientSide()) {
                        PlayFailureSound.play(level, pos, cap.getLockData());
                    }
                }
            });
        }
    }


}

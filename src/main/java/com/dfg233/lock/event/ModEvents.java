package com.dfg233.lock.event;

import com.dfg233.lock.Lock;
import com.dfg233.lock.capability.LockCapabilityProvider;
import com.dfg233.lock.item.AbstractLock;
import com.dfg233.lock.tags.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
        event.addCapability(ResourceLocation.fromNamespaceAndPath(Lock.MODID, "lockable"),
                new LockCapabilityProvider());
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        Player player = event.getEntity();

        if (be != null) {
            boolean isLockable = be.getBlockState().is(ModBlockTags.LOCKABLE);

            be.getCapability(LockCapabilityProvider.LOCK_CAP).ifPresent(cap -> {
                System.out.println("检测交互 - 是否有锁: " + cap.hasLock() + " | 是否锁定: " + cap.getLockData().isLocked());
                // 打印日志：区分一下是哪个端在运行
                String side = level.isClientSide() ? "客户端" : "服务端";
                System.out.println(side + "检测 - 是否锁定: " + cap.getLockData().isLocked());
                if (cap.getLockData().isLocked()) {
                    AbstractLock lock = AbstractLock.create(cap.getLockData());
                    if (lock.tryInteract(player, level, pos, event.getItemStack()) == InteractionResult.FAIL) {
                        //拦截交互
                        event.setCanceled(true);                        ;
                    }
                }
            });
        }
    }
}

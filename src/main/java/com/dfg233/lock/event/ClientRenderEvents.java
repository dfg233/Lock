package com.dfg233.lock.event;

import com.dfg233.lock.Lock;
import com.dfg233.lock.client.ClientLockCache;
import com.dfg233.lock.data.LockData;
import com.dfg233.lock.item.AbstractLock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Lock.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientRenderEvents {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 在粒子渲染阶段之后绘制，确保不会被方块遮挡，且支持透明度
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        for (BlockPos pos : ClientLockCache.getAllLockedPositions()) {
            // 1. 距离裁剪：只渲染 16 格内的锁，优化性能
            if (!pos.closerThan(player.blockPosition(), 16)) continue;

            LockData data = ClientLockCache.getLockData(pos);
            if (data == null) continue;

            // 2. 利用多态工厂创建 AbstractLock 实例
            AbstractLock lock = AbstractLock.create(data);
            if (lock == null) continue;

            BlockState state = level.getBlockState(pos);

            poseStack.pushPose();
            // 3. 将坐标系从世界原点移动到方块相对于相机的当前位置
            poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

            // 4. 获取方块位置的光照信息
            int light = LevelRenderer.getLightColor(level, pos);

            // 5. 调用渲染逻辑
            lock.render(poseStack, buffer, level, pos, state, light);

            poseStack.popPose();
        }
        // 强制刷新缓冲区，完成绘制
        buffer.endBatch();
    }
}
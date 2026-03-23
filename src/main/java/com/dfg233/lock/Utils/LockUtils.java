package com.dfg233.lock.Utils;

import com.dfg233.lock.data.LockLevelData; // 确保导入
import com.dfg233.lock.client.ClientLockCache; // 确保导入
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class LockUtils {
    public static BlockPos getActualLockPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // 1. 处理门：逻辑不变，门的数据永远在下半部分
        if (state.hasProperty(DoorBlock.HALF)) {
            if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                return pos.below();
            }
            return pos;
        }

        // 2. 处理大箱子：双向探测逻辑
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                Direction connectedDir = ChestBlock.getConnectedDirection(state);
                BlockPos peerPos = pos.relative(connectedDir);

                // 【核心修改】：优先寻找真正存有数据的位置
                // 检查当前点击的位置是否有锁
                if (hasLockAt(level, pos)) {
                    return pos;
                }
                // 如果当前位置没锁，检查相连的另一半是否有锁
                if (hasLockAt(level, peerPos)) {
                    return peerPos;
                }

                // 如果都没有锁（说明是新箱子），则回退到坐标值判定，保证安装时的一致性
                return pos.asLong() < peerPos.asLong() ? pos : peerPos;
            }
        }

        return pos;
    }

    // 辅助方法：判断某个位置是否有锁（同时兼容客户端和服务端）
    private static boolean hasLockAt(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return ClientLockCache.isLocked(pos);
        } else {
            LockLevelData worldData = LockLevelData.get(level);
            return worldData != null && worldData.getLock(pos) != null;
        }
    }
}
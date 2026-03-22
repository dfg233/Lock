package com.dfg233.lock.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

// 内部工具类：处理多格方块
public class LockUtils {
    public static BlockPos getActualLockPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // 处理门：始终重定向到下半部分
        if (state.hasProperty(DoorBlock.HALF)) {
            if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                return pos.below();
            }
            // 如果点的是 LOWER，直接返回 pos
            return pos;
        }

        // 2. 处理大箱子逻辑
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                // 获取与之相连的另一个箱子的方向
                Direction connectedDir = ChestBlock.getConnectedDirection(state);
                BlockPos peerPos = pos.relative(connectedDir);

                // 核心策略：比较两个坐标的 Long 值，始终以较小的坐标作为数据存储点
                // 这样无论点击大箱子的哪一半，都会指向同一个唯一的坐标
                return pos.asLong() < peerPos.asLong() ? pos : peerPos;
            }
        }

        return pos;
    }
}

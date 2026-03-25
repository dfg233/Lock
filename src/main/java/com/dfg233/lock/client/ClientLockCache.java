package com.dfg233.lock.client;

import com.dfg233.lock.data.LockData;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientLockCache {
    private static final Map<BlockPos, LockData> LOCKED_DATA = new HashMap<>();

    public static void updateStatus(BlockPos pos, LockData data) {
        LOCKED_DATA.put(pos, data);
    }

    public static void remove(BlockPos pos) {
        LOCKED_DATA.remove(pos);
    }

    public static LockData getLockData(BlockPos pos) {
        return LOCKED_DATA.get(pos);
    }

    public static boolean isLocked(BlockPos pos) {
        LockData data = LOCKED_DATA.get(pos);
        return data != null && data.isLocked();
    }

    // 新增：供渲染器遍历所有上锁的位置
    public static Set<BlockPos> getAllLockedPositions() {
        return LOCKED_DATA.keySet();
    }
}
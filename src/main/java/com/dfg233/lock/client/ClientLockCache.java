package com.dfg233.lock.client;

import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;

public class ClientLockCache {
    private static final Map<BlockPos, Boolean> LOCKED_STATUS = new HashMap<>();

    public static void updateStatus(BlockPos pos, boolean locked) {
        LOCKED_STATUS.put(pos, locked);
    }

    public static void remove(BlockPos pos) {
        LOCKED_STATUS.remove(pos);
    }

    public static boolean isLocked(BlockPos pos) {
        return LOCKED_STATUS.getOrDefault(pos, false);
    }
}
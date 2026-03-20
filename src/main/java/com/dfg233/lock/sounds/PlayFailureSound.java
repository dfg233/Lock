package com.dfg233.lock.sounds;

import com.dfg233.lock.data.LockData;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public class PlayFailureSound {
    public static void play(Level level, BlockPos pos, LockData lockData) {
        // 根据锁的类型播放不同的失败音效
        if ("mechanical".equals(lockData.getLockType())) {
            // 播放铁门的金属撞击声，模拟锁孔被堵住
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.5F);
        } else {
            // 默认失败音效
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
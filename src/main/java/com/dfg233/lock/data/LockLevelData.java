package com.dfg233.lock.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

public class LockLevelData extends SavedData {
    private final Map<BlockPos, LockData> locks = new HashMap<>();

    public static LockLevelData get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            DimensionDataStorage storage = serverLevel.getDataStorage();
            return storage.computeIfAbsent(LockLevelData::load, LockLevelData::new, "lock_registry");
        }
        return null;
    }

    public void addLock(BlockPos pos, LockData data) {
        locks.put(pos, data);
        setDirty();
    }

    public void removeLock(BlockPos pos) {
        if (locks.remove(pos) != null) setDirty();
    }

    public LockData getLock(BlockPos pos) {
        return locks.get(pos);
    }

    public static LockLevelData load(CompoundTag nbt) {
        LockLevelData data = new LockLevelData();
        ListTag list = nbt.getList("Locks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            LockData lock = new LockData();
            lock.readFromNBT(entry.getCompound("data"));
            data.locks.put(pos, lock);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        locks.forEach((pos, data) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("pos", pos.asLong());
            CompoundTag lockTag = new CompoundTag();
            data.writeToNBT(lockTag);
            entry.put("data", lockTag);
            list.add(entry);
        });
        nbt.put("Locks", list);
        return nbt;
    }

    // 内部工具类：处理多格方块
    public static class LockUtils {
        public static BlockPos getActualLockPos(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock) {
                if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                    return pos.below();
                }
            }
            return pos;
        }
    }
}
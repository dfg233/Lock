package com.dfg233.lock.capability;

import com.dfg233.lock.data.LockData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

//当游戏询问某个方块“你有没有锁”时，这个类会负责递出数据
public class LockCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static Capability<ILockable> LOCK_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    // 唯一的实例，所有操作都指向它
    private final LockData lockData = new LockData();

    private final LazyOptional<ILockable> instance = LazyOptional.of(() -> new ILockable() {
        @Override
        public LockData getLockData() { return lockData; }

        @Override
        public void setLockData(LockData data) {
            // 不要替换对象引用，而是复制数据，这样最稳妥
            lockData.setLockId(data.getLockId());
            lockData.setLocked(data.isLocked());
            lockData.setLockType(data.getLockType());
        }

        @Override
        public boolean hasLock() {
            return lockData.getLockId() != null;
        }
    });
    //核心方法：游戏通过这个方法询问“你有这个能力吗？”
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return LOCK_CAP.orEmpty(cap, instance);
    }

    //保存数据到存档
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        lockData.writeToNBT(nbt);
        return nbt;
    }

    //从存档读取数据
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        System.out.println("正在从存档读取 NBT 数据...");
        lockData.readFromNBT(nbt);
    }
}

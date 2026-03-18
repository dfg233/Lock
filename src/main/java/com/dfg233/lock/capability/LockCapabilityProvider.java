package com.dfg233.lock.capability;

import com.dfg233.lock.data.LockData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class LockCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    // 1. 定义一个“钥匙”，用来在茫茫数据中找到我们的 Capability
    public static Capability<ILockable> LOCK_CAP = CapabilityManager.get(new CapabilityToken<>() {});

    // 2. 准备好具体的实现
    private LockData lockData = new LockData();
    private final LazyOptional<ILockable> instance = LazyOptional.of(() -> new ILockable() {
        @Override
        public LockData getLockData() { return lockData; }
        @Override
        public void setLockData(LockData data) { lockData = data; }
        @Override
        public boolean hasLock() {
            // 如果 ID 为空或者处于某种初始状态，可以认为没锁
            return lockData.getLockId() != null;
        }
    });

    // 3. 核心方法：游戏通过这个方法询问“你有这个能力吗？”
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return LOCK_CAP.orEmpty(cap, instance);
    }

    // 4. 保存数据到存档
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        lockData.writeToNBT(nbt);
        return nbt;
    }

    // 5. 从存档读取数据
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        lockData.readFromNBT(nbt);
    }
}

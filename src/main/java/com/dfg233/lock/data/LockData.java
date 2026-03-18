package com.dfg233.lock.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class LockData {
    private boolean isLocked = false;
    private String lockType = "";
    private UUID lockId = null;//每把锁都有对应的UUID方便识别对应的钥匙和复制
    private CompoundTag keyData = new CompoundTag();

    public LockData() {
        this.isLocked = false;
        this.lockType = "";
    }

    //NBT存放和读取
    public void writeToNBT(CompoundTag tag) {
        tag.putBoolean("isLocked", this.isLocked);
        tag.putString("lockType", this.lockType);
        if (this.lockId != null) {
            tag.putUUID("lockId", this.lockId);
        }
        tag.put("keyData", this.keyData);
    }

    public void readFromNBT(CompoundTag tag) {
        this.isLocked = tag.getBoolean("isLocked");
        this.lockType = tag.getString("lockType");
        this.lockId = tag.hasUUID("lockId") ? tag.getUUID("lockId") : UUID.randomUUID();
        this.keyData = tag.getCompound("keyData");
    }

    //get方法
    public boolean isLocked() {
        return this.isLocked;
    }
    public String getLockType() {
        return this.lockType;
    }
    public UUID getLockId() {
        return this.lockId;
    }
    public CompoundTag getKeyData() {
        return this.keyData;
    }
    //set方法
    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }
    public void setLockType(String lockType) {
        this.lockType = lockType;
    }
    public void setLockId(UUID lockId) {
        this.lockId = lockId;
    }
    public void setKeyData(CompoundTag keyData) {
        this.keyData = keyData;
    }
}

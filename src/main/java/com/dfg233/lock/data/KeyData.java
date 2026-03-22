package com.dfg233.lock.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class KeyData {
    private UUID lockId;
    private String keyType;

    public KeyData(UUID lockId, String keyType) {
        this.lockId = lockId;
        this.keyType = keyType;
    }

    public UUID getLockId() {
        return lockId;
    }
    public String getKeyType() {
        return keyType;
    }

    public void setLockId(UUID lockId) {
        this.lockId = lockId;
    }
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    public void writeToNBT(CompoundTag tag) {
        tag.putUUID("lockId", lockId);
        tag.putString("keyType", keyType);
    }
    public void readFromNBT(CompoundTag tag) {
        this.lockId = tag.hasUUID("lockId") ? tag.getUUID("lockId") : null;
        this.keyType = tag.getString("keyType");
    }
}

package com.dfg233.lock.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class KeyData {
    private UUID keyId = null;

    public UUID getKeyId() {
        return keyId;
    }
    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }


    public void writeToNBT(CompoundTag tag) {
        tag.putUUID("keyId", keyId);
    }
    public void readFromNBT(CompoundTag tag) {
        this.keyId = tag.getUUID("keyId");
    }
}

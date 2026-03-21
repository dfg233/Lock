package com.dfg233.lock.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class KeyData {
    private UUID keyId = null;
    private String keyType = "";

    public UUID getKeyId() {
        return keyId;
    }
    public void setKeyId(UUID keyId) {
        this.keyId = keyId;
    }
    public String getKeyType() {return keyType;}
    public void setKeyType(String keyType) {this.keyType = keyType;}


    public void writeToNBT(CompoundTag tag) {
        tag.putUUID("keyId", keyId);
    }
    public void readFromNBT(CompoundTag tag) {
        this.keyId = tag.hasUUID("keyId") ? tag.getUUID("keyId") : null;
    }
}

package com.dfg233.lock.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class LockData{
    // ================= 字段定义 =================

    /**
     * 锁的状态：true = 已上锁，false = 未上锁
     */
    private boolean locked = false;

    /**
     * 钥匙的类型标识 (用于调试或快速路由)
     * 示例: "PLAYER_UUID", "ITEM_NBT_HASH", "PASSCODE_SHA256", "REDSTONE_LEVEL"
     */
    private String keyType = "";

    /**
     * 钥匙的唯一标识符 (指纹)
     * 这是经过子类处理后的字符串，用于匹配。
     * 例如: 玩家的 UUID 字符串, 物品的 NBT 哈希值, 密码的 Hash 等。
     */
    private String keyIdentifier = "";

    /**
     * 锁类型的注册表 ID (例如: "universallock:iron_lock")
     * 用于客户端识别渲染类型和服务端识别逻辑类
     */
    private String lockTypeRegistryId = "";

    /**
     * 自定义数据容器
     * 用于扩展功能（如：密码锁的哈希值、一次性锁的使用次数、魔法锁的充能值等）
     * 由具体的锁物品子类负责读写此数据
     */
    private CompoundTag customData;

    public LockData(){
        this.customData = new CompoundTag();
    }

    // ================= NBT 读写 =================

    public void readFromNBT(CompoundTag tag) {
        if (tag.contains("Locked", 1)) this.locked = tag.getBoolean("Locked");
        if (tag.contains("KeyType", 8)) this.keyType = tag.getString("KeyType");
        if (tag.contains("KeyIdentifier", 8)) this.keyIdentifier = tag.getString("KeyIdentifier");
        if (tag.contains("LockType", 8)) this.lockTypeRegistryId = tag.getString("LockType");
        if (tag.contains("CustomData", 10)) this.customData = tag.getCompound("CustomData");
        else this.customData = new CompoundTag();
    }

    public CompoundTag writeToNBT(CompoundTag tag) {
        tag.putBoolean("Locked", this.locked);
        tag.putString("KeyType", this.keyType);
        tag.putString("KeyIdentifier", this.keyIdentifier);
        tag.putString("LockType", this.lockTypeRegistryId != null ? this.lockTypeRegistryId : "");
        tag.put("CustomData", this.customData);
        return tag;
    }

    // ================= 网络同步 =================

    public void toPacket(FriendlyByteBuf buf) {
        buf.writeBoolean(this.locked);
        buf.writeUtf(this.lockTypeRegistryId != null ? this.lockTypeRegistryId : "", 32767);
        buf.writeUtf(this.keyType, 128); // 类型名通常很短
        buf.writeUtf(this.keyIdentifier, 32767); // 标识符可能较长 (如 Hash)
        buf.writeNbt(this.customData);
    }

    public static LockData fromPacket(FriendlyByteBuf buf) {
        LockData data = new LockData();
        data.setLocked(buf.readBoolean());
        data.setLockType(buf.readUtf(32767));
        data.setKeyType(buf.readUtf(128));
        data.setKeyIdentifier(buf.readUtf(32767));
        try {
            CompoundTag tag = buf.readNbt();
            if (tag != null) data.customData = tag;
        } catch (Exception e) { data.customData = new CompoundTag(); }
        return data;
    }

    // ================= Getter & Setter =================

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }

    public String getKeyIdentifier() { return keyIdentifier; }
    public void setKeyIdentifier(String keyIdentifier) { this.keyIdentifier = keyIdentifier; }

    public String getLockType() { return lockTypeRegistryId; }
    public void setLockType(String lockTypeRegistryId) { this.lockTypeRegistryId = lockTypeRegistryId; }

    public CompoundTag getCustomData() { return customData; }
    public void setCustomData(CompoundTag customData) { this.customData = customData != null ? customData : new CompoundTag(); }

    public void clear() {
        this.locked = false;
        this.keyType = "";
        this.keyIdentifier = "";
        this.lockTypeRegistryId = "";
        this.customData = new CompoundTag();
    }

    @Override
    public String toString() {
        return "LockData{locked=" + locked + ", type='" + keyType + "', id='" +
                (keyIdentifier.length() > 10 ? keyIdentifier.substring(0, 10) + "..." : keyIdentifier) + "'}";
    }

}

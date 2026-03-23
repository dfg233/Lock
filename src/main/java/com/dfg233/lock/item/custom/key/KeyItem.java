package com.dfg233.lock.item.custom.key;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class KeyItem extends Item {
    private final String keyType;

    public KeyItem(Properties pProperties, String keyType) {
        super(pProperties);
        this.keyType = keyType;
    }

    public String getKeyType() {
        return keyType;
    }

    // 静态工具：绑定 UUID
    public static void bindToLock(ItemStack stack, UUID lockId) {
        if (lockId != null) {
            stack.getOrCreateTag().putUUID("lockId", lockId);
        }
    }

    // 静态工具：读取 UUID
    public static @Nullable UUID getLockId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("lockId")) {
            return stack.getTag().getUUID("lockId");
        }
        return null;
    }
}
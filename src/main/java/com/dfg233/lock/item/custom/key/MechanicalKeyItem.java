package com.dfg233.lock.item.custom.key;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MechanicalKeyItem extends KeyItem {
    public MechanicalKeyItem(Properties pProperties) {
        // 固定传入 "mechanical" 类型
        super(pProperties, "mechanical");
    }
    // 这里可以重写方法来添加机械钥匙特有的逻辑，比如配钥匙动画
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pIsAdvanced) {
        UUID id = getLockId(pStack);
        // 显示钥匙所属的物理类型（机械/电子等）
        pTooltip.add(Component.translatable("tooltip.lock.key_type")
                .append(": ")
                .append(Component.translatable("tooltip.lock.type.mechanical").withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        if (id != null) {
            pTooltip.add(Component.translatable("tooltip.lock.key_id")
                    .append(": ")
                    .append(Component.literal(id.toString().substring(0, 16)).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            pTooltip.add(Component.translatable("tooltip.lock.key_blank")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}

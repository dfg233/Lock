package com.dfg233.lock.item.custom.key;

public class MechanicalKeyItem extends KeyItem {
    public MechanicalKeyItem(Properties pProperties) {
        // 固定传入 "mechanical" 类型
        super(pProperties, "mechanical");
    }
    // 这里可以重写方法来添加机械钥匙特有的逻辑，比如配钥匙动画
}

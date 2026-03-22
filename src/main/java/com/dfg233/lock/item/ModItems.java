package com.dfg233.lock.item;

import com.dfg233.lock.Lock;
import com.dfg233.lock.item.custom.lock.MechanicalLockItem;
import com.dfg233.lock.item.custom.key.MechanicalKeyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Lock.MODID);
    //物品注册

    //锁注册
    public static final RegistryObject<Item> MECHANICAL_LOCK = ITEMS.register("mechanical_lock", () -> new MechanicalLockItem(new Item.Properties().stacksTo(1)));

    //钥匙注册
    public static final RegistryObject<Item> MECHANICAL_KEY = ITEMS.register("mechanical_key", () -> new MechanicalKeyItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

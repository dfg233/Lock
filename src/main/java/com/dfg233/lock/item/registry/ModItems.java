package com.dfg233.lock.item.registry;

import com.dfg233.lock.Lock;
import com.dfg233.lock.item.registry.custom.MechanicalLockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Lock.MODID);

    public static final RegistryObject<Item> ICON = ITEMS.register("icon",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MECHANICAL_LOCK = ITEMS.register("mechanical_lock",
            () -> new MechanicalLockItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

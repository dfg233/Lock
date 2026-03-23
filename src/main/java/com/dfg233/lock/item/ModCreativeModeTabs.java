package com.dfg233.lock.item;

import com.dfg233.lock.Lock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Lock.MODID);

    public static final RegistryObject<CreativeModeTab> LOCK_TAB =
            CREATIVE_MODE_TABS.register("lock_tab",() -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MECHANICAL_LOCK.get()))
                    .title(Component.translatable("item_group.lock_tab"))
                    .displayItems((pParameters,pOutput) ->{
                        pOutput.accept(ModItems.MECHANICAL_LOCK.get());
                        pOutput.accept(ModItems.MECHANICAL_KEY.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

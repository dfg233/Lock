package com.dfg233.lock.item.registry;

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

    public static final RegistryObject<CreativeModeTab> TEST_MOD_TAB =
            CREATIVE_MODE_TABS.register("lock_tab",() -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.ICON.get()))
                    .title(Component.translatable("lock_tab"))
                    .displayItems((pParameters,pOutput) ->{
                        pOutput.accept(ModItems.ICON.get());
                        pOutput.accept(ModItems.MECHANICAL_LOCK.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

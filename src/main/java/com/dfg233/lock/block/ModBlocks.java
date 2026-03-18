package com.dfg233.lock.block;

import com.dfg233.lock.Lock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> LOCK_BLOCKS = DeferredRegister.create(Registries.BLOCK, Lock.MODID);


    public static void register(IEventBus eventBus) {
        LOCK_BLOCKS.register(eventBus);
    }
}

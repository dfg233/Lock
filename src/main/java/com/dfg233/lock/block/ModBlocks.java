package com.dfg233.lock.block;

import com.dfg233.lock.Lock;
import com.dfg233.lock.block.custom.KeyDuplicatorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> LOCK_BLOCKS = DeferredRegister.create(Registries.BLOCK, Lock.MODID);

    // 钥匙复制台方块
    public static final RegistryObject<Block> KEY_DUPLICATOR = LOCK_BLOCKS.register("key_duplicator",
            () -> new KeyDuplicatorBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE)));

    public static void register(IEventBus eventBus) {
        LOCK_BLOCKS.register(eventBus);
    }
}

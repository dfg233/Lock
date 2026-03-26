package com.dfg233.lock.block.entity;

import com.dfg233.lock.Lock;
import com.dfg233.lock.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Lock.MODID);

    public static final RegistryObject<BlockEntityType<KeyDuplicatorBlockEntity>> KEY_DUPLICATOR =
            BLOCK_ENTITIES.register("key_duplicator", () ->
                    BlockEntityType.Builder.of(KeyDuplicatorBlockEntity::new,
                            ModBlocks.KEY_DUPLICATOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

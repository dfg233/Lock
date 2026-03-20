package com.dfg233.lock.tags;

import com.dfg233.lock.Lock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {

    public static final TagKey<Block> LOCKABLE = create("lockable");

    private static TagKey<Block> create(String pName) {
        return TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(Lock.MODID, pName));
    }
}

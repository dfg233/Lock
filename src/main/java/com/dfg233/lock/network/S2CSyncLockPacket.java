package com.dfg233.lock.network;

import com.dfg233.lock.client.ClientLockCache;
import com.dfg233.lock.data.LockData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncLockPacket {
    private final BlockPos pos;
    private final CompoundTag tag;

    public S2CSyncLockPacket(BlockPos pos, CompoundTag tag) {
        this.pos = pos;
        this.tag = tag;
    }

    public S2CSyncLockPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.tag = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeNbt(this.tag);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (tag == null || tag.isEmpty()) {
                ClientLockCache.remove(pos);
            } else {
                LockData data = new LockData();
                data.readFromNBT(tag);
                // 修改点：传入 data 而不是 data.isLocked()
                ClientLockCache.updateStatus(pos, data);
            }
        });
        context.setPacketHandled(true);
    }
}
package com.dfg233.lock.network;

import com.dfg233.lock.capability.LockCapabilityProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncLockPacket {
    private final BlockPos pos;
    private final CompoundTag tag;

    // 构造函数
    public S2CSyncLockPacket(BlockPos pos, CompoundTag tag) {
        this.pos = pos;
        this.tag = tag;
    }

    // 从二进制缓冲区读取（解码）
    public S2CSyncLockPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.tag = buf.readNbt();
    }

    // 写入二进制缓冲区（编码）
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeNbt(this.tag);
    }

    // 客户端收到包后的处理逻辑
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Level level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null && level.isLoaded(this.pos)) { // 确保世界存在且区块已加载
                BlockEntity be = level.getBlockEntity(this.pos);
                if (be != null) {
                    be.getCapability(LockCapabilityProvider.LOCK_CAP).ifPresent(cap -> {
                        cap.getLockData().readFromNBT(tag);
                        // 关键：通知客户端方块数据已改变，触发可能的重新渲染
                        level.sendBlockUpdated(this.pos, be.getBlockState(), be.getBlockState(), 3);
                    });
                }
            }
        });
        context.setPacketHandled(true);
    }
}

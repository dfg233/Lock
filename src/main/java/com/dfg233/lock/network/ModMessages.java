package com.dfg233.lock.network;

import com.dfg233.lock.Lock;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(ResourceLocation.fromNamespaceAndPath(Lock.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // 注册我们的同步包
        net.messageBuilder(S2CSyncLockPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CSyncLockPacket::new)
                .encoder(S2CSyncLockPacket::toBytes)
                .consumerMainThread(S2CSyncLockPacket::handle)
                .add();
    }

    public static <MSG> void sendToClients(MSG message) {
        // 这里需要实现发送给附近所有玩家的逻辑
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
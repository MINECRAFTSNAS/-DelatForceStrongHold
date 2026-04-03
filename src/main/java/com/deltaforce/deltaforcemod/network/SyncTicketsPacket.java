package com.deltaforce.deltaforcemod.network;

import com.deltaforce.deltaforcemod.client.ClientTicketsData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTicketsPacket {
    private final int tickets;

    public SyncTicketsPacket(int tickets) {
        this.tickets = tickets;
    }

    public static void encode(SyncTicketsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.tickets);
    }

    public static SyncTicketsPacket decode(FriendlyByteBuf buf) {
        return new SyncTicketsPacket(buf.readInt());
    }

    public static void handle(SyncTicketsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在客户端更新数据
            ClientTicketsData.setTickets(packet.tickets);
        });
        context.get().setPacketHandled(true);
    }
}
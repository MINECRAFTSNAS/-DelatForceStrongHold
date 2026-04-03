package com.deltaforce.deltaforcemod.network;

import com.deltaforce.deltaforcemod.TeamManager;
import com.deltaforce.deltaforcemod.client.ClientTeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncTeamPacket {
    private final Map<UUID, String> teamData;

    public SyncTeamPacket(Map<UUID, String> teamData) {
        this.teamData = teamData;
    }

    // 编码：将数据写入网络包
    public static void encode(SyncTeamPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.teamData.size());
        for (Map.Entry<UUID, String> entry : packet.teamData.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    // 解码：从网络包读取数据
    public static SyncTeamPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<UUID, String> teamData = new HashMap<>();
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String team = buf.readUtf();
            teamData.put(uuid, team);
        }
        return new SyncTeamPacket(teamData);
    }

    // 处理：客户端收到数据后更新缓存
    public static void handle(SyncTeamPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ClientTeamData.receiveTeamData(packet.teamData);
        });
        context.get().setPacketHandled(true);
    }
}
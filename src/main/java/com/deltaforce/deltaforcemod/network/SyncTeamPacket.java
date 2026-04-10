package com.deltaforce.deltaforcemod.network;

import com.deltaforce.deltaforcemod.DeltaForceMod;
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

    public static void encode(SyncTeamPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.teamData.size());
        for (Map.Entry<UUID, String> entry : packet.teamData.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

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

    public static void handle(SyncTeamPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DeltaForceMod.LOGGER.info("客户端收到队伍同步包，数据量: {}", packet.teamData.size());
            ClientTeamData.receiveTeamData(packet.teamData);
        });
        context.get().setPacketHandled(true);
    }
}
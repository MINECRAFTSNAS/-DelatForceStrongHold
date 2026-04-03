package com.deltaforce.deltaforcemod;

import com.deltaforce.deltaforcemod.network.SyncTeamPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    public enum Team {
        GTI,    // 攻方 - 死亡扣兵力（红色）
        HAAVK,  // 守方 - 无限兵力，死亡不扣（蓝色）
        NONE    // 未分配
    }

    private static TeamManager instance;
    private Map<UUID, Team> playerTeams = new HashMap<>();

    private TeamManager() {}

    public static TeamManager getInstance() {
        if (instance == null) {
            instance = new TeamManager();
        }
        return instance;
    }

    // 获取队伍颜色
    public static ChatFormatting getTeamColor(Team team) {
        if (team == Team.GTI) {
            return ChatFormatting.RED;
        } else if (team == Team.HAAVK) {
            return ChatFormatting.BLUE;
        }
        return ChatFormatting.GRAY;
    }

    // 获取队伍颜色（根据玩家）
    public static ChatFormatting getPlayerTeamColor(Player player) {
        Team team = getInstance().getPlayerTeam(player);
        return getTeamColor(team);
    }

    // 获取带颜色的队伍名称
    public static String getColoredTeamName(Team team) {
        if (team == Team.GTI) {
            return "§cGTI";
        } else if (team == Team.HAAVK) {
            return "§9HAAVK";
        }
        return "§7未分配";
    }

    // 获取玩家带颜色的队伍名称
    public static String getPlayerColoredTeamName(Player player) {
        Team team = getInstance().getPlayerTeam(player);
        return getColoredTeamName(team);
    }

    // 设置玩家队伍
    public void setPlayerTeam(Player player, Team team) {
        if (team == Team.NONE) {
            playerTeams.remove(player.getUUID());
            player.sendSystemMessage(
                    Component.literal("§e[三角洲系统] 你的队伍分配已被移除")
            );
        } else {
            playerTeams.put(player.getUUID(), team);
            String teamName = team == Team.GTI ? "GTI (攻方)" : "HAAVK (守方)";
            String coloredTeamName = team == Team.GTI ? "§cGTI" : "§9HAAVK";
            player.sendSystemMessage(
                    Component.literal("§e[三角洲系统] 你已被分配到队伍: " + coloredTeamName + " §e(" + teamName + ")")
            );
        }

        DeltaForceMod.LOGGER.info("玩家 " + player.getName().getString() + " 被分配到队伍: " + team);

        if (player instanceof ServerPlayer) {
            DeltaForceMod.getInstance().updateTicketBossBar((ServerPlayer) player);
            CaptureOrderManager.getInstance().updateBossBars();

            syncTeamDataToAll();
        }
    }

    // 同步所有队伍数据给所有玩家
    public void syncTeamDataToAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // 把所有队伍数据打包
        Map<UUID, String> teamData = new HashMap<>();
        for (Map.Entry<UUID, Team> entry : playerTeams.entrySet()) {
            teamData.put(entry.getKey(), entry.getValue().name());
        }

        SyncTeamPacket packet = new SyncTeamPacket(teamData);

        // 发送给每个在线玩家
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DeltaForceMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    // 同步给单个玩家（新登录时）
    public void syncTeamDataToPlayer(ServerPlayer targetPlayer) {
        Map<UUID, String> teamData = new HashMap<>();
        for (Map.Entry<UUID, Team> entry : playerTeams.entrySet()) {
            teamData.put(entry.getKey(), entry.getValue().name());
        }
        SyncTeamPacket packet = new SyncTeamPacket(teamData);
        DeltaForceMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer), packet);
    }

    // 获取玩家队伍
    public Team getPlayerTeam(Player player) {
        return playerTeams.getOrDefault(player.getUUID(), Team.NONE);
    }

    // 检查玩家是否为GTI（攻方）
    public boolean isGTI(Player player) {
        return getPlayerTeam(player) == Team.GTI;
    }

    // 检查玩家是否为HAAVK（守方）
    public boolean isHAAVK(Player player) {
        return getPlayerTeam(player) == Team.HAAVK;
    }

    // 移除玩家队伍
    public void removePlayerTeam(Player player) {
        playerTeams.remove(player.getUUID());
    }

    // 清空所有队伍
    public void clearAllTeams() {
        playerTeams.clear();
    }

    // 获取所有队伍数据（用于保存）
    public Map<UUID, Team> getAllTeams() {
        return new HashMap<>(playerTeams);
    }

    // 设置队伍数据（用于加载）
    public void setTeamData(UUID uuid, Team team) {
        playerTeams.put(uuid, team);
    }
}
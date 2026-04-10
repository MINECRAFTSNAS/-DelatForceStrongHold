package com.deltaforce.deltaforcemod.client;

import com.deltaforce.deltaforcemod.DeltaForceMod;
import com.deltaforce.deltaforcemod.TeamManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientTeamData {
    private static final Map<UUID, TeamManager.Team> teamCache = new HashMap<>();

    public static void receiveTeamData(Map<UUID, String> teamData) {
        teamCache.clear();
        DeltaForceMod.LOGGER.info("ClientTeamData 接收数据: {}", teamData.size());
        for (Map.Entry<UUID, String> entry : teamData.entrySet()) {
            try {
                TeamManager.Team team = TeamManager.Team.valueOf(entry.getValue());
                teamCache.put(entry.getKey(), team);
                DeltaForceMod.LOGGER.info("  玩家 {} 队伍: {}", entry.getKey(), team);
            } catch (Exception e) {
                DeltaForceMod.LOGGER.error("解析队伍数据失败: {}", entry.getValue());
            }
        }
    }

    public static TeamManager.Team getTeam(UUID uuid) {
        TeamManager.Team team = teamCache.getOrDefault(uuid, TeamManager.Team.NONE);
        DeltaForceMod.LOGGER.debug("获取玩家 {} 队伍: {}", uuid, team);
        return team;
    }

    public static void clear() {
        teamCache.clear();
    }
}
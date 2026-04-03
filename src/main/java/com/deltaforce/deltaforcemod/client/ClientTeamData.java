package com.deltaforce.deltaforcemod.client;

import com.deltaforce.deltaforcemod.TeamManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientTeamData {
    // 存储每个玩家的队伍信息 (UUID -> 队伍)
    private static final Map<UUID, TeamManager.Team> teamCache = new HashMap<>();

    // 接收服务端发来的队伍数据
    public static void receiveTeamData(Map<UUID, String> teamData) {
        teamCache.clear();
        for (Map.Entry<UUID, String> entry : teamData.entrySet()) {
            try {
                teamCache.put(entry.getKey(), TeamManager.Team.valueOf(entry.getValue()));
            } catch (Exception ignored) {}
        }
    }

    // 获取某个玩家的队伍
    public static TeamManager.Team getTeam(UUID uuid) {
        return teamCache.getOrDefault(uuid, TeamManager.Team.NONE);
    }

    // 清空缓存（玩家退出时）
    public static void clear() {
        teamCache.clear();
    }
}
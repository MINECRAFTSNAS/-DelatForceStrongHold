package com.deltaforce.deltaforcemod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PermissionManager {
    private static PermissionManager instance;

    // 存储有权限使用deltaforcesystem命令的玩家（非OP玩家）
    private Set<UUID> allowedPlayers = new HashSet<>();

    private PermissionManager() {}

    public static PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    // 检查玩家是否有权限
    public boolean hasPermission(ServerPlayer player) {
        // OP玩家始终有权限
        if (player.hasPermissions(2)) {
            return true;
        }
        // 检查是否在允许列表中
        return allowedPlayers.contains(player.getUUID());
    }

    // 添加玩家权限
    public void addPermission(ServerPlayer player) {
        allowedPlayers.add(player.getUUID());
        DeltaForceMod.LOGGER.info("已授予玩家 {} deltaforcesystem 命令权限", player.getName().getString());
    }

    // 移除玩家权限
    public void removePermission(ServerPlayer player) {
        allowedPlayers.remove(player.getUUID());
        DeltaForceMod.LOGGER.info("已移除玩家 {} deltaforcesystem 命令权限", player.getName().getString());
    }

    // 获取所有有权限的玩家（非OP）
    public Set<UUID> getAllowedPlayers() {
        return allowedPlayers;
    }

    // 获取所有有权限的玩家名称列表
    public String getPermissionList(MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : allowedPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(player.getName().getString());
            }
        }
        return sb.toString();
    }

    // 检查玩家是否在权限列表中
    public boolean isInPermissionList(UUID uuid) {
        return allowedPlayers.contains(uuid);
    }
}
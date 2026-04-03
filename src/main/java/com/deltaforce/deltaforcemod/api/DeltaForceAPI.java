package com.deltaforce.deltaforcemod.api;

import com.deltaforce.deltaforcemod.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 三角洲行动模组 API
 * 其他模组可以通过这个类调用本模组的功能
 */
public class DeltaForceAPI {

    // ========== 兵力系统 API ==========

    public static int getRemainingTickets() {
        return DeltaForceMod.getInstance().getAttackerTickets();
    }

    public static int addTickets(int amount) {
        return DeltaForceMod.getInstance().addTickets(amount);
    }

    public static int reduceTickets(int amount) {
        return DeltaForceMod.getInstance().reduceTicketsByAmount(amount);
    }

    public static void setTickets(int amount) {
        DeltaForceMod.getInstance().setTickets(amount);
    }

    public static boolean isGameStarted() {
        return DeltaForceMod.getInstance().isGameStarted();
    }

    public static boolean isGameActive() {
        return DeltaForceMod.getInstance().isGameActive();
    }

    public static void startGame() {
        DeltaForceMod.getInstance().startGame();
    }

    public static void stopGame() {
        DeltaForceMod.getInstance().stopGame();
    }

    public static void resetGame() {
        DeltaForceMod.getInstance().resetGame();
    }

    // ========== 队伍系统 API ==========

    public static String getPlayerTeam(Player player) {
        TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);
        return team.name();
    }

    public static boolean isGTI(Player player) {
        return TeamManager.getInstance().isGTI(player);
    }

    public static boolean isHAAVK(Player player) {
        return TeamManager.getInstance().isHAAVK(player);
    }

    public static boolean setPlayerTeam(Player player, String team) {
        if (team.equalsIgnoreCase("GTI")) {
            TeamManager.getInstance().setPlayerTeam(player, TeamManager.Team.GTI);
            return true;
        } else if (team.equalsIgnoreCase("HAAVK")) {
            TeamManager.getInstance().setPlayerTeam(player, TeamManager.Team.HAAVK);
            return true;
        } else if (team.equalsIgnoreCase("NONE")) {
            TeamManager.getInstance().setPlayerTeam(player, TeamManager.Team.NONE);
            return true;
        }
        return false;
    }

    public static void removePlayerTeam(Player player) {
        TeamManager.getInstance().removePlayerTeam(player);
    }

    /**
     * 获取所有玩家的队伍信息
     * @return Map<玩家UUID, 队伍名称>
     */
    public static Map<UUID, String> getAllTeams() {
        Map<UUID, TeamManager.Team> teams = TeamManager.getInstance().getAllTeams();
        Map<UUID, String> result = new java.util.HashMap<>();
        for (Map.Entry<UUID, TeamManager.Team> entry : teams.entrySet()) {
            result.put(entry.getKey(), entry.getValue().name());
        }
        return result;
    }

    // ========== 权限系统 API ==========

    /**
     * 检查玩家是否有使用 deltaforcesystem 命令的权限
     * @param player 玩家
     * @return true=有权限
     */
    public static boolean hasCommandPermission(ServerPlayer player) {
        return PermissionManager.getInstance().hasPermission(player);
    }

    /**
     * 授予玩家命令权限
     * @param player 玩家
     * @return 是否成功
     */
    public static boolean grantCommandPermission(ServerPlayer player) {
        PermissionManager.getInstance().addPermission(player);
        return true;
    }

    /**
     * 撤销玩家命令权限
     * @param player 玩家
     * @return 是否成功
     */
    public static boolean revokeCommandPermission(ServerPlayer player) {
        PermissionManager.getInstance().removePermission(player);
        return true;
    }

    /**
     * 获取所有有命令权限的玩家UUID列表
     * @return UUID列表
     */
    public static List<UUID> getAllPermittedPlayers() {
        return new java.util.ArrayList<>(PermissionManager.getInstance().getAllowedPlayers());
    }

    // ========== 据点系统 API ==========

    public static List<String> getAllStrongholdNames() {
        return new java.util.ArrayList<>(StrongholdManager.getInstance().getAllStrongholds().keySet());
    }

    public static boolean isStrongholdExists(String name) {
        return StrongholdManager.getInstance().getStronghold(name) != null;
    }

    public static int[] getStrongholdBounds(String name) {
        StrongholdManager.Stronghold sh = StrongholdManager.getInstance().getStronghold(name);
        if (sh == null) return null;
        return new int[]{sh.minX, sh.minY, sh.minZ, sh.maxX, sh.maxY, sh.maxZ};
    }

    public static boolean isInStronghold(String strongholdName, BlockPos pos) {
        StrongholdManager.Stronghold sh = StrongholdManager.getInstance().getStronghold(strongholdName);
        if (sh == null) return false;
        return sh.contains(pos);
    }

    // ========== 占点系统 API ==========

    public static List<String> getCaptureOrder() {
        return CaptureOrderManager.getInstance().getCaptureOrder();
    }

    /**
     * 获取占点顺序（数字到据点列表的映射）
     * @return Map<顺序数字, 据点列表>
     */
    public static Map<Integer, List<String>> getCaptureOrderMap() {
        return CaptureOrderManager.getInstance().getCaptureOrderMap();
    }

    public static int getCurrentArea() {
        return CaptureOrderManager.getInstance().getCurrentOrder();
    }

    public static int getStrongholdProgress(String strongholdName) {
        return CaptureOrderManager.getInstance().getStrongholdProgress(strongholdName);
    }

    public static int getCurrentAreaProgress() {
        List<String> strongholds = CaptureOrderManager.getInstance().getCurrentOrderStrongholds();
        if (strongholds.isEmpty()) return 0;
        int total = 0;
        for (String name : strongholds) {
            total += CaptureOrderManager.getInstance().getStrongholdProgress(name);
        }
        return total / strongholds.size();
    }

    public static boolean isStrongholdCaptured(String strongholdName) {
        return CaptureOrderManager.getInstance().getStrongholdProgress(strongholdName) >= 100;
    }

    public static boolean isCurrentAreaComplete() {
        return CaptureOrderManager.getInstance().isCurrentOrderComplete();
    }

    /**
     * 设置占点顺序（单个）
     * @param order 顺序数字
     * @param strongholdNames 据点列表
     */
    public static void setCaptureOrder(int order, List<String> strongholdNames) {
        CaptureOrderManager.getInstance().setCaptureOrder(order, strongholdNames);
    }

    /**
     * 清除所有占点顺序
     */
    public static void clearCaptureOrder() {
        CaptureOrderManager.getInstance().clearCaptureOrder();
    }

    /**
     * 重置占点进度（保留顺序）
     */
    public static void resetCaptureProgress() {
        CaptureOrderManager.getInstance().reset();
    }

    // ========== 游戏模式 API ==========

    public static String getGameMode() {
        return DeltaForceMod.getInstance().getGameMode();
    }

    public static void setGameMode(String mode) {
        DeltaForceMod.getInstance().setGameMode(mode);
    }

    public static boolean isTestMode() {
        return DeltaForceMod.getInstance().getGameMode().equals("test");
    }

    public static boolean isNormalMode() {
        return DeltaForceMod.getInstance().getGameMode().equals("normal");
    }
}
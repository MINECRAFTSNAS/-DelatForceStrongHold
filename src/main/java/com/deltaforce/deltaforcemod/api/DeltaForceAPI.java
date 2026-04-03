package com.deltaforce.deltaforcemod.api;

import com.deltaforce.deltaforcemod.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.List;

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
        }
        return false;
    }

    public static void removePlayerTeam(Player player) {
        TeamManager.getInstance().removePlayerTeam(player);
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
}
package com.deltaforce.deltaforcemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = DeltaForceMod.MOD_ID)
public class CaptureOrderManager {
    // GTI 占领速度：15秒 = 300 tick，每 tick 增加 100/300 ≈ 0.333%
    private static final double GTI_CAPTURE_SPEED = 0.25;
    // HAAVK 反占领速度：13秒 = 260 tick，每 tick 增加 100/260 ≈ 0.385%
    private static final double HAAVK_CAPTURE_SPEED = 0.278;

    private static CaptureOrderManager instance;

    private List<String> captureOrder = new ArrayList<>();
    private Map<Integer, List<String>> captureOrderMap = new HashMap<>();
    private Map<String, Double> strongholdProgress = new HashMap<>();
    private Map<Integer, Integer> orderCompletedCount = new HashMap<>();
    private int currentOrder = 1;
    private int maxOrderNumber = 0;
    private int currentOrderIndex = 0;
    private CustomBossEvent gtiBossBar = null;
    private CustomBossEvent haavkBossBar = null;
    private int captureTimer = 0;

    private CaptureOrderManager() {}

    public static CaptureOrderManager getInstance() {
        if (instance == null) {
            instance = new CaptureOrderManager();
        }
        return instance;
    }

    public void setCaptureOrder(int order, List<String> strongholdNames) {
        captureOrderMap.put(order, new ArrayList<>(strongholdNames));
        if (order > maxOrderNumber) {
            maxOrderNumber = order;
        }
        orderCompletedCount.put(order, 0);
        for (String name : strongholdNames) {
            strongholdProgress.put(name, 0.0);
        }
        updateCaptureOrderList();
        DeltaForceMod.LOGGER.info("占点顺序已设置: 第{}位 -> {}", order, strongholdNames);
    }

    public void setCaptureOrder(int order, String strongholdName) {
        List<String> list = new ArrayList<>();
        list.add(strongholdName);
        setCaptureOrder(order, list);
    }

    public void clearCaptureOrder() {
        captureOrderMap.clear();
        captureOrder.clear();
        strongholdProgress.clear();
        orderCompletedCount.clear();
        currentOrder = 1;
        currentOrderIndex = 0;
        maxOrderNumber = 0;
        removeBossBars();
        DeltaForceMod.LOGGER.info("已清除所有占点顺序");
    }

    private void updateCaptureOrderList() {
        captureOrder.clear();
        for (int i = 1; i <= maxOrderNumber; i++) {
            List<String> strongholds = captureOrderMap.get(i);
            if (strongholds != null) {
                captureOrder.addAll(strongholds);
            }
        }
    }

    private void removeBossBars() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            if (gtiBossBar != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    gtiBossBar.removePlayer(player);
                }
                gtiBossBar = null;
            }
            if (haavkBossBar != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    haavkBossBar.removePlayer(player);
                }
                haavkBossBar = null;
            }
        }
    }

    public List<String> getCaptureOrder() {
        return captureOrder;
    }

    public Map<Integer, List<String>> getCaptureOrderMap() {
        return captureOrderMap;
    }

    public int getMaxOrderNumber() {
        return maxOrderNumber;
    }

    public int getCurrentOrder() {
        return currentOrder;
    }

    public List<String> getCurrentOrderStrongholds() {
        return captureOrderMap.getOrDefault(currentOrder, new ArrayList<>());
    }

    public List<String> getCurrentIncompleteStrongholds() {
        List<String> incomplete = new ArrayList<>();
        List<String> orderStrongholds = getCurrentOrderStrongholds();
        for (String name : orderStrongholds) {
            if (strongholdProgress.getOrDefault(name, 0.0) < 100) {
                incomplete.add(name);
            }
        }
        return incomplete;
    }

    public boolean isStrongholdCompleted(String name) {
        return strongholdProgress.getOrDefault(name, 0.0) >= 100;
    }

    public boolean isCurrentOrderComplete() {
        List<String> orderStrongholds = getCurrentOrderStrongholds();
        for (String name : orderStrongholds) {
            if (strongholdProgress.getOrDefault(name, 0.0) < 100) {
                return false;
            }
        }
        return true;
    }

    public int getStrongholdProgress(String name) {
        return (int) Math.floor(strongholdProgress.getOrDefault(name, 0.0));
    }

    public void reset() {
        currentOrder = 1;
        currentOrderIndex = 0;
        strongholdProgress.clear();
        orderCompletedCount.clear();
        for (Map.Entry<Integer, List<String>> entry : captureOrderMap.entrySet()) {
            orderCompletedCount.put(entry.getKey(), 0);
            for (String name : entry.getValue()) {
                strongholdProgress.put(name, 0.0);
            }
        }
        updateBossBars();
        DeltaForceMod.LOGGER.info("占点进度已重置");
    }

    public void clearBossBar() {
        removeBossBars();
    }

    public void updateBossBars() {
        if (captureOrderMap.isEmpty()) {
            if (gtiBossBar != null) {
                gtiBossBar.setVisible(false);
            }
            if (haavkBossBar != null) {
                haavkBossBar.setVisible(false);
            }
            return;
        }

        List<String> orderStrongholds = getCurrentOrderStrongholds();
        int totalProgress = 0;
        int totalStrongholds = orderStrongholds.size();

        if (totalStrongholds > 0) {
            for (String name : orderStrongholds) {
                int progress = (int) Math.floor(strongholdProgress.getOrDefault(name, 0.0));
                totalProgress += progress;
            }
            totalProgress = totalProgress / totalStrongholds;
        }

        if (gtiBossBar != null) gtiBossBar.setVisible(true);
        if (haavkBossBar != null) haavkBossBar.setVisible(true);

        String areaName = currentOrder + "区";

        String gtiTitle;
        if (currentOrder > maxOrderNumber) {
            gtiTitle = "§a[胜利] 所有据点已被占领！";
        } else if (totalProgress >= 100) {
            gtiTitle = "§a[" + areaName + "] 全部占领，继续前进！";
        } else {
            gtiTitle = String.format("§e[%s] GTI占领进度: §f%d%%", areaName, totalProgress);
        }

        int remainingProgress = 100 - totalProgress;
        if (remainingProgress < 0) remainingProgress = 0;

        String haavkTitle;
        if (currentOrder > maxOrderNumber) {
            haavkTitle = "§c[失败] 所有据点已被占领！";
        } else if (totalProgress >= 100) {
            haavkTitle = "§c[" + areaName + "] 我们失守了，撤退至下一据点";
        } else {
            haavkTitle = String.format("§c[%s] HAAVK剩余防线: §f%d%%", areaName, remainingProgress);
        }

        if (gtiBossBar == null) {
            gtiBossBar = new CustomBossEvent(
                    new net.minecraft.resources.ResourceLocation(DeltaForceMod.MOD_ID, "gti_capture"),
                    Component.literal(gtiTitle)
            );
            gtiBossBar.setColor(BossEvent.BossBarColor.RED);
            gtiBossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            gtiBossBar.setCreateWorldFog(false);
            gtiBossBar.setDarkenScreen(false);
        } else {
            gtiBossBar.setName(Component.literal(gtiTitle));
        }
        gtiBossBar.setProgress(totalProgress / 100.0f);

        if (haavkBossBar == null) {
            haavkBossBar = new CustomBossEvent(
                    new net.minecraft.resources.ResourceLocation(DeltaForceMod.MOD_ID, "haavk_capture"),
                    Component.literal(haavkTitle)
            );
            haavkBossBar.setColor(BossEvent.BossBarColor.BLUE);
            haavkBossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            haavkBossBar.setCreateWorldFog(false);
            haavkBossBar.setDarkenScreen(false);
        } else {
            haavkBossBar.setName(Component.literal(haavkTitle));
        }
        haavkBossBar.setProgress(remainingProgress / 100.0f);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);

                if (team == TeamManager.Team.GTI) {
                    if (!gtiBossBar.getPlayers().contains(player)) {
                        if (haavkBossBar != null && haavkBossBar.getPlayers().contains(player)) {
                            haavkBossBar.removePlayer(player);
                        }
                        gtiBossBar.addPlayer(player);
                    }
                } else if (team == TeamManager.Team.HAAVK) {
                    if (!haavkBossBar.getPlayers().contains(player)) {
                        if (gtiBossBar != null && gtiBossBar.getPlayers().contains(player)) {
                            gtiBossBar.removePlayer(player);
                        }
                        haavkBossBar.addPlayer(player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            getInstance().updateCapture();
        }
    }

    private void updateCapture() {
        if (!DeltaForceMod.getInstance().isGameStarted()) {
            if (gtiBossBar != null) gtiBossBar.setVisible(false);
            if (haavkBossBar != null) haavkBossBar.setVisible(false);
            return;
        }

        if (currentOrder > maxOrderNumber) {
            return;
        }

        if (gtiBossBar != null) gtiBossBar.setVisible(true);
        if (haavkBossBar != null) haavkBossBar.setVisible(true);

        List<String> currentStrongholds = getCurrentOrderStrongholds();
        if (currentStrongholds.isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Map<String, Integer> gtiCounts = new HashMap<>();
        Map<String, Integer> haavkCounts = new HashMap<>();
        Map<String, List<ServerPlayer>> playersInStronghold = new HashMap<>();

        for (String name : currentStrongholds) {
            gtiCounts.put(name, 0);
            haavkCounts.put(name, 0);
            playersInStronghold.put(name, new ArrayList<>());
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isDeadOrDying() || player.getHealth() <= 0.0f) {
                continue;
            }

            BlockPos feetPos = player.blockPosition();
            BlockPos headPos = new BlockPos(feetPos.getX(), feetPos.getY() + 1, feetPos.getZ());

            for (String strongholdName : currentStrongholds) {
                StrongholdManager.Stronghold stronghold = StrongholdManager.getInstance().getStronghold(strongholdName);
                if (stronghold != null && (stronghold.contains(feetPos) || stronghold.contains(headPos))) {
                    playersInStronghold.get(strongholdName).add(player);
                    if (TeamManager.getInstance().isGTI(player)) {
                        gtiCounts.put(strongholdName, gtiCounts.get(strongholdName) + 1);
                    } else if (TeamManager.getInstance().isHAAVK(player)) {
                        haavkCounts.put(strongholdName, haavkCounts.get(strongholdName) + 1);
                    }
                }
            }
        }

        boolean anyProgressChanged = false;

        for (String strongholdName : currentStrongholds) {
            double currentProgress = strongholdProgress.getOrDefault(strongholdName, 0.0);
            int gtiCount = gtiCounts.get(strongholdName);
            int haavkCount = haavkCounts.get(strongholdName);

            double progressChange = 0;
            boolean isCompleted = currentProgress >= 100;

            if (!isCompleted) {
                if (gtiCount > haavkCount) {
                    // GTI 占领（较慢，15秒）
                    progressChange = (gtiCount - haavkCount) * GTI_CAPTURE_SPEED;
                } else if (haavkCount > gtiCount) {
                    // HAAVK 反占领（较快，13秒）
                    progressChange = (haavkCount - gtiCount) * -HAAVK_CAPTURE_SPEED;
                }
            } else {
                if (haavkCount > gtiCount) {
                    // 已占领的据点被 HAAVK 反占领（较快，13秒）
                    progressChange = (haavkCount - gtiCount) * -HAAVK_CAPTURE_SPEED;
                }
            }

            if (progressChange != 0) {
                double newProgress = currentProgress + progressChange;
                if (newProgress > 100) newProgress = 100;
                if (newProgress < 0) newProgress = 0;
                strongholdProgress.put(strongholdName, newProgress);
                anyProgressChanged = true;
            }
        }

        if (isCurrentOrderComplete()) {
            completeCurrentOrder();
            updateBossBars();
            return;
        }

        if (anyProgressChanged) {
            updateBossBars();
        }

        for (String strongholdName : currentStrongholds) {
            int progress = (int) Math.floor(strongholdProgress.getOrDefault(strongholdName, 0.0));
            List<ServerPlayer> players = playersInStronghold.get(strongholdName);

            if (players != null && !players.isEmpty()) {
                for (ServerPlayer player : players) {
                    TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);
                    String message;

                    if (progress >= 100) {
                        if (team == TeamManager.Team.GTI) {
                            message = String.format("§a[%s] 已占领！", strongholdName);
                        } else {
                            message = String.format("§c[%s] 已失守！", strongholdName);
                        }
                    } else {
                        int gtiCount = gtiCounts.get(strongholdName);
                        int haavkCount = haavkCounts.get(strongholdName);
                        int remaining = 100 - progress;

                        if (team == TeamManager.Team.GTI) {
                            if (gtiCount > haavkCount) {
                                message = String.format("§e[%s] 占领进度: §f%d%% §a(+%d)", strongholdName, progress, gtiCount - haavkCount);
                            } else if (haavkCount > gtiCount) {
                                message = String.format("§e[%s] 占领进度: §f%d%% §c(-%d)", strongholdName, progress, haavkCount - gtiCount);
                            } else if (gtiCount > 0 && haavkCount > 0) {
                                message = String.format("§c[%s] 争夺中! 进度: §f%d%%", strongholdName, progress);
                            } else {
                                message = String.format("§7[%s] 无人占领 进度: §f%d%%", strongholdName, progress);
                            }
                        } else {
                            if (haavkCount > gtiCount) {
                                message = String.format("§e[%s] 防线剩余: §f%d%% §a(+%d)", strongholdName, remaining, haavkCount - gtiCount);
                            } else if (gtiCount > haavkCount) {
                                message = String.format("§e[%s] 防线剩余: §f%d%% §c(-%d)", strongholdName, remaining, gtiCount - haavkCount);
                            } else if (gtiCount > 0 && haavkCount > 0) {
                                message = String.format("§c[%s] 争夺中! 防线: §f%d%%", strongholdName, remaining);
                            } else {
                                message = String.format("§7[%s] 无人防守 防线: §f%d%%", strongholdName, remaining);
                            }
                        }
                    }
                    player.displayClientMessage(Component.literal(message), true);
                }
            }
        }

        captureTimer++;
        if (captureTimer > 1000) captureTimer = 0;
    }

    private void completeCurrentOrder() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        String areaName = currentOrder + "区";

        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);
                if (team == TeamManager.Team.GTI) {
                    showTitleToPlayer(player,
                            "§a" + areaName + " 已占领",
                            "§e[" + areaName + "] 全部占领，继续前进！",
                            20, 60, 20);
                } else if (team == TeamManager.Team.HAAVK) {
                    showTitleToPlayer(player,
                            "§c" + areaName + " 已失守",
                            "§e[" + areaName + "] 我们失守了，撤退至下一据点",
                            20, 60, 20);
                }
            }
        }

        currentOrder++;

        if (currentOrder > maxOrderNumber) {
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);

                    if (team == TeamManager.Team.GTI) {
                        showTitleToPlayer(player,
                                "§a我们夺下了所有据点，大获全胜！",
                                "§6Victory!",
                                20, 80, 20);
                    } else if (team == TeamManager.Team.HAAVK) {
                        showTitleToPlayer(player,
                                "§c所有据点被占领了，撤退！",
                                "§eDefeat",
                                20, 80, 20);
                    }
                }
            }

            DeltaForceMod.getInstance().stopGame();
            clearBossBar();

            if (server != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        server.execute(() -> {
                            DeltaForceMod.getInstance().resetGame();
                            DeltaForceMod.LOGGER.info("新的一局开始，保留占点顺序: {}", getOrderListFormatted());
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } else {
            String nextAreaName = currentOrder + "区";
            List<String> nextStrongholds = getCurrentOrderStrongholds();
            if (server != null && !nextStrongholds.isEmpty()) {
                String targetList = String.join(", ", nextStrongholds);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);
                    if (team == TeamManager.Team.GTI) {
                        showTitleToPlayer(player,
                                "§e下一区域目标: §6" + nextAreaName,
                                "§7需要占领: " + targetList,
                                10, 40, 10);
                    } else {
                        showTitleToPlayer(player,
                                "§c下一区域防守: §6" + nextAreaName,
                                "§7需要防守: " + targetList,
                                10, 40, 10);
                    }
                }
            }
            updateBossBars();
        }
    }

    private void showTitleToPlayer(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }

    public String getProgressInfo() {
        StringBuilder info = new StringBuilder();
        for (int i = 1; i <= maxOrderNumber; i++) {
            List<String> strongholds = captureOrderMap.get(i);
            if (strongholds == null) continue;

            if (i < currentOrder) {
                info.append("§a✓ 顺序").append(i).append(": ").append(String.join(", ", strongholds)).append(" §7(已完成)\n");
            } else if (i == currentOrder) {
                info.append("§e→ 顺序").append(i).append(": ");
                for (String name : strongholds) {
                    int progress = (int) Math.floor(strongholdProgress.getOrDefault(name, 0.0));
                    if (progress >= 100) {
                        info.append("§a").append(name).append("(100%) ");
                    } else {
                        info.append("§f").append(name).append("§e(").append(progress).append("%) ");
                    }
                }
                info.append("\n");
            } else {
                info.append("§7○ 顺序").append(i).append(": ").append(String.join(", ", strongholds)).append("\n");
            }
        }
        return info.toString();
    }

    public String getOrderListFormatted() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= maxOrderNumber; i++) {
            List<String> strongholds = captureOrderMap.get(i);
            if (strongholds != null && !strongholds.isEmpty()) {
                if (sb.length() > 0) sb.append(" §7-> §e");
                sb.append(i).append(".[").append(String.join(",", strongholds)).append("]");
            }
        }
        return sb.toString();
    }
}
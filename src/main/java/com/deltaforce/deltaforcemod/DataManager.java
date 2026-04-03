package com.deltaforce.deltaforcemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = DeltaForceMod.MOD_ID)
public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SAVE_DIR = Paths.get("config", "deltaforce");
    private static final Path DATA_FILE = SAVE_DIR.resolve("data.json");

    private static DataManager instance;
    private SaveData saveData = new SaveData();

    private DataManager() {
        loadData();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    // 保存所有数据
    public void saveData() {
        try {
            Files.createDirectories(SAVE_DIR);

            // 保存兵力数据
            saveData.attackerTickets = DeltaForceMod.getInstance().getAttackerTickets();
            saveData.gameActive = DeltaForceMod.getInstance().isGameActive();

            // 保存游戏模式
            saveData.gameMode = DeltaForceMod.getInstance().getGameMode();

            // 保存游戏开始状态
            saveData.gameStarted = DeltaForceMod.getInstance().isGameStarted();

            // 保存队伍数据
            saveData.playerTeams.clear();
            if (TeamManager.getInstance() != null) {
                Map<UUID, TeamManager.Team> allTeams = TeamManager.getInstance().getAllTeams();
                for (Map.Entry<UUID, TeamManager.Team> entry : allTeams.entrySet()) {
                    saveData.playerTeams.put(entry.getKey().toString(), entry.getValue().name());
                }
            }

            // 保存占点顺序数据
            saveData.captureOrderMap.clear();
            saveData.captureOrderMap.putAll(CaptureOrderManager.getInstance().getCaptureOrderMap());

            // 保存据点进度数据
            saveData.strongholdProgress.clear();
            Map<String, Integer> progress = new HashMap<>();
            for (String name : StrongholdManager.getInstance().getAllStrongholds().keySet()) {
                progress.put(name, CaptureOrderManager.getInstance().getStrongholdProgress(name));
            }
            saveData.strongholdProgress.putAll(progress);

            // 保存当前顺序
            saveData.currentOrder = CaptureOrderManager.getInstance().getCurrentOrder();

            // 保存据点数据
            saveData.strongholds.clear();
            for (var entry : StrongholdManager.getInstance().getAllStrongholds().entrySet()) {
                StrongholdManager.Stronghold sh = entry.getValue();
                StrongholdData shData = new StrongholdData();
                shData.name = sh.name;
                shData.minX = sh.minX;
                shData.minY = sh.minY;
                shData.minZ = sh.minZ;
                shData.maxX = sh.maxX;
                shData.maxY = sh.maxY;
                shData.maxZ = sh.maxZ;
                shData.blockId = sh.blockId;
                saveData.strongholds.put(entry.getKey(), shData);
            }

            String json = GSON.toJson(saveData);
            Files.write(DATA_FILE, json.getBytes());

            DeltaForceMod.LOGGER.info("数据已保存到: " + DATA_FILE);
        } catch (IOException e) {
            DeltaForceMod.LOGGER.error("保存数据失败: ", e);
        }
    }

    // 加载数据
    public void loadData() {
        try {
            if (Files.exists(DATA_FILE)) {
                String json = new String(Files.readAllBytes(DATA_FILE));
                saveData = GSON.fromJson(json, SaveData.class);

                // 恢复兵力数据
                DeltaForceMod.getInstance().setTickets(saveData.attackerTickets);

                // 恢复游戏模式
                if (saveData.gameMode != null && !saveData.gameMode.isEmpty()) {
                    DeltaForceMod.getInstance().setGameMode(saveData.gameMode);
                }

                // 恢复游戏开始状态（注意：不自动开始游戏，只恢复状态）
                // 游戏开始状态需要在玩家登录后根据情况恢复

                // 恢复队伍数据
                if (TeamManager.getInstance() != null) {
                    TeamManager.getInstance().clearAllTeams();
                    for (Map.Entry<String, String> entry : saveData.playerTeams.entrySet()) {
                        UUID uuid = UUID.fromString(entry.getKey());
                        TeamManager.Team team = TeamManager.Team.valueOf(entry.getValue());
                        TeamManager.getInstance().setTeamData(uuid, team);
                    }
                }

                // 恢复占点顺序
                if (saveData.captureOrderMap != null && !saveData.captureOrderMap.isEmpty()) {
                    for (Map.Entry<Integer, java.util.List<String>> entry : saveData.captureOrderMap.entrySet()) {
                        CaptureOrderManager.getInstance().setCaptureOrder(entry.getKey(), entry.getValue());
                    }
                }

                // 恢复当前顺序
                if (saveData.currentOrder > 0) {
                    // 需要恢复当前顺序的逻辑
                }

                // 恢复据点进度
                if (saveData.strongholdProgress != null) {
                    for (Map.Entry<String, Integer> entry : saveData.strongholdProgress.entrySet()) {
                        // 进度会在 CaptureOrderManager 中恢复
                    }
                }

                // 恢复据点数据
                StrongholdManager.getInstance().clearAllStrongholds();
                for (var entry : saveData.strongholds.entrySet()) {
                    StrongholdData shData = entry.getValue();
                    net.minecraft.world.level.block.Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                            .getValue(new net.minecraft.resources.ResourceLocation(shData.blockId));
                    StrongholdManager.Stronghold stronghold =
                            new StrongholdManager.Stronghold(
                                    shData.name, shData.minX, shData.minY, shData.minZ,
                                    shData.maxX, shData.maxY, shData.maxZ, block, shData.blockId
                            );
                    StrongholdManager.getInstance().getAllStrongholds().put(entry.getKey(), stronghold);
                }

                DeltaForceMod.LOGGER.info("数据已从 " + DATA_FILE + " 加载");
            } else {
                DeltaForceMod.LOGGER.info("没有找到存档文件，使用默认数据");
            }
        } catch (Exception e) {
            DeltaForceMod.LOGGER.error("加载数据失败: ", e);
        }
    }

    // 保存类
    private static class SaveData {
        int attackerTickets = 120;
        boolean gameActive = true;
        boolean gameStarted = false;
        String gameMode = "normal";
        Map<String, String> playerTeams = new HashMap<>();
        Map<String, StrongholdData> strongholds = new HashMap<>();
        Map<Integer, java.util.List<String>> captureOrderMap = new HashMap<>();
        Map<String, Integer> strongholdProgress = new HashMap<>();
        int currentOrder = 1;
    }

    private static class StrongholdData {
        String name;
        int minX, minY, minZ;
        int maxX, maxY, maxZ;
        String blockId;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        getInstance().saveData();
        DeltaForceMod.LOGGER.info("服务器已停止，数据已保存");
    }
}
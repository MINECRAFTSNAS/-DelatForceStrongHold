package com.deltaforce.deltaforcemod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrongholdManager {
    private static final Logger LOGGER = LogManager.getLogger(StrongholdManager.class);
    private static StrongholdManager instance;

    // 存储玩家正在选择的据点
    private Map<UUID, StrongholdSelection> playerSelections = new HashMap<>();
    // 存储已创建的据点
    private Map<String, Stronghold> strongholds = new HashMap<>();

    private StrongholdManager() {}

    public static StrongholdManager getInstance() {
        if (instance == null) {
            instance = new StrongholdManager();
        }
        return instance;
    }

    // 设置第一个点
    public void setFirstPoint(Player player, BlockPos pos, String strongholdName, String blockId) {
        StrongholdSelection selection = playerSelections.computeIfAbsent(player.getUUID(),
                k -> new StrongholdSelection());
        selection.strongholdName = strongholdName;
        selection.blockId = blockId;
        selection.firstPos = pos;
        selection.hasFirstPoint = true;

        player.sendSystemMessage(
                Component.literal("§a[据点系统] 已设置第一个点: §fX=" + pos.getX() + " Y=" + pos.getY() + " Z=" + pos.getZ())
        );
        player.sendSystemMessage(
                Component.literal("§e[据点系统] 请走到另一个位置，再次输入相同命令来设置第二个点")
        );
    }

    // 显示方块建议
    private void showBlockSuggestions(Player player) {
        player.sendSystemMessage(
                Component.literal("§7可用的方块示例: minecraft:stone, minecraft:diamond_block, minecraft:oak_planks")
        );
        player.sendSystemMessage(
                Component.literal("§7输入方块ID时按 Tab 键可以查看所有方块列表")
        );
    }

    // 设置第二个点并创建据点
    public void setSecondPoint(Player player, BlockPos pos) {
        StrongholdSelection selection = playerSelections.get(player.getUUID());
        if (selection == null || !selection.hasFirstPoint) {
            player.sendSystemMessage(
                    Component.literal("§c[据点系统] 请先设置第一个点！")
            );
            return;
        }

        // 再次验证方块ID（防止在等待期间方块ID被修改）
        ResourceLocation blockResource = ResourceLocation.tryParse(selection.blockId);
        if (blockResource == null || !ForgeRegistries.BLOCKS.containsKey(blockResource)) {
            player.sendSystemMessage(
                    Component.literal("§c[据点系统] 方块ID无效: " + selection.blockId)
            );
            playerSelections.remove(player.getUUID());
            return;
        }

        selection.secondPos = pos;
        selection.hasSecondPoint = true;

        // 显示当前脚下的方块信息
        BlockState currentBlock = player.level().getBlockState(pos);
        String blockName = ForgeRegistries.BLOCKS.getKey(currentBlock.getBlock()).toString();

        player.sendSystemMessage(
                Component.literal("§a[据点系统] 已设置第二个点: §fX=" + pos.getX() + " Y=" + pos.getY() + " Z=" + pos.getZ())
        );
        player.sendSystemMessage(
                Component.literal("§7当前脚下方块: §f" + blockName)
        );

        // 创建据点
        createStronghold(player, selection);

        // 清除该玩家的选择数据
        playerSelections.remove(player.getUUID());
    }

    // 创建据点
    private void createStronghold(Player player, StrongholdSelection selection) {
        // 获取方块
        ResourceLocation blockResource = ResourceLocation.tryParse(selection.blockId);
        Block block = ForgeRegistries.BLOCKS.getValue(blockResource);

        if (block == null || block == Blocks.AIR) {
            player.sendSystemMessage(
                    Component.literal("§c[据点系统] 无效的方块: " + selection.blockId)
            );
            return;
        }

        // 计算最小和最大坐标 (X和Z轴)
        int minX = Math.min(selection.firstPos.getX(), selection.secondPos.getX());
        int maxX = Math.max(selection.firstPos.getX(), selection.secondPos.getX());
        int minZ = Math.min(selection.firstPos.getZ(), selection.secondPos.getZ());
        int maxZ = Math.max(selection.firstPos.getZ(), selection.secondPos.getZ());

        // Y轴设置为世界最低到最高，让玩家在任何高度都能触发占点
        int minY = player.level().getMinBuildHeight();
        int maxY = player.level().getMaxBuildHeight();

        // 检查是否重名
        if (strongholds.containsKey(selection.strongholdName)) {
            player.sendSystemMessage(
                    Component.literal("§c[据点系统] 据点名称已存在: " + selection.strongholdName)
            );
            return;
        }

        // 创建据点对象
        Stronghold stronghold = new Stronghold(
                selection.strongholdName,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                block,
                selection.blockId
        );

        // 保存据点
        strongholds.put(selection.strongholdName, stronghold);

        // 显示创建成功信息
        player.sendSystemMessage(
                Component.literal("§a§l[据点系统] 据点 '" + selection.strongholdName + "' 已成功创建！")
        );
        player.sendSystemMessage(
                Component.literal("§7范围: X[" + minX + "~" + maxX + "] Y[" + minY + "~" + maxY + "] Z[" + minZ + "~" + maxZ + "]")
        );
        player.sendSystemMessage(
                Component.literal("§7Y轴已覆盖世界全部高度，玩家站在任何高度都能触发占点！")
        );

        LOGGER.info("玩家 {} 创建了据点: {} (范围: X{}-{} Y{}-{} Z{}-{})",
                player.getName().getString(), selection.strongholdName,
                minX, maxX, minY, maxY, minZ, maxZ);
    }

    // 删除据点
    public boolean removeStronghold(String name) {
        Stronghold stronghold = strongholds.get(name);
        if (stronghold != null) {
            strongholds.remove(name);
            LOGGER.info("已删除据点: {}", name);
            return true;
        }
        return false;
    }

    // 删除据点并清除所有方块
    public boolean removeStrongholdAndClear(ServerLevel level, String name) {
        Stronghold stronghold = strongholds.get(name);
        if (stronghold != null) {
            // 清除据点内的所有边界方块
            for (int x = stronghold.minX; x <= stronghold.maxX; x++) {
                for (int y = stronghold.minY; y <= stronghold.maxY; y++) {
                    for (int z = stronghold.minZ; z <= stronghold.maxZ; z++) {
                        // 只清除边界方块
                        boolean isBorder =
                                x == stronghold.minX || x == stronghold.maxX ||
                                        y == stronghold.minY || y == stronghold.maxY ||
                                        z == stronghold.minZ || z == stronghold.maxZ;

                        if (isBorder) {
                            BlockPos pos = new BlockPos(x, y, z);
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
            strongholds.remove(name);
            LOGGER.info("已删除据点并清除方块: {}", name);
            return true;
        }
        return false;
    }

    // 获取据点
    public Stronghold getStronghold(String name) {
        return strongholds.get(name);
    }

    // 获取所有据点
    public Map<String, Stronghold> getAllStrongholds() {
        return strongholds;
    }

    // 清除所有据点
    public void clearAllStrongholds() {
        strongholds.clear();
        LOGGER.info("已清除所有据点");
    }

    // 清除玩家的选择
    public void clearPlayerSelection(Player player) {
        playerSelections.remove(player.getUUID());
        player.sendSystemMessage(
                Component.literal("§e[据点系统] 已取消据点选择")
        );
    }

    // 获取玩家的选择状态
    public boolean hasFirstPoint(Player player) {
        StrongholdSelection selection = playerSelections.get(player.getUUID());
        return selection != null && selection.hasFirstPoint;
    }

    // 据点选择类
    private static class StrongholdSelection {
        String strongholdName;
        String blockId;
        BlockPos firstPos;
        BlockPos secondPos;
        boolean hasFirstPoint = false;
        boolean hasSecondPoint = false;
    }

    // 据点类
    public static class Stronghold {
        public final String name;
        public final int minX, minY, minZ;
        public final int maxX, maxY, maxZ;
        private final Block block;
        public final String blockId;

        public Stronghold(String name, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Block block, String blockId) {
            this.name = name;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.block = block;
            this.blockId = blockId;
        }

        public Block getBlock() {
            return block;
        }

        // 检查位置是否在据点内
        public boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX &&
                    pos.getY() >= minY && pos.getY() <= maxY &&
                    pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        // 检查玩家是否在据点内（包括脚和头）
        public boolean containsPlayer(ServerPlayer player) {
            BlockPos feetPos = player.blockPosition();
            BlockPos headPos = new BlockPos(feetPos.getX(), feetPos.getY() + 1, feetPos.getZ());
            return contains(feetPos) || contains(headPos);
        }

        // 获取据点的中心点
        public BlockPos getCenter() {
            return new BlockPos(
                    (minX + maxX) / 2,
                    (minY + maxY) / 2,
                    (minZ + maxZ) / 2
            );
        }

        // 获取据点边界方块数量
        public int getSize() {
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int depth = maxZ - minZ + 1;
            int totalVolume = width * height * depth;
            int innerVolume = (width - 2) * (height - 2) * (depth - 2);
            if (innerVolume < 0) innerVolume = 0;
            return totalVolume - innerVolume;
        }

        @Override
        public String toString() {
            return String.format("据点 '%s': 范围 [%d,%d,%d] -> [%d,%d,%d]",
                    name, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
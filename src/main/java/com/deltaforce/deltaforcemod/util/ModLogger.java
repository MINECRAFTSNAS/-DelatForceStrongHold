package com.deltaforce.deltaforcemod.util;

import com.deltaforce.deltaforcemod.DeltaForceMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModLogger {
    private static final Logger LOGGER = LogManager.getLogger(DeltaForceMod.MOD_ID);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 是否启用详细日志（调试模式）
    private static boolean debugMode = false;

    // 日志文件路径
    private static final String LOG_FILE = "logs/deltaforce.log";

    // 基础日志方法
    public static void info(String message) {
        LOGGER.info(message);
        writeToFile("[INFO] " + message);
    }

    public static void info(String format, Object... args) {
        String message = String.format(format, args);
        LOGGER.info(message);
        writeToFile("[INFO] " + message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
        writeToFile("[WARN] " + message);
    }

    public static void warn(String format, Object... args) {
        String message = String.format(format, args);
        LOGGER.warn(message);
        writeToFile("[WARN] " + message);
    }

    public static void error(String message) {
        LOGGER.error(message);
        writeToFile("[ERROR] " + message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
        writeToFile("[ERROR] " + message);
        writeToFile("[STACKTRACE] " + getStackTrace(throwable));
    }

    public static void error(String format, Object... args) {
        String message = String.format(format, args);
        LOGGER.error(message);
        writeToFile("[ERROR] " + message);
    }

    // 调试日志（仅在调试模式下输出）
    public static void debug(String message) {
        if (debugMode) {
            LOGGER.debug(message);
            writeToFile("[DEBUG] " + message);
        }
    }

    public static void debug(String format, Object... args) {
        if (debugMode) {
            String message = String.format(format, args);
            LOGGER.debug(message);
            writeToFile("[DEBUG] " + message);
        }
    }

    // 模块化日志（方便追踪不同功能模块）
    public static void logModule(String module, String action, String detail) {
        String message = String.format("[%s] %s: %s", module, action, detail);
        LOGGER.info(message);
        writeToFile("[MODULE] " + message);
    }

    // 玩家相关日志
    public static void logPlayerAction(ServerPlayer player, String action, String detail) {
        String playerName = player != null ? player.getName().getString() : "null";
        String message = String.format("[PLAYER] %s - %s: %s", playerName, action, detail);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 据点相关日志
    public static void logStronghold(String strongholdName, String action, String detail) {
        String message = String.format("[STRONGHOLD] %s - %s: %s", strongholdName, action, detail);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 占点相关日志
    public static void logCapture(String areaName, String action, String detail) {
        String message = String.format("[CAPTURE] %s - %s: %s", areaName, action, detail);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 游戏状态日志
    public static void logGameState(String state, String detail) {
        String message = String.format("[GAME] %s: %s", state, detail);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 队伍相关日志
    public static void logTeam(String playerName, String team, String action) {
        String message = String.format("[TEAM] %s - %s: %s", playerName, action, team);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 命令相关日志
    public static void logCommand(ServerPlayer player, String command) {
        String playerName = player != null ? player.getName().getString() : "CONSOLE";
        String message = String.format("[COMMAND] %s: /deltaforcesystem %s", playerName, command);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 初始化日志（模组启动时）
    public static void logInit(String component, String status) {
        String message = String.format("[INIT] %s: %s", component, status);
        LOGGER.info(message);
        writeToFile(message);
    }

    // 设置调试模式
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        info("调试模式已" + (enabled ? "开启" : "关闭"));
    }

    // 检查是否调试模式
    public static boolean isDebugMode() {
        return debugMode;
    }

    // 写入日志文件
    private static void writeToFile(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            pw.println(timestamp + " - " + message);
        } catch (Exception e) {
            // 静默失败，不影响游戏运行
        }
    }

    // 获取异常堆栈
    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    // 发送消息给玩家（用于调试）
    public static void sendPlayerMessage(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
        info("[PLAYER_MSG] " + (player != null ? player.getName().getString() : "null") + ": " + message);
    }

    // 发送错误消息给玩家
    public static void sendPlayerError(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal("§c[错误] " + message));
        }
        error("[PLAYER_ERROR] " + (player != null ? player.getName().getString() : "null") + ": " + message);
    }

    // 发送成功消息给玩家
    public static void sendPlayerSuccess(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal("§a[成功] " + message));
        }
        info("[PLAYER_SUCCESS] " + (player != null ? player.getName().getString() : "null") + ": " + message);
    }

    // 分隔线（用于日志可读性）
    public static void logSeparator() {
        String separator = "==================================================";
        LOGGER.info(separator);
        writeToFile(separator);
    }

    // 记录方法进入/退出（调试用）
    public static void logMethodEntry(String className, String methodName) {
        if (debugMode) {
            debug("-> " + className + "." + methodName + "()");
        }
    }

    public static void logMethodExit(String className, String methodName) {
        if (debugMode) {
            debug("<- " + className + "." + methodName + "()");
        }
    }
}
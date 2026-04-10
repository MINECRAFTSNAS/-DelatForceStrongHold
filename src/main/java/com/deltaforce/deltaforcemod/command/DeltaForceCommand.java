package com.deltaforce.deltaforcemod.command;

import com.deltaforce.deltaforcemod.*;
import com.deltaforce.deltaforcemod.util.ModLogger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;

public class DeltaForceCommand {

    // 据点名称建议提供器（用于已创建的据点）
    private static final SuggestionProvider<CommandSourceStack> STRONGHOLD_NAME_SUGGESTIONS =
            (context, builder) -> {
                var strongholds = StrongholdManager.getInstance().getAllStrongholds();
                for (String name : strongholds.keySet()) {
                    builder.suggest(name, Component.literal("据点: " + name + " - 范围: " +
                            strongholds.get(name).minX + "," + strongholds.get(name).minY + "," + strongholds.get(name).minZ +
                            " -> " + strongholds.get(name).maxX + "," + strongholds.get(name).maxY + "," + strongholds.get(name).maxZ));
                }
                return builder.buildFuture();
            };

    // 玩家名称建议提供器（用于op/deop命令）
    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS =
            (context, builder) -> {
                MinecraftServer server = context.getSource().getServer();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    boolean hasPerm = PermissionManager.getInstance().hasPermission(player);
                    String status = hasPerm ? "§a(已授权)" : "§7(未授权)";
                    builder.suggest(player.getName().getString(), Component.literal(status));
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deltaforcesystem")
                // 帮助命令
                .then(Commands.literal("help")
                        .executes(DeltaForceCommand::executeHelp)
                )
                //调试命令
                .then(Commands.literal("debug")
                        .then(Commands.literal("log")
                                .then(Commands.literal("on")
                                        .executes(DeltaForceCommand::executeDebugLogOn)
                                )
                                .then(Commands.literal("off")
                                        .executes(DeltaForceCommand::executeDebugLogOff)
                                )
                        )
                )
                .then(Commands.literal("nametag")
                        .then(Commands.literal("always")
                                .executes(DeltaForceCommand::executeNametagAlways)
                        )
                        .then(Commands.literal("hide")
                                .executes(DeltaForceCommand::executeNametagHide)
                        )
                        .then(Commands.literal("never")
                                .executes(DeltaForceCommand::executeNametagNever)
                        )
                )
                // 游戏控制命令
                .then(Commands.literal("start")
                        .executes(DeltaForceCommand::executeStart)
                )
                .then(Commands.literal("stop")
                        .executes(DeltaForceCommand::executeStop)
                )
                .then(Commands.literal("rest")
                        .executes(DeltaForceCommand::executeReset)
                )
                .then(Commands.literal("fullreset")
                        .executes(DeltaForceCommand::executeFullReset)
                )
                .then(Commands.literal("status")
                        .executes(DeltaForceCommand::executeStatus)
                )
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 999))
                                .executes(DeltaForceCommand::executeSet)
                        )
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 999))
                                .executes(DeltaForceCommand::executeAdd)
                        )
                )
                .then(Commands.literal("reduce")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 999))
                                .executes(DeltaForceCommand::executeReduce)
                        )
                )
                // 队伍管理命令
                .then(Commands.literal("team")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.players())
                                        .then(Commands.argument("team", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("GTI");
                                                    builder.suggest("HAAVK");
                                                    builder.suggest("NONE", Component.literal("移除队伍分配"));
                                                    return builder.buildFuture();
                                                })
                                                .executes(DeltaForceCommand::executeTeamSet)
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(DeltaForceCommand::executeTeamRemove)
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(DeltaForceCommand::executeTeamList)
                        )
                        .then(Commands.literal("clear")
                                .executes(DeltaForceCommand::executeTeamClear)
                        )
                        .then(Commands.literal("check")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(DeltaForceCommand::executeTeamCheck)
                                )
                        )
                )
                // 据点管理命令
                .then(Commands.literal("stronghold")
                        .then(Commands.literal("set")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(DeltaForceCommand::executeStrongholdSet)
                                )
                        )
                        .then(Commands.literal("clear")
                                .executes(DeltaForceCommand::executeStrongholdClear)
                        )
                        .then(Commands.literal("list")
                                .executes(DeltaForceCommand::executeStrongholdList)
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(STRONGHOLD_NAME_SUGGESTIONS)
                                        .executes(DeltaForceCommand::executeStrongholdRemove)
                                )
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(STRONGHOLD_NAME_SUGGESTIONS)
                                        .executes(DeltaForceCommand::executeStrongholdInfo)
                                )
                        )
                        .then(Commands.literal("order")
                                .then(Commands.argument("number", IntegerArgumentType.integer(1, 100))
                                        .then(Commands.argument("strongholds", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    // 为每个据点名称提供悬浮框建议
                                                    var strongholds = StrongholdManager.getInstance().getAllStrongholds();
                                                    for (String name : strongholds.keySet()) {
                                                        StrongholdManager.Stronghold sh = strongholds.get(name);
                                                        builder.suggest(name, Component.literal("据点: " + name + " - 范围: " +
                                                                sh.minX + "," + sh.minY + "," + sh.minZ + " -> " + sh.maxX + "," + sh.maxY + "," + sh.maxZ));
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(DeltaForceCommand::executeStrongholdOrderSet)
                                        )
                                )
                        )
                        .then(Commands.literal("orderlist")
                                .executes(DeltaForceCommand::executeStrongholdOrderList)
                        )
                        .then(Commands.literal("orderclear")
                                .executes(DeltaForceCommand::executeStrongholdOrderClear)
                        )
                        .then(Commands.literal("progress")
                                .executes(DeltaForceCommand::executeStrongholdProgress)
                        )
                        .then(Commands.literal("cancel")
                                .executes(DeltaForceCommand::executeStrongholdCancel)
                        )
                )
                // 模式切换命令
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("test", Component.literal("测试模式 - 无任何限制，可以单人开始游戏"));
                                    builder.suggest("normal", Component.literal("正常模式 - 需要GTI和HAAVK都有玩家才能开始"));
                                    return builder.buildFuture();
                                })
                                .executes(DeltaForceCommand::executeMode)
                        )
                )
                // OP管理命令（只有真正OP可以使用）
                .then(Commands.literal("op")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .suggests(PLAYER_SUGGESTIONS)  // 添加悬浮框
                                .executes(DeltaForceCommand::executeGrantPermission)
                        )
                )
                .then(Commands.literal("deop")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .suggests(PLAYER_SUGGESTIONS)  // 添加悬浮框
                                .executes(DeltaForceCommand::executeRevokePermission)
                        )
                )
                .then(Commands.literal("permlist")
                        .requires(source -> source.hasPermission(2))
                        .executes(DeltaForceCommand::executePermList)
                )
        );
    }

    // 权限检查方法
    private static boolean hasCommandPermission(CommandSourceStack source) {
        // 控制台默认有权限
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return true;
        }
        ServerPlayer player = (ServerPlayer) source.getEntity();
        return PermissionManager.getInstance().hasPermission(player);
    }

    // 获取玩家脚下真正站立的方块坐标
    private static BlockPos getBlockPosUnderPlayer(ServerPlayer player) {
        BlockPos pos = player.blockPosition().below();

        int checkY = pos.getY();
        while (checkY > player.level().getMinBuildHeight() &&
                player.level().getBlockState(new BlockPos(pos.getX(), checkY, pos.getZ())).isAir()) {
            checkY--;
        }

        if (checkY > player.level().getMinBuildHeight()) {
            return new BlockPos(pos.getX(), checkY, pos.getZ());
        }

        return pos;
    }

    // ========== 游戏控制命令 ==========

    // 开始游戏
    private static int executeStart(CommandContext<CommandSourceStack> context) {
        String gameMode = DeltaForceMod.getInstance().getGameMode();
        boolean isTestMode = gameMode.equals("test");

        // 检查是否设置了占点顺序
        if (CaptureOrderManager.getInstance().getMaxOrderNumber() == 0) {
            context.getSource().sendFailure(
                    Component.literal("§c[三角洲系统] 无法开始游戏！请先设置占点顺序！\n§7使用: /deltaforcesystem stronghold order <数字> <据点1> [据点2]...").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // 测试模式下跳过队伍检查
        if (!isTestMode) {
            // 正常模式：检查是否有GTI玩家
            boolean hasGTI = false;
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                if (TeamManager.getInstance().isGTI(player)) {
                    hasGTI = true;
                    break;
                }
            }
            if (!hasGTI) {
                context.getSource().sendFailure(
                        Component.literal("§c[三角洲系统] 无法开始游戏！请先分配GTI玩家！\n§7使用: /deltaforcesystem team set <玩家> GTI").withStyle(ChatFormatting.RED)
                );
                return 0;
            }

            // 正常模式：检查是否有HAAVK玩家
            boolean hasHAAVK = false;
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                if (TeamManager.getInstance().isHAAVK(player)) {
                    hasHAAVK = true;
                    break;
                }
            }
            if (!hasHAAVK) {
                context.getSource().sendFailure(
                        Component.literal("§c[三角洲系统] 无法开始游戏！请先分配HAAVK玩家！\n§7使用: /deltaforcesystem team set <玩家> HAAVK").withStyle(ChatFormatting.RED)
                );
                return 0;
            }
        } else {
            // 测试模式：如果没有GTI玩家，自动将第一个玩家设为GTI
            boolean hasGTI = false;
            ServerPlayer firstPlayer = null;
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                if (firstPlayer == null) firstPlayer = player;
                if (TeamManager.getInstance().isGTI(player)) {
                    hasGTI = true;
                    break;
                }
            }
            if (!hasGTI && firstPlayer != null) {
                final ServerPlayer finalFirstPlayer = firstPlayer;  // 创建final变量
                TeamManager.getInstance().setPlayerTeam(finalFirstPlayer, TeamManager.Team.GTI);
                context.getSource().sendSuccess(() ->
                                Component.literal("§a[测试模式] 自动将 " + finalFirstPlayer.getName().getString() + " 设置为GTI").withStyle(ChatFormatting.GREEN),
                        false);
            }

            // 测试模式：如果没有HAAVK玩家，自动将第二个玩家设为HAAVK
            boolean hasHAAVK = false;
            ServerPlayer secondPlayer = null;
            int count = 0;
            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                if (TeamManager.getInstance().isHAAVK(player)) {
                    hasHAAVK = true;
                    break;
                }
                count++;
                if (count == 2 && secondPlayer == null) {
                    secondPlayer = player;
                }
            }
            if (!hasHAAVK && secondPlayer != null) {
                // 确保secondPlayer不是第一个玩家
                ServerPlayer firstPlayerCheck = null;
                for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    firstPlayerCheck = player;
                    break;
                }
                if (secondPlayer != firstPlayerCheck) {
                    final ServerPlayer finalSecondPlayer = secondPlayer;  // 创建final变量
                    TeamManager.getInstance().setPlayerTeam(finalSecondPlayer, TeamManager.Team.HAAVK);
                    context.getSource().sendSuccess(() ->
                                    Component.literal("§a[测试模式] 自动将 " + finalSecondPlayer.getName().getString() + " 设置为HAAVK").withStyle(ChatFormatting.GREEN),
                            false);
                }
            }
        }

        DeltaForceMod.getInstance().startGame();
        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 游戏已开始！").withStyle(ChatFormatting.GREEN),
                true);

        // 显示当前模式提示
        if (isTestMode) {
            context.getSource().sendSuccess(() ->
                            Component.literal("§7[测试模式] 无队伍限制，可单人测试").withStyle(ChatFormatting.GRAY),
                    false);
        }

        return 1;
    }

    private static int executeStop(CommandContext<CommandSourceStack> context) {
        DeltaForceMod.getInstance().stopGame();
        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 游戏已结束！").withStyle(ChatFormatting.RED),
                true);
        return 1;
    }

    // ========== 兵力管理命令 ==========

    // 重置游戏（新的一局）
    private static int executeReset(CommandContext<CommandSourceStack> context) {
        DeltaForceMod.getInstance().resetGame();
        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 游戏已重置！新的一局开始，兵力: 120").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    // 完全重置
    private static int executeFullReset(CommandContext<CommandSourceStack> context) {
        DeltaForceMod.getInstance().fullReset();
        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 完全重置！占点顺序已清除，兵力: 120").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        int tickets = DeltaForceMod.getInstance().getAttackerTickets();
        boolean gameStarted = DeltaForceMod.getInstance().isGameStarted();
        boolean active = DeltaForceMod.getInstance().isGameActive();

        String status;
        if (!gameStarted) {
            status = "未开始";
        } else if (active) {
            status = "进行中";
        } else {
            status = "已结束";
        }

        ChatFormatting statusColor = gameStarted ? (active ? ChatFormatting.GREEN : ChatFormatting.RED) : ChatFormatting.YELLOW;

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 攻方剩余兵力: ")
                                .withStyle(ChatFormatting.GOLD)
                                .append(Component.literal(String.valueOf(tickets)).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" | 游戏状态: ").withStyle(ChatFormatting.GOLD))
                                .append(Component.literal(status).withStyle(statusColor)),
                false);
        return 1;
    }

    private static int executeSet(CommandContext<CommandSourceStack> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        DeltaForceMod.getInstance().setTickets(amount);

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 兵力已设置为: " + amount)
                                .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int newAmount = DeltaForceMod.getInstance().addTickets(amount);

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 兵力增加 " + amount + "，当前兵力: " + newAmount)
                                .withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int executeReduce(CommandContext<CommandSourceStack> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int newAmount = DeltaForceMod.getInstance().reduceTicketsByAmount(amount);

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 兵力减少 " + amount + "，当前兵力: " + newAmount)
                                .withStyle(ChatFormatting.YELLOW),
                true);
        return 1;
    }

    // ========== 队伍管理命令 ==========

    private static int executeTeamSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        String teamName = StringArgumentType.getString(context, "team").toUpperCase();

        TeamManager.Team team;
        String coloredName;
        if (teamName.equals("GTI")) {
            team = TeamManager.Team.GTI;
            coloredName = "§cGTI (攻方)";
        } else if (teamName.equals("HAAVK")) {
            team = TeamManager.Team.HAAVK;
            coloredName = "§9HAAVK (守方)";
        } else if (teamName.equals("NONE")) {
            team = TeamManager.Team.NONE;
            coloredName = "§7未分配";
        } else {
            context.getSource().sendFailure(Component.literal("无效的队伍名称！请使用 GTI、HAAVK 或 NONE").withStyle(ChatFormatting.RED));
            return 0;
        }

        TeamManager.getInstance().setPlayerTeam(targetPlayer, team);

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 已将玩家 ")
                                .append(targetPlayer.getDisplayName())
                                .append(Component.literal(" 分配到队伍: " + coloredName)),
                true);

        return 1;
    }

    private static int executeTeamRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        TeamManager.getInstance().removePlayerTeam(targetPlayer);

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 已移除玩家 ")
                                .append(targetPlayer.getDisplayName())
                                .append(Component.literal(" 的队伍分配"))
                                .withStyle(ChatFormatting.YELLOW),
                true);

        targetPlayer.sendSystemMessage(
                Component.literal("[三角洲系统] 你的队伍分配已被移除").withStyle(ChatFormatting.YELLOW)
        );

        return 1;
    }

    private static int executeTeamList(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Component.literal("§6=== 队伍分配情况 ==="),
                false);

        // GTI队伍列表（红色）
        context.getSource().sendSuccess(() ->
                        Component.literal("§cGTI (攻方 - 死亡扣兵力):"),
                false);

        boolean hasGTI = false;
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (TeamManager.getInstance().isGTI(player)) {
                context.getSource().sendSuccess(() ->
                                Component.literal("  §7- §c" + player.getName().getString()),
                        false);
                hasGTI = true;
            }
        }

        if (!hasGTI) {
            context.getSource().sendSuccess(() ->
                            Component.literal("  §7暂无GTI队员"),
                    false);
        }

        // HAAVK队伍列表（蓝色）
        context.getSource().sendSuccess(() ->
                        Component.literal("§9HAAVK (守方 - 无限兵力):"),
                false);

        boolean hasHAAVK = false;
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (TeamManager.getInstance().isHAAVK(player)) {
                context.getSource().sendSuccess(() ->
                                Component.literal("  §7- §9" + player.getName().getString()),
                        false);
                hasHAAVK = true;
            }
        }

        if (!hasHAAVK) {
            context.getSource().sendSuccess(() ->
                            Component.literal("  §7暂无HAAVK队员"),
                    false);
        }

        // 未分配玩家
        context.getSource().sendSuccess(() ->
                        Component.literal("§7未分配队伍:"),
                false);

        boolean hasNone = false;
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (TeamManager.getInstance().getPlayerTeam(player) == TeamManager.Team.NONE) {
                context.getSource().sendSuccess(() ->
                                Component.literal("  §7- §e" + player.getName().getString()),
                        false);
                hasNone = true;
            }
        }

        if (!hasNone) {
            context.getSource().sendSuccess(() ->
                            Component.literal("  §7所有玩家都已分配队伍"),
                    false);
        }

        return 1;
    }

    private static int executeTeamClear(CommandContext<CommandSourceStack> context) {
        TeamManager.getInstance().clearAllTeams();
        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 已清空所有队伍分配").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int executeTeamCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(targetPlayer);

        String teamDisplay;
        ChatFormatting color;
        if (team == TeamManager.Team.GTI) {
            teamDisplay = "GTI (攻方)";
            color = ChatFormatting.RED;
        } else if (team == TeamManager.Team.HAAVK) {
            teamDisplay = "HAAVK (守方)";
            color = ChatFormatting.GREEN;
        } else {
            teamDisplay = "未分配";
            color = ChatFormatting.YELLOW;
        }

        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 玩家 ")
                                .append(targetPlayer.getDisplayName())
                                .append(Component.literal(" 的队伍: " + teamDisplay).withStyle(color)),
                false);

        return 1;
    }

    // ========== 据点管理命令 ==========

    private static int executeStrongholdSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ServerPlayer player = context.getSource().getPlayerOrException();

        BlockPos pos = getBlockPosUnderPlayer(player);

        if (StrongholdManager.getInstance().hasFirstPoint(player)) {
            StrongholdManager.getInstance().setSecondPoint(player, pos);
        } else {
            StrongholdManager.getInstance().setFirstPoint(player, pos, name, "minecraft:stone");
        }

        return 1;
    }

    private static int executeStrongholdClear(CommandContext<CommandSourceStack> context) {
        StrongholdManager.getInstance().clearAllStrongholds();
        context.getSource().sendSuccess(() ->
                        Component.literal("[据点系统] 已清除所有据点").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int executeStrongholdList(CommandContext<CommandSourceStack> context) {
        var strongholds = StrongholdManager.getInstance().getAllStrongholds();

        if (strongholds.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            Component.literal("[据点系统] 暂无任何据点").withStyle(ChatFormatting.YELLOW),
                    false);
            return 1;
        }

        context.getSource().sendSuccess(() ->
                        Component.literal("§6=== 据点列表 ==="),
                false);

        List<StrongholdManager.Stronghold> strongholdList = new ArrayList<>(strongholds.values());
        for (int i = 0; i < strongholdList.size(); i++) {
            StrongholdManager.Stronghold sh = strongholdList.get(i);
            final int number = i + 1;
            context.getSource().sendSuccess(() ->
                            Component.literal("§7" + number + ". §e" + sh.name + " §7: [" + sh.minX + "," + sh.minY + "," + sh.minZ + "] -> [" + sh.maxX + "," + sh.maxY + "," + sh.maxZ + "]"),
                    false);
        }

        return 1;
    }

    private static int executeStrongholdRemove(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");

        if (StrongholdManager.getInstance().removeStronghold(name)) {
            context.getSource().sendSuccess(() ->
                            Component.literal("[据点系统] 已删除据点: " + name).withStyle(ChatFormatting.GREEN),
                    true);
        } else {
            context.getSource().sendFailure(
                    Component.literal("[据点系统] 未找到据点: " + name).withStyle(ChatFormatting.RED)
            );
        }

        return 1;
    }

    private static int executeStrongholdInfo(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        StrongholdManager.Stronghold sh = StrongholdManager.getInstance().getStronghold(name);

        if (sh == null) {
            context.getSource().sendFailure(
                    Component.literal("[据点系统] 未找到据点: " + name).withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        context.getSource().sendSuccess(() ->
                        Component.literal("§6=== 据点信息: " + sh.name + " ==="),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§7范围: §fX[" + sh.minX + "~" + sh.maxX + "] Y[" + sh.minY + "~" + sh.maxY + "] Z[" + sh.minZ + "~" + sh.maxZ + "]"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§7中心点: §f" + sh.getCenter().getX() + ", " + sh.getCenter().getY() + ", " + sh.getCenter().getZ()),
                false);

        return 1;
    }

    // ========== 占点顺序命令 ==========

    // 设置占点顺序（支持多个据点）
    private static int executeStrongholdOrderSet(CommandContext<CommandSourceStack> context) {
        int number = IntegerArgumentType.getInteger(context, "number");
        String orderStr = StringArgumentType.getString(context, "strongholds");
        String[] parts = orderStr.split(" ");

        List<String> strongholds = new ArrayList<>();
        for (String name : parts) {
            if (StrongholdManager.getInstance().getStronghold(name) == null) {
                context.getSource().sendFailure(
                        Component.literal("据点不存在: " + name).withStyle(ChatFormatting.RED)
                );
                return 0;
            }
            strongholds.add(name);
        }

        CaptureOrderManager.getInstance().setCaptureOrder(number, strongholds);

        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 已设置占点顺序: §e第" + number + "位 -> §f" + String.join(", ", strongholds))
                                .withStyle(ChatFormatting.GREEN),
                true);

        // 显示当前所有顺序
        String orderList = CaptureOrderManager.getInstance().getOrderListFormatted();
        if (!orderList.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            Component.literal("§7当前顺序: " + orderList),
                    false);
        }

        return 1;
    }

    private static int executeStrongholdOrderList(CommandContext<CommandSourceStack> context) {
        String orderList = CaptureOrderManager.getInstance().getOrderListFormatted();

        if (orderList.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            Component.literal("[据点系统] 未设置占点顺序").withStyle(ChatFormatting.YELLOW),
                    false);
            return 1;
        }

        context.getSource().sendSuccess(() ->
                        Component.literal("§6=== 占点顺序 ==="),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§7" + orderList),
                false);

        return 1;
    }

    private static int executeStrongholdOrderClear(CommandContext<CommandSourceStack> context) {
        CaptureOrderManager.getInstance().clearCaptureOrder();
        context.getSource().sendSuccess(() ->
                        Component.literal("[三角洲系统] 已清除所有占点顺序").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int executeStrongholdProgress(CommandContext<CommandSourceStack> context) {
        String progressInfo = CaptureOrderManager.getInstance().getProgressInfo();
        context.getSource().sendSuccess(() ->
                        Component.literal("§6=== 占点进度 ===\n" + progressInfo),
                false);
        return 1;
    }

    private static int executeStrongholdCancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        StrongholdManager.getInstance().clearPlayerSelection(player);
        return 1;
    }

    // ========== 帮助命令 ==========

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() ->
                        Component.literal("§6§l========== 三角洲行动系统帮助 =========="),
                false);

        // 游戏控制
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem start §7- 开始游戏，显示兵力并开始占点"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stop §7- 结束游戏，隐藏兵力并重置"),
                false);

        // 兵力管理
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem rest §7- 重置兵力为120"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem status §7- 查看当前兵力状态"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem set <数量> §7- 设置兵力值 (0-999)"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem add <数量> §7- 增加兵力值"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem reduce <数量> §7- 减少兵力值"),
                false);

        // 队伍管理
        // 兵力管理
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem rest §7- 重置兵力为120，开始新的一局（保留占点顺序）"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem fullreset §7- 完全重置，清除所有占点顺序"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem team remove <玩家> §7- 移除玩家队伍"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem team list §7- 查看所有队伍分配"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem team clear §7- 清空所有队伍"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem team check <玩家> §7- 查看指定玩家队伍"),
                false);

        // 据点管理
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold set <名称> §7- 创建据点（两点定义）"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold list §7- 列出所有据点"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold info <名称> §7- 查看据点详细信息"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold remove <名称> §7- 删除指定据点"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold clear §7- 删除所有据点"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold cancel §7- 取消当前据点选择"),
                false);

        // 占点系统
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold order <数字> <据点1> [据点2]... §7- 设置占点顺序（多个据点）"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold orderlist §7- 查看占点顺序"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold orderclear §7- 清除所有占点顺序"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem stronghold progress §7- 查看当前占点进度"),
                false);

        // 调试命令
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem debug checkpos §7- 检查当前位置是否在据点内"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem debug checkstronghold <名称> §7- 查看据点详细范围"),
                false);

        // 权限管理命令
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem op <玩家名> §7- 授予玩家deltaforcesystem命令权限"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem deop <玩家名> §7- 撤销玩家deltaforcesystem命令权限"),
                false);

        context.getSource().sendSuccess(() ->
                        Component.literal("§e/deltaforcesystem permlist §7- 查看有权限使用命令的玩家列表"),
                false);
        context.getSource().sendSuccess(() ->
                        Component.literal("§7----------------------------------------"),
                false);

        context.getSource().sendSuccess(() ->
                        Component.literal("§e§l如想开始游戏并占点，请先输入/deltaforcesystem stronghold order来进行选择占点顺序,后再输入/deltaforcesystem start开始游戏"),
                false);

        context.getSource().sendSuccess(() ->
                        Component.literal("§e§l=============================================="),
                false);

        return 1;
    }

    // 切换游戏模式
    private static int executeMode(CommandContext<CommandSourceStack> context) {
        String mode = StringArgumentType.getString(context, "mode").toLowerCase();

        if (!mode.equals("test") && !mode.equals("normal")) {
            context.getSource().sendFailure(
                    Component.literal("无效的模式！请使用 test 或 normal").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        DeltaForceMod.getInstance().setGameMode(mode);

        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 游戏模式已切换为: " +
                                (mode.equals("test") ? "§a测试模式(无限制)" : "§c正常模式(有限制)")),
                true);

        return 1;
    }
    // 授予玩家deltaforcesystem命令权限
    private static int executeGrantPermission(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

        PermissionManager.getInstance().addPermission(targetPlayer);

        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 已授予玩家 " + targetPlayer.getName().getString() + " deltaforcesystem 命令权限")
                                .withStyle(ChatFormatting.GREEN),
                true);

        targetPlayer.sendSystemMessage(
                Component.literal("§a[三角洲系统] 你已获得 deltaforcesystem 命令使用权限").withStyle(ChatFormatting.GREEN)
        );

        return 1;
    }

    private static int executeRevokePermission(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

        PermissionManager.getInstance().removePermission(targetPlayer);

        context.getSource().sendSuccess(() ->
                        Component.literal("§c[三角洲系统] 已撤销玩家 " + targetPlayer.getName().getString() + " deltaforcesystem 命令权限")
                                .withStyle(ChatFormatting.RED),
                true);

        targetPlayer.sendSystemMessage(
                Component.literal("§c[三角洲系统] 你的 deltaforcesystem 命令使用权限已被撤销").withStyle(ChatFormatting.RED)
        );

        return 1;
    }

    // 查看权限列表
    private static int executePermList(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        Set<UUID> allowedPlayers = PermissionManager.getInstance().getAllowedPlayers();

        context.getSource().sendSuccess(() ->
                        Component.literal("§6=== 三角洲系统权限列表 ==="),
                false);

        // 显示OP玩家（始终有权限）
        context.getSource().sendSuccess(() ->
                        Component.literal("§7[OP] §c(始终拥有权限):"),
                false);
        boolean hasOp = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                context.getSource().sendSuccess(() ->
                                Component.literal("  §7- §e" + player.getName().getString()),
                        false);
                hasOp = true;
            }
        }
        if (!hasOp) {
            context.getSource().sendSuccess(() ->
                            Component.literal("  §7暂无OP在线"),
                    false);
        }

        // 显示已授权的普通玩家
        context.getSource().sendSuccess(() ->
                        Component.literal("§7[已授权] §a(通过/op命令授权):"),
                false);
        if (allowedPlayers.isEmpty()) {
            context.getSource().sendSuccess(() ->
                            Component.literal("  §7暂无已授权玩家"),
                    false);
        } else {
            for (UUID uuid : allowedPlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    context.getSource().sendSuccess(() ->
                                    Component.literal("  §7- §a" + player.getName().getString()),
                            false);
                }
            }
        }

        context.getSource().sendSuccess(() ->
                        Component.literal("§6================================="),
                false);

        return 1;
    }

    private static int executeDebugLogOn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ModLogger.setDebugMode(true);
        ModLogger.sendPlayerSuccess(context.getSource().getPlayerOrException(), "调试日志已开启");
        return 1;
    }

    // 关闭调试日志
    private static int executeDebugLogOff(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ModLogger.setDebugMode(false);
        ModLogger.sendPlayerSuccess(context.getSource().getPlayerOrException(), "调试日志已关闭");
        return 1;
    }

    private static int executeNametagAlways(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NametagVisibility.setPlayerRule(player.getUUID(), NametagVisibility.Rule.ALWAYS);
        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 名字标签已设置为: 总是显示"),
                true);
        return 1;
    }

    private static int executeNametagHide(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NametagVisibility.setPlayerRule(player.getUUID(), NametagVisibility.Rule.HIDE_FOR_OTHER_TEAMS);
        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 名字标签已设置为: 对其他队伍隐藏"),
                true);
        return 1;
    }

    private static int executeNametagNever(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NametagVisibility.setPlayerRule(player.getUUID(), NametagVisibility.Rule.NEVER);
        context.getSource().sendSuccess(() ->
                        Component.literal("§a[三角洲系统] 名字标签已设置为: 永远不显示"),
                true);
        return 1;
    }
}
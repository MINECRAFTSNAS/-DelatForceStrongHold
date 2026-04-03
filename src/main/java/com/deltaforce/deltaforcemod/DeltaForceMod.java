package com.deltaforce.deltaforcemod;

import com.deltaforce.deltaforcemod.client.ClientTeamData;
import com.deltaforce.deltaforcemod.command.DeltaForceCommand;
import com.deltaforce.deltaforcemod.event.PlayerDeathHandler;
import com.deltaforce.deltaforcemod.network.SyncTeamPacket;
import com.deltaforce.deltaforcemod.network.SyncTicketsPacket;
import com.deltaforce.deltaforcemod.util.ModLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("removal")
@Mod(DeltaForceMod.MOD_ID)
public class DeltaForceMod {
    public static final String MOD_ID = "deltaforcemod";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private String gameMode = "normal";
    private static DeltaForceMod instance;
    private int attackerTickets = 120;
    private boolean gameStarted = false;
    private boolean gameActive = true;
    private Map<ServerPlayer, CustomBossEvent> gtiTicketBossBar = new HashMap<>();
    private Map<ServerPlayer, CustomBossEvent> haavkTicketBossBar = new HashMap<>();
    private boolean isShowingGameEndTitle = false;

    // 网络通道
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID + ":main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public DeltaForceMod() {
        instance = this;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PlayerDeathHandler());

        // 初始化管理器
        TeamManager.getInstance();
        StrongholdManager.getInstance();
        CaptureOrderManager.getInstance();
        PermissionManager.getInstance();

        // 注册网络包
        CHANNEL.registerMessage(0, SyncTicketsPacket.class,
                SyncTicketsPacket::encode,
                SyncTicketsPacket::decode,
                SyncTicketsPacket::handle);

        CHANNEL.registerMessage(1, SyncTeamPacket.class,
                SyncTeamPacket::encode,
                SyncTeamPacket::decode,
                SyncTeamPacket::handle);
    }

    private void setup(final FMLCommonSetupEvent event) {
        ModLogger.logInit("三角洲行动兵力系统", "加载中...");
        ModLogger.logSeparator();
        ModLogger.logInit("三角洲行动兵力系统", "加载完成！");
    }

    public static DeltaForceMod getInstance() {
        return instance;
    }

    // Getters
    public int getAttackerTickets() {
        return attackerTickets;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isGameActive() {
        return gameActive;
    }

    // 开始游戏
    public void startGame() {
        if (!gameStarted) {
            gameStarted = true;
            gameActive = true;
            attackerTickets = 120;

            CaptureOrderManager.getInstance().reset();

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    updateTicketBossBar(player);
                }
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§a§l=== 游戏开始！GTI攻方兵力: 120 ==="), false);
            }

            CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncTicketsPacket(attackerTickets));
        }
    }

    // 结束游戏
    public void stopGame() {
        if (gameStarted) {
            gameStarted = false;
            gameActive = false;

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    removeTicketBossBar(player);
                }
            }
        }
    }

    // 更新兵力BossBar（阵营独立）
    public void updateTicketBossBar(ServerPlayer player) {
        if (!gameStarted) {
            removeTicketBossBar(player);
            return;
        }

        TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);
        int progress = attackerTickets;
        float percentage = progress / 120.0f;

        if (team == TeamManager.Team.GTI) {
            CustomBossEvent bossBar = gtiTicketBossBar.get(player);
            if (bossBar == null) {
                bossBar = new CustomBossEvent(
                        new ResourceLocation(MOD_ID, "gti_tickets_" + player.getUUID()),
                        Component.literal("§a我方剩余兵力: §f" + progress)
                );
                bossBar.setColor(BossEvent.BossBarColor.GREEN);
                bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                bossBar.setCreateWorldFog(false);
                bossBar.setDarkenScreen(false);
                gtiTicketBossBar.put(player, bossBar);
            }
            bossBar.setName(Component.literal("§a我方剩余兵力: §f" + progress));
            bossBar.setProgress(percentage);
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
            CustomBossEvent haavkBar = haavkTicketBossBar.remove(player);
            if (haavkBar != null) {
                haavkBar.removePlayer(player);
            }
        } else if (team == TeamManager.Team.HAAVK) {
            CustomBossEvent bossBar = haavkTicketBossBar.get(player);
            if (bossBar == null) {
                bossBar = new CustomBossEvent(
                        new ResourceLocation(MOD_ID, "haavk_tickets_" + player.getUUID()),
                        Component.literal("§c敌方剩余兵力: §f" + progress)
                );
                bossBar.setColor(BossEvent.BossBarColor.RED);
                bossBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                bossBar.setCreateWorldFog(false);
                bossBar.setDarkenScreen(false);
                haavkTicketBossBar.put(player, bossBar);
            }
            bossBar.setName(Component.literal("§c敌方剩余兵力: §f" + progress));
            bossBar.setProgress(percentage);
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
            CustomBossEvent gtiBar = gtiTicketBossBar.remove(player);
            if (gtiBar != null) {
                gtiBar.removePlayer(player);
            }
        } else {
            removeTicketBossBar(player);
        }
    }

    private void removeTicketBossBar(ServerPlayer player) {
        CustomBossEvent gtiBar = gtiTicketBossBar.remove(player);
        if (gtiBar != null) {
            gtiBar.removePlayer(player);
        }
        CustomBossEvent haavkBar = haavkTicketBossBar.remove(player);
        if (haavkBar != null) {
            haavkBar.removePlayer(player);
        }
    }

    // 减少兵力（死亡时调用）
    public void reduceTickets() {
        if (!gameStarted || !gameActive) return;

        attackerTickets--;
        LOGGER.info("攻方剩余兵力: " + attackerTickets);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateTicketBossBar(player);
            }
        }

        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncTicketsPacket(attackerTickets));

        if (attackerTickets <= 0 && gameActive) {
            gameActive = false;
            handleGameEnd(server);
        }
    }

    // 处理游戏结束
    private void handleGameEnd(MinecraftServer server) {
        if (isShowingGameEndTitle) return;
        isShowingGameEndTitle = true;

        LOGGER.info("游戏结束！攻方兵力耗尽！");

        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                TeamManager.Team team = TeamManager.getInstance().getPlayerTeam(player);

                if (team == TeamManager.Team.GTI) {
                    showTitleToPlayer(player,
                            "§c兵力耗尽，我们失败了！",
                            "§eGame Over",
                            20, 60, 20);
                } else if (team == TeamManager.Team.HAAVK) {
                    showTitleToPlayer(player,
                            "§a敌方兵力耗尽，我们胜利了！",
                            "§6Victory!",
                            20, 60, 20);
                }
            }

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    server.execute(() -> {
                        startNewGame();
                        isShowingGameEndTitle = false;
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 新的一局（保留占点顺序，重置兵力和进度）
    public void startNewGame() {
        attackerTickets = 120;
        gameActive = true;
        gameStarted = true;
        isShowingGameEndTitle = false;

        CaptureOrderManager.getInstance().reset();

        LOGGER.info("新的一局开始！攻方剩余兵力: 120，当前占点顺序: {}",
                CaptureOrderManager.getInstance().getOrderListFormatted());

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateTicketBossBar(player);
            }
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§a§l=== 新的一局开始！ ==="), false);
        }

        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncTicketsPacket(attackerTickets));
    }

    // 重置游戏（新的一局，保留占点顺序）
    public void resetGame() {
        attackerTickets = 120;
        gameActive = true;
        gameStarted = true;
        isShowingGameEndTitle = false;

        CaptureOrderManager.getInstance().reset();

        LOGGER.info("游戏已重置！攻方剩余兵力: 120");

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateTicketBossBar(player);
            }
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§a§l=== 新的一局开始！ ==="), false);
        }

        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncTicketsPacket(attackerTickets));
    }

    private void showTitleToPlayer(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }

    // 设置兵力值
    public void setTickets(int amount) {
        if (amount < 0) amount = 0;
        attackerTickets = amount;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateTicketBossBar(player);
            }
        }

        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncTicketsPacket(attackerTickets));

        if (attackerTickets <= 0 && gameActive && gameStarted) {
            gameActive = false;
            handleGameEnd(server);
        } else if (attackerTickets > 0 && !gameActive && gameStarted) {
            gameActive = true;
            isShowingGameEndTitle = false;
        }
    }

    public int addTickets(int amount) {
        int newAmount = attackerTickets + amount;
        setTickets(newAmount);
        return newAmount;
    }

    public int reduceTicketsByAmount(int amount) {
        int newAmount = attackerTickets - amount;
        if (newAmount < 0) newAmount = 0;
        setTickets(newAmount);
        return newAmount;
    }

    public String getGameMode() {
        return gameMode;
    }

    // 设置游戏模式
    public void setGameMode(String mode) {
        if (mode.equals("test") || mode.equals("normal")) {
            this.gameMode = mode;
            LOGGER.info("游戏模式已切换为: " + (mode.equals("test") ? "测试模式(无限制)" : "正常模式(有限制)"));

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§e[三角洲系统] 游戏模式已切换为: " +
                                (mode.equals("test") ? "§a测试模式(无限制)" : "§c正常模式(有限制)")),
                        false);
            }
        }
    }

    // 完全重置（清除所有数据，包括占点顺序，并结束游戏）
    public void fullReset() {
        attackerTickets = 120;
        gameActive = true;
        gameStarted = false;
        isShowingGameEndTitle = false;

        CaptureOrderManager.getInstance().clearCaptureOrder();

        LOGGER.info("完全重置！占点顺序已清除，游戏已结束");

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateTicketBossBar(player);
                CustomBossEvent gtiBar = gtiTicketBossBar.get(player);
                if (gtiBar != null) gtiBar.setVisible(false);
                CustomBossEvent haavkBar = haavkTicketBossBar.get(player);
                if (haavkBar != null) haavkBar.setVisible(false);
            }
        }

        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncTicketsPacket(attackerTickets));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        DataManager.getInstance().loadData();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            updateTicketBossBar(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            player.sendSystemMessage(
                    Component.literal("§e输入 §f/deltaforcesystem help §e查看所有命令")
            );
            TeamManager.getInstance().syncTeamDataToPlayer(player);

            TeamManager.Team savedTeam = TeamManager.getInstance().getPlayerTeam(player);
            if (savedTeam != TeamManager.Team.NONE) {
                player.sendSystemMessage(
                        Component.literal("§e[三角洲系统] 你已恢复队伍: " +
                                (savedTeam == TeamManager.Team.GTI ? "§cGTI (攻方)" : "§aHAAVK (守方)"))
                );
            }

            updateTicketBossBar(player);
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncTicketsPacket(attackerTickets));
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            removeTicketBossBar(player);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DeltaForceCommand.register(event.getDispatcher());
    }

    // ========== 客户端名字颜色 ==========
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onRenderNameTag(RenderNameTagEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            Minecraft mc = Minecraft.getInstance();

            // 不处理自己的名字
            if (player == mc.player) {
                return;
            }

            // 从客户端缓存获取队伍信息（而不是直接调用服务端方法）
            TeamManager.Team team = ClientTeamData.getTeam(player.getUUID());

            if (team == TeamManager.Team.GTI) {
                event.setContent(Component.literal(player.getName().getString()).withStyle(ChatFormatting.RED));
            } else if (team == TeamManager.Team.HAAVK) {
                event.setContent(Component.literal(player.getName().getString()).withStyle(ChatFormatting.BLUE));
            }
        }
    }

    // ========== 窗口标题修改 ==========
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class WindowTitleManager {
        private static String lastTitle = "";
        private static int tickCounter = 0;

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            String playerName = mc.player.getName().getString();
            String newTitle = playerName + " | 三角洲行动 | 1.20.1";

            tickCounter++;
            if (tickCounter >= 20 || !newTitle.equals(lastTitle)) {
                tickCounter = 0;
                lastTitle = newTitle;

                try {
                    mc.getWindow().setTitle(newTitle);
                } catch (Exception e) {
                }
            }
        }
    }
}
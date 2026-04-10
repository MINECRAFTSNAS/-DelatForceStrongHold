package com.deltaforce.deltaforcemod;

import com.deltaforce.deltaforcemod.client.ClientTeamData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 控制玩家名字标签（Nametag）可见性的类
 */
public class NametagVisibility {

    public enum Rule {
        ALWAYS,               // 总是显示
        HIDE_FOR_OTHER_TEAMS, // 对敌对队伍隐藏
        NEVER                 // 永远不显示
    }

    private static final Map<UUID, Rule> playerRules = new HashMap<>();

    public static void setPlayerRule(UUID uuid, Rule rule) {
        playerRules.put(uuid, rule);
    }

    public static Rule getPlayerRule(UUID uuid) {
        return playerRules.getOrDefault(uuid, Rule.HIDE_FOR_OTHER_TEAMS);
    }

    @Mod.EventBusSubscriber(modid = DeltaForceMod.MOD_ID, value = Dist.CLIENT)
    public static class ClientHandler {

        @SubscribeEvent
        public static void onRenderNameTag(RenderNameTagEvent event) {
            if (!(event.getEntity() instanceof Player target)) return;

            Player viewer = Minecraft.getInstance().player;
            if (viewer == null || viewer == target) return;

            TeamManager.Team viewerTeam = ClientTeamData.getTeam(viewer.getUUID());
            TeamManager.Team targetTeam = ClientTeamData.getTeam(target.getUUID());

            Rule rule = getPlayerRule(viewer.getUUID());
            boolean isEnemy = (targetTeam != TeamManager.Team.NONE && viewerTeam != targetTeam);

            boolean shouldShow = false;
            switch (rule) {
                case ALWAYS -> shouldShow = true;
                case HIDE_FOR_OTHER_TEAMS -> shouldShow = !isEnemy;
                case NEVER -> shouldShow = false;
            }

            if (shouldShow) {
                // 正常显示逻辑
                if (!isEnemy && viewerTeam == targetTeam && viewerTeam != TeamManager.Team.NONE) {
                    if (viewerTeam == TeamManager.Team.GTI) {
                        event.setContent(Component.literal(target.getName().getString()).withStyle(ChatFormatting.RED));
                    } else if (viewerTeam == TeamManager.Team.HAAVK) {
                        event.setContent(Component.literal(target.getName().getString()).withStyle(ChatFormatting.BLUE));
                    }
                }
            } else {
                // --- 修复后的隐藏逻辑 ---
                // 1. 设置内容为空组件
                event.setContent(Component.empty());
                // 2. 将渲染结果设为 DENY，这在某些 Forge 版本中可以阻止后续渲染
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        }
    }
}
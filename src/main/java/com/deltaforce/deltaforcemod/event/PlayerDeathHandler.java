package com.deltaforce.deltaforcemod.event;

import com.deltaforce.deltaforcemod.DeltaForceMod;
import com.deltaforce.deltaforcemod.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerDeathHandler {

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player killed = (Player) event.getEntity();

            if (!DeltaForceMod.getInstance().isGameStarted()) {
                return;
            }

            if (TeamManager.getInstance().isGTI(killed)) {
                DeltaForceMod.getInstance().reduceTickets();

                if (!killed.level().isClientSide) {
                    int remainingTickets = DeltaForceMod.getInstance().getAttackerTickets();
                    killed.sendSystemMessage(
                            Component.literal("[三角洲系统] GTI损失一名士兵！剩余兵力: " + remainingTickets)
                                    .withStyle(ChatFormatting.RED)
                    );
                }
            } else if (TeamManager.getInstance().isHAAVK(killed)) {
                // HAAVK死亡，不扣兵力
            } else {
                if (!killed.level().isClientSide) {
                    killed.sendSystemMessage(
                            Component.literal("[三角洲系统] 你尚未分配队伍，请使用 /deltaforcesystem team set 来分配队伍")
                                    .withStyle(ChatFormatting.YELLOW)
                    );
                }
            }
        }
    }

    // 方法1：在攻击事件中阻止（最有效）
    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        // 获取攻击者
        Player attacker = event.getEntity();
        // 获取被攻击者
        if (!(event.getTarget() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getTarget();

        // 如果是同一个人，允许
        if (attacker == target) {
            return;
        }

        // 获取双方队伍
        TeamManager.Team attackerTeam = TeamManager.getInstance().getPlayerTeam(attacker);
        TeamManager.Team targetTeam = TeamManager.getInstance().getPlayerTeam(target);

        // 同阵营禁止攻击
        if (attackerTeam == targetTeam && attackerTeam != TeamManager.Team.NONE) {
            event.setCanceled(true);
            // 只发送一次消息，避免刷屏（使用冷却机制）
            sendMessageWithCooldown(attacker, "§c你不能攻击同阵营的队员！");
        }
    }

    // 方法2：备用，防止伤害穿透
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }

        Player target = (Player) event.getEntity();
        Player attacker = (Player) event.getSource().getEntity();

        if (attacker == target) {
            return;
        }

        TeamManager.Team attackerTeam = TeamManager.getInstance().getPlayerTeam(attacker);
        TeamManager.Team targetTeam = TeamManager.getInstance().getPlayerTeam(target);

        if (attackerTeam == targetTeam && attackerTeam != TeamManager.Team.NONE) {
            event.setCanceled(true);
        }
    }

    // 消息冷却机制，防止刷屏
    private static final java.util.Map<Player, Long> lastMessageTime = new java.util.HashMap<>();

    private void sendMessageWithCooldown(Player player, String message) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(player);
        if (lastTime == null || currentTime - lastTime > 2000) { // 2秒冷却
            lastMessageTime.put(player, currentTime);
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        }
    }
}
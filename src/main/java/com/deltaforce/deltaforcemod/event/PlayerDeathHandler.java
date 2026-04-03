package com.deltaforce.deltaforcemod.event;

import com.deltaforce.deltaforcemod.DeltaForceMod;
import com.deltaforce.deltaforcemod.TeamManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
}
package com.deltaforce.deltaforcemod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "deltaforcemod")
public class KeyBindings {

    public static final String KEY_CATEGORY = "key.category.deltaforcemod";
    public static final String KEY_OPEN_GUI = "key.deltaforcemod.open_gui";

    public static KeyMapping openGuiKey = new KeyMapping(
            KEY_OPEN_GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            KEY_CATEGORY
    );

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "deltaforcemod", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(openGuiKey);
        }
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "deltaforcemod")
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (openGuiKey.consumeClick()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new DeltaForceGui());
            }
        }
    }
}
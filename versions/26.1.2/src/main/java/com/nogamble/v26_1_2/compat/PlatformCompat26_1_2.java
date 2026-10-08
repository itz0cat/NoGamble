package com.nogamble.v26_1_2.compat;

import com.nogamble.compat.PlatformCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public class PlatformCompat26_1_2 implements PlatformCompat {

    @Override
    public void sendClientChatMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(message));
        }
    }

    @Override
    public String getCurrentServerAddress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            ServerData server = mc.getCurrentServer();
            if (server != null && server.ip != null) {
                return server.ip;
            }
        }
        return "";
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}

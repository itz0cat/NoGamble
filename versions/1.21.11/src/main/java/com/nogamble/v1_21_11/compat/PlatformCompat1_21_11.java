package com.nogamble.v1_21_11.compat;

import com.nogamble.compat.PlatformCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.nio.file.Path;

public class PlatformCompat1_21_11 implements PlatformCompat {

    @Override
    public void sendClientChatMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal(message));
        }
    }

    @Override
    public String getCurrentServerAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ServerInfo entry = client.getCurrentServerEntry();
            if (entry != null && entry.address != null) {
                return entry.address;
            }
        }
        return "";
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}

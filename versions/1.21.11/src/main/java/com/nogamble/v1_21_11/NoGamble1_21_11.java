package com.nogamble.v1_21_11;

import com.nogamble.NoGamble;
import com.nogamble.v1_21_11.command.ClientCommands1_21_11;
import com.nogamble.v1_21_11.compat.PlatformCompat1_21_11;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.network.ServerInfo;

public class NoGamble1_21_11 implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PlatformCompat1_21_11 compat = new PlatformCompat1_21_11();
        NoGamble mod = NoGamble.getInstance();
        mod.init(compat);

        // Command interception: returns false to CANCEL the command, true to ALLOW it
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            boolean blocked = mod.handleOutgoingCommand(command);
            return !blocked;
        });

        // Connection events for server-specific profile matching
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo server = client.getCurrentServerEntry();
            String address = server != null ? server.address : "";
            mod.onServerJoin(address);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            mod.onServerDisconnect();
        });

        // Register client-side commands
        ClientCommands1_21_11.register();
    }
}

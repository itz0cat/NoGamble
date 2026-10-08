package com.nogamble.v26_1_2;

import com.nogamble.NoGamble;
import com.nogamble.v26_1_2.command.ClientCommands26_1_2;
import com.nogamble.v26_1_2.compat.PlatformCompat26_1_2;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.multiplayer.ServerData;

public class NoGamble26_1_2 implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PlatformCompat26_1_2 compat = new PlatformCompat26_1_2();
        NoGamble mod = NoGamble.getInstance();
        mod.init(compat);

        // Command interception: returns false to CANCEL the command, true to ALLOW it
        ClientSendMessageEvents.ALLOW_COMMAND.register(command -> {
            boolean blocked = mod.handleOutgoingCommand(command);
            return !blocked;
        });

        // Connection events for server-specific profile matching
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            ServerData server = client.getCurrentServer();
            String ip = server != null ? server.ip : "";
            mod.onServerJoin(ip);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
            mod.onServerDisconnect();
        });

        // Register client-side commands
        ClientCommands26_1_2.register();
    }
}

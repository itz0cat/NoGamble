package com.nogamble.compat;

import java.nio.file.Path;

/**
 * Compatibility interface implemented by version-specific mod modules.
 */
public interface PlatformCompat {

    /**
     * Sends a client-side message to the local chat HUD.
     */
    void sendClientChatMessage(String message);

    /**
     * Retrieves the host address of the currently connected multiplayer server, or empty string if singleplayer.
     */
    String getCurrentServerAddress();

    /**
     * Gets the game's config directory (e.g. .minecraft/config).
     */
    Path getConfigDirectory();
}

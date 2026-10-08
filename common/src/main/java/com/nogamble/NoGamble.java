package com.nogamble;

import com.nogamble.compat.PlatformCompat;
import com.nogamble.config.ModConfig;
import com.nogamble.network.ProfileFetcher;
import com.nogamble.profile.ProfileManager;
import com.nogamble.util.CommandMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Main cross-version mod controller.
 */
public class NoGamble {
    private static final Logger LOGGER = LoggerFactory.getLogger("NoGamble");
    private static final NoGamble INSTANCE = new NoGamble();

    private PlatformCompat compat;
    private ModConfig config;
    private ProfileManager profileManager;
    private ProfileFetcher profileFetcher;
    private Path configDir;
    private Path configFile;
    private Path modDataDir;

    public static NoGamble getInstance() {
        return INSTANCE;
    }

    private NoGamble() {
    }

    public synchronized void init(PlatformCompat compat) {
        this.compat = compat;
        this.configDir = compat.getConfigDirectory();
        this.configFile = configDir.resolve("nogamble.json");
        this.modDataDir = configDir.resolve("nogamble");

        // Load configuration
        this.config = ModConfig.load(configFile);

        // Initialize profile manager
        this.profileManager = new ProfileManager();
        this.profileFetcher = new ProfileFetcher();

        // Load cached profiles or default
        boolean cacheLoaded = profileManager.loadCache(modDataDir.resolve("profiles.cache.json"));
        if (!cacheLoaded) {
            LOGGER.info("No cache found. Using default profile (/cf).");
        }

        // Start background auto-updater if enabled
        if (config.isEnabled() && config.getRefreshMinutes() > 0) {
            profileFetcher.startPeriodicFetching(
                    config.getProfileUrl(),
                    config.getRefreshMinutes(),
                    profileManager,
                    modDataDir
            );
        }

        LOGGER.info("NoGamble initialized successfully (enabled={})", config.isEnabled());
    }

    /**
     * Intercepts an outgoing command. Returns true if blocked, false if allowed.
     */
    public boolean handleOutgoingCommand(String rawCommand) {
        if (config == null || !config.isEnabled()) {
            return false;
        }

        CommandMatcher.ParsedCommand parsed = CommandMatcher.parse(rawCommand);
        if (parsed.getNormalizedRoot().isEmpty()) {
            return false;
        }

        // 1. Check local allowlist first
        if (parsed.matchesAny(config.getLocalAllowlist())) {
            return false;
        }

        // 2. Check local extra blocked
        boolean isBlocked = parsed.matchesAny(config.getLocalExtraBlocked());

        // 3. Check active remote / default profiles
        if (!isBlocked && profileManager != null) {
            isBlocked = parsed.matchesAny(profileManager.getActiveBlockedCommands());
        }

        if (isBlocked) {
            if (config.isShowBlockMessages() && compat != null) {
                compat.sendClientChatMessage("§c[NoGamble] §7Blocked /§f" + parsed.getRawRoot());
            }
            LOGGER.info("Blocked command: /{}", parsed.getRawRoot());
            return true;
        }

        return false;
    }

    /**
     * Tests if a command token is blocked (used for suggestion/tab-completion filtering).
     */
    public boolean isCommandBlocked(String commandName) {
        if (config == null || !config.isEnabled()) {
            return false;
        }
        CommandMatcher.ParsedCommand parsed = CommandMatcher.parse(commandName);
        if (parsed.getNormalizedRoot().isEmpty()) {
            return false;
        }
        if (parsed.matchesAny(config.getLocalAllowlist())) {
            return false;
        }
        if (parsed.matchesAny(config.getLocalExtraBlocked())) {
            return true;
        }
        if (profileManager != null && parsed.matchesAny(profileManager.getActiveBlockedCommands())) {
            return true;
        }
        return false;
    }

    public void onServerJoin(String serverAddress) {
        if (profileManager != null) {
            profileManager.onServerConnected(serverAddress);
        }
    }

    public void onServerDisconnect() {
        if (profileManager != null) {
            profileManager.onServerDisconnected();
        }
    }

    public void manualRefresh(Consumer<String> feedback) {
        if (profileFetcher != null && config != null) {
            profileFetcher.manualRefresh(config.getProfileUrl(), profileManager, modDataDir, feedback);
        } else if (feedback != null) {
            feedback.accept("NoGamble is not initialized yet.");
        }
    }

    public boolean toggleEnabled() {
        if (config != null) {
            config.setEnabled(!config.isEnabled());
            config.save(configFile);
            return config.isEnabled();
        }
        return false;
    }

    public boolean addLocalBlocked(String command) {
        if (config != null) {
            boolean added = config.addLocalBlocked(command);
            if (added) {
                config.save(configFile);
            }
            return added;
        }
        return false;
    }

    public boolean removeLocalBlocked(String command) {
        if (config != null) {
            boolean removed = config.removeLocalBlocked(command);
            if (removed) {
                config.save(configFile);
            }
            return removed;
        }
        return false;
    }

    public Set<String> getAllCurrentlyBlockedCommands() {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (profileManager != null) {
            set.addAll(profileManager.getActiveBlockedCommands());
        }
        if (config != null) {
            set.addAll(config.getLocalExtraBlocked());
            set.removeAll(config.getLocalAllowlist());
        }
        return set;
    }

    public ModConfig getConfig() {
        return config;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public PlatformCompat getCompat() {
        return compat;
    }
}

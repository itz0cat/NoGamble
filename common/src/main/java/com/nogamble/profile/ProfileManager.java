package com.nogamble.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages loaded profiles, caching, server matching, and active blocked command sets.
 */
public class ProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("NoGamble/ProfileManager");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Profile defaultProfile;
    private final List<Profile> loadedProfiles = new ArrayList<>();
    private String currentServerAddress = "";
    private final Set<String> activeBlockedCommands = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final List<String> activeProfileNames = new ArrayList<>();

    public ProfileManager() {
        this.defaultProfile = new Profile(
                "default",
                "Default",
                List.of("*"),
                List.of("cf"),
                Collections.emptyList()
        );
        resetToDefault();
    }

    /**
     * Resets the active profiles to the built-in default profile (/cf).
     */
    public synchronized void resetToDefault() {
        this.loadedProfiles.clear();
        this.loadedProfiles.add(defaultProfile);
        recalculateActiveCommands();
    }

    /**
     * Loads cached profiles from disk if available.
     */
    public synchronized boolean loadCache(Path cacheFile) {
        if (!Files.exists(cacheFile)) {
            return false;
        }

        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            ProfilesData data = GSON.fromJson(reader, ProfilesData.class);
            if (data != null && data.getProfiles() != null && !data.getProfiles().isEmpty()) {
                applyProfilesData(data);
                LOGGER.info("Loaded {} profile(s) from local cache: {}", loadedProfiles.size(), cacheFile);
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read cached profiles from {}: {}", cacheFile, e.getMessage());
        }

        return false;
    }

    /**
     * Saves raw JSON profiles to the cache file.
     */
    public synchronized void saveCache(Path cacheFile, String jsonContent) {
        try {
            if (cacheFile.getParent() != null) {
                Files.createDirectories(cacheFile.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(cacheFile)) {
                writer.write(jsonContent);
            }
            LOGGER.debug("Cached profiles saved to {}", cacheFile);
        } catch (Exception e) {
            LOGGER.warn("Failed to write profiles cache to {}: {}", cacheFile, e.getMessage());
        }
    }

    /**
     * Applies newly fetched profile data.
     */
    public synchronized void applyProfilesData(ProfilesData data) {
        if (data == null || data.getProfiles() == null || data.getProfiles().isEmpty()) {
            return;
        }

        this.loadedProfiles.clear();
        this.loadedProfiles.addAll(data.getProfiles());
        recalculateActiveCommands();
    }

    /**
     * Updates the current server address and recalculates blocked commands.
     */
    public synchronized void onServerConnected(String serverAddress) {
        this.currentServerAddress = serverAddress != null ? serverAddress : "";
        recalculateActiveCommands();
        LOGGER.info("Active server: '{}'. Active profile(s): {}. Blocked commands count: {}",
                this.currentServerAddress, this.activeProfileNames, this.activeBlockedCommands.size());
    }

    /**
     * Resets server address when disconnecting.
     */
    public synchronized void onServerDisconnected() {
        this.currentServerAddress = "";
        recalculateActiveCommands();
    }

    /**
     * Recalculates the active blocked commands by matching the current server address.
     */
    public synchronized void recalculateActiveCommands() {
        this.activeBlockedCommands.clear();
        this.activeProfileNames.clear();

        if (loadedProfiles.isEmpty()) {
            loadedProfiles.add(defaultProfile);
        }

        boolean matchedAny = false;
        for (Profile profile : loadedProfiles) {
            boolean isGlobal = "global".equalsIgnoreCase(profile.getId()) || profile.matchesServer("*");
            boolean matchesCurrent = !currentServerAddress.isEmpty() && profile.matchesServer(currentServerAddress);

            if (isGlobal || matchesCurrent) {
                matchedAny = true;
                activeProfileNames.add(profile.getName());

                for (String cmd : profile.getBlockedCommands()) {
                    addCommandToSet(cmd);
                }
                for (String alias : profile.getBlockedAliases()) {
                    addCommandToSet(alias);
                }
            }
        }

        if (!matchedAny) {
            // Fall back to default profile if no server pattern matched
            activeProfileNames.add(defaultProfile.getName());
            for (String cmd : defaultProfile.getBlockedCommands()) {
                addCommandToSet(cmd);
            }
        }
    }

    private void addCommandToSet(String cmd) {
        if (cmd == null) return;
        String s = cmd.trim();
        if (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        if (!s.isEmpty()) {
            activeBlockedCommands.add(s.toLowerCase(Locale.ROOT));
        }
    }

    public synchronized Set<String> getActiveBlockedCommands() {
        return Collections.unmodifiableSet(new TreeSet<>(activeBlockedCommands));
    }

    public synchronized List<String> getActiveProfileNames() {
        return Collections.unmodifiableList(new ArrayList<>(activeProfileNames));
    }

    public synchronized List<Profile> getAllLoadedProfiles() {
        return Collections.unmodifiableList(new ArrayList<>(loadedProfiles));
    }

    public synchronized String getCurrentServerAddress() {
        return currentServerAddress;
    }
}

package com.nogamble.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages local mod configuration stored in config/nogamble.json.
 */
public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("NoGamble/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean enabled = true;
    private String profileUrl = "https://raw.githubusercontent.com/<user>/<repo>/main/profiles.json";
    private int refreshMinutes = 60;
    private boolean showBlockMessages = true;
    private final Set<String> localExtraBlocked = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> localAllowlist = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public static ModConfig load(Path configFile) {
        ModConfig config = new ModConfig();
        if (!Files.exists(configFile)) {
            config.save(configFile);
            return config;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();

            if (obj.has("enabled") && obj.get("enabled").isJsonPrimitive()) {
                config.enabled = obj.get("enabled").getAsBoolean();
            }
            if (obj.has("profileUrl") && obj.get("profileUrl").isJsonPrimitive()) {
                config.profileUrl = obj.get("profileUrl").getAsString();
            }
            if (obj.has("refreshMinutes") && obj.get("refreshMinutes").isJsonPrimitive()) {
                int minutes = obj.get("refreshMinutes").getAsInt();
                config.refreshMinutes = Math.max(1, minutes);
            }
            if (obj.has("showBlockMessages") && obj.get("showBlockMessages").isJsonPrimitive()) {
                config.showBlockMessages = obj.get("showBlockMessages").getAsBoolean();
            }
            if (obj.has("localExtraBlocked") && obj.get("localExtraBlocked").isJsonArray()) {
                Set<String> extra = GSON.fromJson(obj.get("localExtraBlocked"), new TypeToken<Set<String>>() {}.getType());
                if (extra != null) {
                    for (String s : extra) {
                        if (s != null && !s.isBlank()) {
                            config.localExtraBlocked.add(cleanCommand(s));
                        }
                    }
                }
            }
            if (obj.has("localAllowlist") && obj.get("localAllowlist").isJsonArray()) {
                Set<String> allow = GSON.fromJson(obj.get("localAllowlist"), new TypeToken<Set<String>>() {}.getType());
                if (allow != null) {
                    for (String s : allow) {
                        if (s != null && !s.isBlank()) {
                            config.localAllowlist.add(cleanCommand(s));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse config file: {}, creating backup and restoring default", configFile, e);
            config.save(configFile);
        }

        return config;
    }

    public synchronized void save(Path configFile) {
        try {
            if (configFile.getParent() != null) {
                Files.createDirectories(configFile.getParent());
            }

            JsonObject obj = new JsonObject();
            obj.addProperty("enabled", this.enabled);
            obj.addProperty("profileUrl", this.profileUrl);
            obj.addProperty("refreshMinutes", this.refreshMinutes);
            obj.addProperty("showBlockMessages", this.showBlockMessages);
            obj.add("localExtraBlocked", GSON.toJsonTree(this.localExtraBlocked));
            obj.add("localAllowlist", GSON.toJsonTree(this.localAllowlist));

            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: {}", configFile, e);
        }
    }

    private static String cleanCommand(String cmd) {
        String s = cmd.trim();
        if (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        return s.toLowerCase(Locale.ROOT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public int getRefreshMinutes() {
        return refreshMinutes;
    }

    public void setRefreshMinutes(int refreshMinutes) {
        this.refreshMinutes = Math.max(1, refreshMinutes);
    }

    public boolean isShowBlockMessages() {
        return showBlockMessages;
    }

    public void setShowBlockMessages(boolean showBlockMessages) {
        this.showBlockMessages = showBlockMessages;
    }

    public synchronized Set<String> getLocalExtraBlocked() {
        return Collections.unmodifiableSet(new TreeSet<>(this.localExtraBlocked));
    }

    public synchronized boolean addLocalBlocked(String command) {
        String clean = cleanCommand(command);
        this.localAllowlist.remove(clean);
        return this.localExtraBlocked.add(clean);
    }

    public synchronized boolean removeLocalBlocked(String command) {
        String clean = cleanCommand(command);
        return this.localExtraBlocked.remove(clean);
    }

    public synchronized Set<String> getLocalAllowlist() {
        return Collections.unmodifiableSet(new TreeSet<>(this.localAllowlist));
    }

    public synchronized boolean addLocalAllowlist(String command) {
        String clean = cleanCommand(command);
        this.localExtraBlocked.remove(clean);
        return this.localAllowlist.add(clean);
    }

    public synchronized boolean removeLocalAllowlist(String command) {
        String clean = cleanCommand(command);
        return this.localAllowlist.remove(clean);
    }
}

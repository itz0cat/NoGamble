package com.nogamble.network;

import com.google.gson.Gson;
import com.nogamble.profile.ProfileManager;
import com.nogamble.profile.ProfilesData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Asynchronously fetches and auto-updates profiles from a remote GitHub raw JSON URL.
 */
public class ProfileFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("NoGamble/Fetcher");
    private static final int MAX_PAYLOAD_SIZE = 256 * 1024; // 256 KB max
    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> periodicTask;

    private String lastEtag = null;

    public ProfileFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "NoGamble-Fetcher-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Initializes periodic fetching.
     */
    public synchronized void startPeriodicFetching(String url, int intervalMinutes, ProfileManager profileManager, Path baseDir) {
        stopPeriodicFetching();

        Path cacheFile = baseDir.resolve("profiles.cache.json");
        Path etagFile = baseDir.resolve("profiles.etag");

        // Load cached ETag if available
        if (Files.exists(etagFile)) {
            try {
                this.lastEtag = Files.readString(etagFile, StandardCharsets.UTF_8).trim();
            } catch (IOException ignored) {}
        }

        // Run immediate initial fetch
        scheduler.execute(() -> fetchProfiles(url, profileManager, cacheFile, etagFile, null));

        // Schedule periodic fetch
        long period = Math.max(1, intervalMinutes);
        periodicTask = scheduler.scheduleAtFixedRate(
                () -> fetchProfiles(url, profileManager, cacheFile, etagFile, null),
                period,
                period,
                TimeUnit.MINUTES
        );
        LOGGER.info("Scheduled profile auto-updates every {} minute(s)", period);
    }

    public synchronized void stopPeriodicFetching() {
        if (periodicTask != null && !periodicTask.isCancelled()) {
            periodicTask.cancel(false);
            periodicTask = null;
        }
    }

    /**
     * Triggers a manual background refresh.
     */
    public void manualRefresh(String url, ProfileManager profileManager, Path baseDir, Consumer<String> callback) {
        Path cacheFile = baseDir.resolve("profiles.cache.json");
        Path etagFile = baseDir.resolve("profiles.etag");
        scheduler.execute(() -> fetchProfiles(url, profileManager, cacheFile, etagFile, callback));
    }

    /**
     * Fetches profiles from URL with validation and caching.
     */
    private void fetchProfiles(String urlString, ProfileManager profileManager, Path cacheFile, Path etagFile, Consumer<String> callback) {
        if (urlString == null || urlString.isBlank() || urlString.contains("<user>/<repo>")) {
            LOGGER.debug("Profile URL is not configured or using default placeholder. Using local/cached profiles.");
            if (callback != null) {
                callback.accept("Profile URL is set to placeholder or empty. Check config/nogamble.json.");
            }
            return;
        }

        if (!urlString.toLowerCase().startsWith("https://")) {
            LOGGER.warn("Rejected non-HTTPS profile URL: {}", urlString);
            if (callback != null) {
                callback.accept("Error: Profile URL must use HTTPS.");
            }
            return;
        }

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "NoGamble-Fabric-Mod/1.0.0");

            if (lastEtag != null && !lastEtag.isEmpty()) {
                reqBuilder.header("If-None-Match", lastEtag);
            }

            HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = response.statusCode();

            if (statusCode == 304) {
                LOGGER.info("Profiles up to date (304 Not Modified).");
                if (callback != null) {
                    callback.accept("Profiles are already up to date (304 Not Modified).");
                }
                return;
            }

            if (statusCode != 200) {
                LOGGER.warn("Failed to fetch profiles: HTTP status {}", statusCode);
                if (callback != null) {
                    callback.accept("Failed to fetch profiles: HTTP " + statusCode);
                }
                return;
            }

            byte[] bodyBytes = response.body();
            if (bodyBytes.length > MAX_PAYLOAD_SIZE) {
                LOGGER.warn("Profiles payload exceeded max allowed size ({} bytes > {} max bytes). Rejecting.",
                        bodyBytes.length, MAX_PAYLOAD_SIZE);
                if (callback != null) {
                    callback.accept("Error: Downloaded profiles file exceeds max allowed size (256 KB).");
                }
                return;
            }

            String json = new String(bodyBytes, StandardCharsets.UTF_8);
            ProfilesData data = GSON.fromJson(json, ProfilesData.class);

            if (data == null || data.getProfiles() == null || data.getProfiles().isEmpty()) {
                LOGGER.warn("Fetched profiles data was empty or malformed.");
                if (callback != null) {
                    callback.accept("Error: Malformed or empty profiles JSON.");
                }
                return;
            }

            // Save new cache and update profile manager
            profileManager.applyProfilesData(data);
            profileManager.saveCache(cacheFile, json);

            // Update and save ETag
            Optional<String> newEtag = response.headers().firstValue("ETag");
            if (newEtag.isPresent()) {
                this.lastEtag = newEtag.get();
                try {
                    Files.writeString(etagFile, this.lastEtag, StandardCharsets.UTF_8);
                } catch (IOException ignored) {}
            }

            LOGGER.info("Successfully fetched and updated {} profile(s) from GitHub", data.getProfiles().size());
            if (callback != null) {
                callback.accept("Successfully updated " + data.getProfiles().size() + " profile(s) from GitHub.");
            }
        } catch (Exception e) {
            LOGGER.warn("Error while fetching profiles from {}: {}", urlString, e.getMessage());
            if (callback != null) {
                callback.accept("Failed to refresh profiles: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        stopPeriodicFetching();
        scheduler.shutdown();
    }
}

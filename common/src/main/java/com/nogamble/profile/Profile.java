package com.nogamble.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Represents a single server-specific or global profile with blocked commands and aliases.
 */
public class Profile {
    private String id;
    private String name;
    private List<String> servers = new ArrayList<>();
    private List<String> blockedCommands = new ArrayList<>();
    private List<String> blockedAliases = new ArrayList<>();

    public Profile() {
    }

    public Profile(String id, String name, List<String> servers, List<String> blockedCommands, List<String> blockedAliases) {
        this.id = id;
        this.name = name;
        this.servers = servers != null ? servers : new ArrayList<>();
        this.blockedCommands = blockedCommands != null ? blockedCommands : new ArrayList<>();
        this.blockedAliases = blockedAliases != null ? blockedAliases : new ArrayList<>();
    }

    public String getId() {
        return id != null ? id : "unknown";
    }

    public String getName() {
        return name != null ? name : getId();
    }

    public List<String> getServers() {
        return servers != null ? Collections.unmodifiableList(servers) : Collections.emptyList();
    }

    public List<String> getBlockedCommands() {
        return blockedCommands != null ? Collections.unmodifiableList(blockedCommands) : Collections.emptyList();
    }

    public List<String> getBlockedAliases() {
        return blockedAliases != null ? Collections.unmodifiableList(blockedAliases) : Collections.emptyList();
    }

    /**
     * Determines whether this profile applies to the given server address.
     */
    public boolean matchesServer(String serverAddress) {
        if (servers == null || servers.isEmpty()) {
            return false;
        }

        String host = cleanHost(serverAddress);

        for (String pattern : servers) {
            if (pattern == null) continue;
            String p = pattern.trim().toLowerCase(Locale.ROOT);
            if (p.equals("*")) {
                return true;
            }

            if (host.isEmpty()) {
                continue;
            }

            if (p.startsWith("*.")) {
                String suffix = p.substring(2);
                if (host.equals(suffix) || host.endsWith("." + suffix)) {
                    return true;
                }
            } else if (p.equalsIgnoreCase(host)) {
                return true;
            }
        }

        return false;
    }

    private static String cleanHost(String address) {
        if (address == null) return "";
        String s = address.trim().toLowerCase(Locale.ROOT);
        int colonIdx = s.indexOf(':');
        if (colonIdx != -1) {
            s = s.substring(0, colonIdx);
        }
        return s;
    }
}

package com.nogamble.util;

import java.util.Locale;
import java.util.Set;

/**
 * Utility for parsing and matching outgoing command names.
 */
public class CommandMatcher {

    public static class ParsedCommand {
        private final String rawRoot;
        private final String normalizedRoot;
        private final String normalizedNamespaced;

        public ParsedCommand(String rawRoot, String normalizedRoot, String normalizedNamespaced) {
            this.rawRoot = rawRoot;
            this.normalizedRoot = normalizedRoot;
            this.normalizedNamespaced = normalizedNamespaced;
        }

        public String getRawRoot() {
            return rawRoot;
        }

        public String getNormalizedRoot() {
            return normalizedRoot;
        }

        public String getNormalizedNamespaced() {
            return normalizedNamespaced;
        }

        public boolean matchesAny(Set<String> set) {
            if (set == null || set.isEmpty()) {
                return false;
            }
            return set.contains(normalizedRoot) || set.contains(normalizedNamespaced);
        }
    }

    /**
     * Parses a command string into root and namespaced variants.
     */
    public static ParsedCommand parse(String command) {
        if (command == null) {
            return new ParsedCommand("", "", "");
        }

        String s = command.trim();
        while (s.startsWith("/")) {
            s = s.substring(1).trim();
        }

        if (s.isEmpty()) {
            return new ParsedCommand("", "", "");
        }

        // Get the first token (command name before arguments)
        int spaceIdx = -1;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                spaceIdx = i;
                break;
            }
        }

        String firstToken = spaceIdx == -1 ? s : s.substring(0, spaceIdx);
        String normalizedFull = firstToken.toLowerCase(Locale.ROOT);

        int colonIdx = normalizedFull.indexOf(':');
        String normalizedRoot = colonIdx == -1 ? normalizedFull : normalizedFull.substring(colonIdx + 1);

        return new ParsedCommand(firstToken, normalizedRoot, normalizedFull);
    }
}

package com.searchengine.crawler;

import com.searchengine.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parser for robots.txt files
 * Determines which URLs are allowed to be crawled
 */
public class RobotsTxtParser {
    private static final Logger logger = LoggerFactory.getLogger(RobotsTxtParser.class);

    private final String userAgent;
    private final boolean respectRobotsTxt;
    private final Map<String, RobotRules> rulesCache;

    public RobotsTxtParser() {
        this.userAgent = ConfigLoader.getString("crawler.user.agent", "DesktopSearchBot/1.0");
        this.respectRobotsTxt = ConfigLoader.getBoolean("crawler.respect.robots.txt", true);
        this.rulesCache = new ConcurrentHashMap<>();
    }

    /**
     * Check if a URL is allowed to be crawled
     */
    public boolean isAllowed(String url) {
        if (!respectRobotsTxt) {
            return true;
        }

        try {
            URI uri = new URI(url);
            String baseUrl = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                baseUrl += ":" + uri.getPort();
            }

            // Get or fetch rules for this domain
            RobotRules rules = rulesCache.computeIfAbsent(baseUrl, this::fetchRobotsTxt);

            // Check if path is allowed
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            return rules.isAllowed(path);

        } catch (Exception e) {
            logger.debug("Error checking robots.txt for URL: {}", url, e);
            // If we can't check, allow it
            return true;
        }
    }

    /**
     * Fetch and parse robots.txt for a domain
     */
    private RobotRules fetchRobotsTxt(String baseUrl) {
        String robotsTxtUrl = baseUrl + "/robots.txt";
        logger.debug("Fetching robots.txt from: {}", robotsTxtUrl);

        try {
            URL url = new URL(robotsTxtUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", userAgent);

            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                // Parse robots.txt
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
                return parseRobotsTxt(lines);

            } else {
                // No robots.txt or error - allow all
                logger.debug("No robots.txt found at {} (status: {})", robotsTxtUrl, responseCode);
                return new RobotRules(true);
            }

        } catch (Exception e) {
            logger.debug("Failed to fetch robots.txt from: {}", robotsTxtUrl, e);
            // On error, allow all
            return new RobotRules(true);
        }
    }

    /**
     * Parse robots.txt content
     */
    private RobotRules parseRobotsTxt(List<String> lines) {
        List<String> disallowedPaths = new ArrayList<>();
        boolean relevantSection = false;

        for (String line : lines) {
            line = line.trim();

            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Split by colon
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String directive = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            if (directive.equals("user-agent")) {
                // Check if this section applies to us
                relevantSection = value.equals("*") ||
                                value.toLowerCase().contains(userAgent.toLowerCase().split("/")[0].toLowerCase());
            } else if (relevantSection && directive.equals("disallow")) {
                if (!value.isEmpty()) {
                    disallowedPaths.add(value);
                    logger.debug("Disallowed path: {}", value);
                }
            }
        }

        return new RobotRules(disallowedPaths);
    }

    /**
     * Rules from robots.txt
     */
    private static class RobotRules {
        private final List<String> disallowedPaths;
        private final boolean allowAll;

        public RobotRules(boolean allowAll) {
            this.allowAll = allowAll;
            this.disallowedPaths = new ArrayList<>();
        }

        public RobotRules(List<String> disallowedPaths) {
            this.allowAll = false;
            this.disallowedPaths = disallowedPaths;
        }

        public boolean isAllowed(String path) {
            if (allowAll) {
                return true;
            }

            // Check if path starts with any disallowed path
            for (String disallowed : disallowedPaths) {
                if (path.startsWith(disallowed)) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * Clear the rules cache
     */
    public void clearCache() {
        rulesCache.clear();
        logger.info("Robots.txt cache cleared");
    }
}

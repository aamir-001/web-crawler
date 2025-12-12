package com.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Utility class to normalize URLs to avoid duplicate crawling
 */
public class URLNormalizer {
    private static final Logger logger = LoggerFactory.getLogger(URLNormalizer.class);

    // Pattern for removing URL fragments
    private static final Pattern FRAGMENT_PATTERN = Pattern.compile("#.*$");

    /**
     * Normalize a URL by:
     * - Converting to lowercase (domain part)
     * - Removing fragments (#section)
     * - Removing default ports (80 for http, 443 for https)
     * - Removing trailing slashes
     * - Handling www vs non-www
     */
    public static String normalize(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove leading/trailing whitespace
            url = url.trim();

            // Remove fragment
            url = FRAGMENT_PATTERN.matcher(url).replaceAll("");

            // Parse URL
            URI uri = new URI(url);

            // Get components
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();

            // Validate scheme and host
            if (scheme == null || host == null) {
                logger.debug("Invalid URL (missing scheme or host): {}", url);
                return null;
            }

            // Normalize scheme to lowercase
            scheme = scheme.toLowerCase();

            // Normalize host to lowercase
            host = host.toLowerCase();

            // Remove default ports
            if ((scheme.equals("http") && port == 80) ||
                (scheme.equals("https") && port == 443) ||
                port == -1) {
                port = -1; // Don't include port in normalized URL
            }

            // Normalize path
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            // Remove trailing slash (except for root)
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Rebuild URL
            StringBuilder normalized = new StringBuilder();
            normalized.append(scheme).append("://").append(host);

            if (port != -1) {
                normalized.append(":").append(port);
            }

            normalized.append(path);

            if (query != null && !query.isEmpty()) {
                normalized.append("?").append(query);
            }

            return normalized.toString();

        } catch (URISyntaxException e) {
            logger.debug("Failed to normalize URL: {}", url, e);
            return null;
        }
    }

    /**
     * Resolve a relative URL against a base URL
     */
    public static String resolve(String baseUrl, String relativeUrl) {
        if (relativeUrl == null || relativeUrl.trim().isEmpty()) {
            return null;
        }

        try {
            URI baseUri = new URI(baseUrl);
            URI resolvedUri = baseUri.resolve(relativeUrl);
            return normalize(resolvedUri.toString());
        } catch (URISyntaxException e) {
            logger.debug("Failed to resolve URL: base={}, relative={}", baseUrl, relativeUrl, e);
            return null;
        }
    }

    /**
     * Check if two URLs are equivalent after normalization
     */
    public static boolean areEquivalent(String url1, String url2) {
        String normalized1 = normalize(url1);
        String normalized2 = normalize(url2);

        if (normalized1 == null || normalized2 == null) {
            return false;
        }

        return normalized1.equals(normalized2);
    }

    /**
     * Get the domain from a URL
     */
    public static String getDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : null;
        } catch (URISyntaxException e) {
            logger.debug("Failed to extract domain from URL: {}", url, e);
            return null;
        }
    }

    /**
     * Check if URL belongs to the same domain as base URL
     */
    public static boolean isSameDomain(String baseUrl, String url) {
        String baseDomain = getDomain(baseUrl);
        String urlDomain = getDomain(url);

        if (baseDomain == null || urlDomain == null) {
            return false;
        }

        return baseDomain.equals(urlDomain);
    }
}

package com.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Utility class to validate URLs
 */
public class URLValidator {
    private static final Logger logger = LoggerFactory.getLogger(URLValidator.class);

    // Pattern for valid URL schemes
    private static final Pattern VALID_SCHEME = Pattern.compile("^https?$", Pattern.CASE_INSENSITIVE);

    // Patterns for URLs to exclude
    private static final Pattern[] EXCLUDE_PATTERNS = {
            Pattern.compile("\\.(jpg|jpeg|png|gif|bmp|svg|ico|webp)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.(pdf|doc|docx|xls|xlsx|ppt|pptx)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.(zip|rar|tar|gz|7z)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.(mp3|mp4|avi|mov|wmv|flv|wav)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.(exe|dmg|pkg|deb|rpm)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^mailto:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^tel:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^ftp:", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Check if a URL is valid for crawling
     */
    public static boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = new URI(url);

            // Check if scheme is http or https
            String scheme = uri.getScheme();
            if (scheme == null || !VALID_SCHEME.matcher(scheme).matches()) {
                logger.debug("Invalid scheme for URL: {}", url);
                return false;
            }

            // Check if host exists
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                logger.debug("Missing host for URL: {}", url);
                return false;
            }

            // Check against exclude patterns
            for (Pattern pattern : EXCLUDE_PATTERNS) {
                if (pattern.matcher(url).find()) {
                    logger.debug("URL excluded by pattern: {}", url);
                    return false;
                }
            }

            return true;

        } catch (URISyntaxException e) {
            logger.debug("Invalid URL syntax: {}", url, e);
            return false;
        }
    }

    /**
     * Check if URL is an HTTP or HTTPS URL
     */
    public static boolean isHttpUrl(String url) {
        if (url == null) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && VALID_SCHEME.matcher(scheme).matches();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Check if URL points to a media file
     */
    public static boolean isMediaFile(String url) {
        if (url == null) {
            return false;
        }

        return url.matches(".*\\.(jpg|jpeg|png|gif|bmp|svg|ico|webp|pdf|mp3|mp4|avi|mov|wmv)$");
    }

    /**
     * Check if URL is too long (some web servers have limits)
     */
    public static boolean isTooLong(String url) {
        return url != null && url.length() > 2048;
    }

    /**
     * Comprehensive validation with detailed error message
     */
    public static ValidationResult validateWithReason(String url) {
        if (url == null || url.trim().isEmpty()) {
            return new ValidationResult(false, "URL is null or empty");
        }

        if (isTooLong(url)) {
            return new ValidationResult(false, "URL is too long (> 2048 characters)");
        }

        try {
            URI uri = new URI(url);

            String scheme = uri.getScheme();
            if (scheme == null || !VALID_SCHEME.matcher(scheme).matches()) {
                return new ValidationResult(false, "Invalid scheme: " + scheme);
            }

            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return new ValidationResult(false, "Missing host");
            }

            for (Pattern pattern : EXCLUDE_PATTERNS) {
                if (pattern.matcher(url).find()) {
                    return new ValidationResult(false, "Matches exclusion pattern: " + pattern.pattern());
                }
            }

            return new ValidationResult(true, "Valid");

        } catch (URISyntaxException e) {
            return new ValidationResult(false, "Invalid URL syntax: " + e.getMessage());
        }
    }

    /**
     * Result of URL validation with reason
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        public ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + valid + ", reason='" + reason + "'}";
        }
    }
}

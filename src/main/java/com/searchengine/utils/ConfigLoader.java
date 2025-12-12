package com.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to load configuration from config.properties
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;

    static {
        loadProperties();
    }

    /**
     * Load properties from config file
     */
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                logger.warn("Unable to find {}, using defaults", CONFIG_FILE);
                loadDefaults();
                return;
            }

            properties.load(input);
            logger.info("Configuration loaded successfully from {}", CONFIG_FILE);

        } catch (IOException e) {
            logger.error("Error loading configuration file", e);
            loadDefaults();
        }
    }

    /**
     * Load default configuration values
     */
    private static void loadDefaults() {
        properties.setProperty("crawler.default.depth", "3");
        properties.setProperty("crawler.thread.pool.size", "10");
        properties.setProperty("crawler.request.timeout", "30000");
        properties.setProperty("crawler.max.retries", "3");
        properties.setProperty("crawler.delay.between.requests", "1000");
        properties.setProperty("crawler.user.agent", "DesktopSearchBot/1.0");
        properties.setProperty("crawler.respect.robots.txt", "true");
        properties.setProperty("crawler.max.pages", "500");

        properties.setProperty("database.path", "data/search_engine.db");
        properties.setProperty("database.connection.pool.size", "5");

        properties.setProperty("indexer.enable.stemming", "true");
        properties.setProperty("indexer.remove.stop.words", "true");
        properties.setProperty("indexer.min.word.length", "3");
        properties.setProperty("indexer.max.word.length", "50");

        properties.setProperty("search.max.results", "50");
        properties.setProperty("search.snippet.length", "200");

        properties.setProperty("gui.window.width", "1200");
        properties.setProperty("gui.window.height", "800");
        properties.setProperty("gui.theme", "light");

        logger.info("Using default configuration values");
    }

    /**
     * Get string property
     */
    public static String getString(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get string property with default value
     */
    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get integer property
     */
    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    /**
     * Get integer property with default value
     */
    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get boolean property
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    /**
     * Get boolean property with default value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Get long property
     */
    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    /**
     * Get long property with default value
     */
    public static long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(properties.getProperty(key));
        } catch (NumberFormatException e) {
            logger.warn("Invalid long value for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Reload configuration from file
     */
    public static void reload() {
        logger.info("Reloading configuration");
        loadProperties();
    }
}

package com.searchengine;

import com.searchengine.crawler.WebCrawler;
import com.searchengine.database.DatabaseManager;
import com.searchengine.utils.ConfigLoader;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Desktop Search Engine application
 *
 * NOTE: This class is for Phase 5 (GUI implementation) and currently has compile errors
 * because JavaFX dependencies are not yet added to pom.xml.
 * For now, use CrawlerDemo.java to test the crawler functionality.
 *
 * TODO Phase 5: Add JavaFX dependencies to pom.xml and implement GUI
 */
public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private DatabaseManager dbManager;
    private WebCrawler webCrawler;

    public static void main(String[] args) {
        logger.info("Starting Desktop Search Engine...");
        launch(args);
    }

    @Override
    public void init() throws Exception {
        logger.info("Initializing application...");

        // Initialize database
        String dbPath = ConfigLoader.getString("database.path", "data/search_engine.db");
        int poolSize = ConfigLoader.getInt("database.connection.pool.size", 5);

        // Ensure data directory exists
        java.io.File dataDir = new java.io.File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
            logger.info("Created data directory");
        }

        // Ensure logs directory exists
        java.io.File logsDir = new java.io.File("logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
            logger.info("Created logs directory");
        }

        dbManager = DatabaseManager.getInstance(dbPath, poolSize);
        logger.info("Database initialized");

        // Initialize web crawler
        webCrawler = new WebCrawler(dbManager);
        logger.info("Web crawler initialized");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting JavaFX application...");

        primaryStage.setTitle("Desktop Search Engine");

        // Set window size from config
        int width = ConfigLoader.getInt("gui.window.width", 1200);
        int height = ConfigLoader.getInt("gui.window.height", 800);

        primaryStage.setWidth(width);
        primaryStage.setHeight(height);

        // TODO: Create and set the main GUI scene
        // This will be implemented in Phase 5
        logger.warn("GUI not yet implemented. Running in console mode for testing.");

        // For now, just test the crawler with a simple example
        testCrawler();

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down application...");

        // Stop crawler if running
        if (webCrawler != null && webCrawler.isRunning()) {
            webCrawler.stopCrawl();
        }

        // Shutdown database
        if (dbManager != null) {
            dbManager.shutdown();
        }

        logger.info("Application shutdown complete");
    }

    /**
     * Test method to verify crawler functionality
     * This will be removed once GUI is implemented
     */
    private void testCrawler() {
        logger.info("=== Testing Crawler ===");
        logger.info("To test the crawler, you can:");
        logger.info("1. Uncomment the test code below");
        logger.info("2. Or wait for Phase 5 GUI implementation");
        logger.info("========================");

        /*
        // Example: Crawl a test website
        webCrawler.setProgressListener(new WebCrawler.CrawlerProgressListener() {
            @Override
            public void onCrawlStarted(String startUrl, int maxDepth) {
                logger.info("Crawl started: {} (depth: {})", startUrl, maxDepth);
            }

            @Override
            public void onPageCrawlStart(String url, int depth) {
                logger.info("Crawling: {}", url);
            }

            @Override
            public void onPageCrawlSuccess(String url, int depth, Long pageId, int totalCrawled) {
                logger.info("Success: {} (total: {})", url, totalCrawled);
            }

            @Override
            public void onPageCrawlError(String url, int depth, Exception e) {
                logger.error("Error crawling: {}", url, e);
            }

            @Override
            public void onPageCrawlSkipped(String url, String reason) {
                logger.info("Skipped: {} ({})", url, reason);
            }

            @Override
            public void onCrawlCompleted(int totalPages) {
                logger.info("Crawl completed! Total pages: {}", totalPages);
            }

            @Override
            public void onCrawlStopped(int totalPages) {
                logger.info("Crawl stopped. Total pages: {}", totalPages);
            }
        });

        // Start crawling in a separate thread
        new Thread(() -> {
            webCrawler.startCrawl("https://example.com", 2);
        }).start();
        */
    }
}

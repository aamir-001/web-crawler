package com.searchengine;

import com.searchengine.crawler.WebCrawler;
import com.searchengine.database.DatabaseManager;
import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Simple demo to test the crawler functionality
 * Run this to see the crawler in action!
 */
public class CrawlerDemo {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerDemo.class);

    public static void main(String[] args) throws Exception {
        logger.info("=== Desktop Search Engine - Crawler Demo ===");

        // Create data directory if it doesn't exist
        new java.io.File("data").mkdirs();
        new java.io.File("logs").mkdirs();

        // Initialize database
        logger.info("Initializing database...");
        DatabaseManager dbManager = DatabaseManager.getInstance("data/demo.db", 5);

        // Clear previous data (optional - comment out to keep previous crawls)
        dbManager.clearAllData();
        logger.info("Database cleared and ready");

        // Create crawler
        WebCrawler crawler = new WebCrawler(dbManager);

        // Set up progress listener to see what's happening
        crawler.setProgressListener(new WebCrawler.CrawlerProgressListener() {
            @Override
            public void onCrawlStarted(String startUrl, int maxDepth) {
                System.out.println("\n🚀 CRAWL STARTED");
                System.out.println("   URL: " + startUrl);
                System.out.println("   Max Depth: " + maxDepth);
                System.out.println("   Thread Pool: 10 threads");
                System.out.println("\n" + "=".repeat(60));
            }

            @Override
            public void onPageCrawlStart(String url, int depth) {
                System.out.println("⏳ Crawling [depth " + depth + "]: " + url);
            }

            @Override
            public void onPageCrawlSuccess(String url, int depth, Long pageId, int totalCrawled) {
                System.out.println("✅ SUCCESS [" + totalCrawled + " pages]: " + url);
                System.out.println("   Page ID: " + pageId + " | Depth: " + depth);
            }

            @Override
            public void onPageCrawlError(String url, int depth, Exception e) {
                System.out.println("❌ ERROR: " + url);
                System.out.println("   Reason: " + e.getMessage());
            }

            @Override
            public void onPageCrawlSkipped(String url, String reason) {
                System.out.println("⏭️  SKIPPED: " + url);
                System.out.println("   Reason: " + reason);
            }

            @Override
            public void onCrawlCompleted(int totalPages) {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("🎉 CRAWL COMPLETED!");
                System.out.println("   Total pages crawled: " + totalPages);
                System.out.println("=".repeat(60) + "\n");

                // Display results
                displayResults(dbManager);

                // Shutdown
                dbManager.shutdown();
                System.exit(0);
            }

            @Override
            public void onCrawlStopped(int totalPages) {
                System.out.println("\n⏹️  CRAWL STOPPED");
                System.out.println("   Total pages crawled: " + totalPages);

                // Display results
                displayResults(dbManager);

                // Shutdown
                dbManager.shutdown();
                System.exit(0);
            }
        });

        // Start crawling
        // Try these URLs (pick one):

        // Option 1: Simple test site (recommended)
        String startUrl = "https://example.com";

        // Option 2: Quotes scraping practice site
        // String startUrl = "https://quotes.toscrape.com";

        // Option 3: Books scraping practice site
        // String startUrl = "https://books.toscrape.com";

        int maxDepth = 2;

        System.out.println("\n🔧 Configuration:");
        System.out.println("   Start URL: " + startUrl);
        System.out.println("   Max Depth: " + maxDepth);
        System.out.println("   Max Pages: 500 (configurable in config.properties)");
        System.out.println("   Delay Between Requests: 1000ms");
        System.out.println("   Respect robots.txt: true");
        System.out.println();

        // Start crawling in a separate thread
        new Thread(() -> {
            try {
                crawler.startCrawl(startUrl, maxDepth);
            } catch (Exception e) {
                logger.error("Error during crawl", e);
                dbManager.shutdown();
                System.exit(1);
            }
        }).start();

        // Keep main thread alive
        Thread.currentThread().join();
    }

    /**
     * Display the crawled results
     */
    private static void displayResults(DatabaseManager dbManager) {
        try {
            PageDAO pageDAO = new PageDAO(dbManager);
            List<Page> pages = pageDAO.getAllPages();

            System.out.println("\n📊 CRAWLED PAGES:");
            System.out.println("=".repeat(80));

            if (pages.isEmpty()) {
                System.out.println("   No pages crawled.");
            } else {
                for (int i = 0; i < pages.size(); i++) {
                    Page page = pages.get(i);
                    System.out.println("\n" + (i + 1) + ". " + page.getTitle());
                    System.out.println("   URL: " + page.getUrl());
                    System.out.println("   Depth: " + page.getDepth());
                    System.out.println("   Content Length: " +
                        (page.getContent() != null ? page.getContent().length() : 0) + " chars");

                    // Show first 100 chars of content
                    if (page.getContent() != null && page.getContent().length() > 0) {
                        String preview = page.getContent().substring(0,
                            Math.min(100, page.getContent().length()));
                        System.out.println("   Preview: " + preview + "...");
                    }
                }
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("💾 Database saved at: data/demo.db");
            System.out.println("📝 Logs saved at: logs/application.log");
            System.out.println("\n💡 TIP: You can inspect the database with:");
            System.out.println("   sqlite3 data/demo.db");
            System.out.println("   > SELECT url, title, depth FROM pages;");

        } catch (Exception e) {
            logger.error("Error displaying results", e);
        }
    }
}

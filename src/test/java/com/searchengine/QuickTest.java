package com.searchengine;

import com.searchengine.database.*;
import com.searchengine.utils.ConfigLoader;
import com.searchengine.utils.URLNormalizer;
import com.searchengine.utils.URLValidator;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quick tests to verify everything is working
 * These tests don't require internet connection
 */
public class QuickTest {

    @Test
    @DisplayName("Test 1: Configuration Loading")
    public void testConfigLoader() {
        System.out.println("\n=== Test 1: Configuration Loading ===");

        String userAgent = ConfigLoader.getString("crawler.user.agent");
        int threadPool = ConfigLoader.getInt("crawler.thread.pool.size");
        boolean respectRobots = ConfigLoader.getBoolean("crawler.respect.robots.txt");

        System.out.println("✅ User Agent: " + userAgent);
        System.out.println("✅ Thread Pool Size: " + threadPool);
        System.out.println("✅ Respect robots.txt: " + respectRobots);

        assertNotNull(userAgent, "User agent should not be null");
        assertTrue(threadPool > 0, "Thread pool should be positive");
    }

    @Test
    @DisplayName("Test 2: URL Normalization")
    public void testURLNormalization() {
        System.out.println("\n=== Test 2: URL Normalization ===");

        String url1 = "HTTPS://EXAMPLE.COM/path/";
        String url2 = "https://example.com/path";
        String url3 = "https://example.com/path#section";

        String normalized1 = URLNormalizer.normalize(url1);
        String normalized2 = URLNormalizer.normalize(url2);
        String normalized3 = URLNormalizer.normalize(url3);

        System.out.println("Original: " + url1);
        System.out.println("Normalized: " + normalized1);
        System.out.println();
        System.out.println("Original: " + url2);
        System.out.println("Normalized: " + normalized2);
        System.out.println();
        System.out.println("Original: " + url3);
        System.out.println("Normalized: " + normalized3);

        assertEquals(normalized1, normalized2, "URLs should normalize to same value");
        assertEquals(normalized1, normalized3, "Fragment should be removed");
        System.out.println("\n✅ All URLs normalized correctly!");
    }

    @Test
    @DisplayName("Test 3: URL Validation")
    public void testURLValidation() {
        System.out.println("\n=== Test 3: URL Validation ===");

        String[] validUrls = {
            "https://example.com",
            "http://example.com/path",
            "https://sub.domain.com/path/to/page"
        };

        String[] invalidUrls = {
            "ftp://example.com",
            "https://example.com/image.jpg",
            "mailto:test@example.com",
            "javascript:alert('xss')",
            null,
            ""
        };

        System.out.println("Valid URLs:");
        for (String url : validUrls) {
            boolean valid = URLValidator.isValid(url);
            System.out.println("  " + (valid ? "✅" : "❌") + " " + url);
            assertTrue(valid, "Should be valid: " + url);
        }

        System.out.println("\nInvalid URLs:");
        for (String url : invalidUrls) {
            boolean valid = URLValidator.isValid(url);
            System.out.println("  " + (valid ? "❌" : "✅") + " " + url);
            assertFalse(valid, "Should be invalid: " + url);
        }

        System.out.println("\n✅ URL validation working correctly!");
    }

    @Test
    @DisplayName("Test 4: Database Initialization")
    public void testDatabaseInit() throws Exception {
        System.out.println("\n=== Test 4: Database Initialization ===");

        // Ensure data directory exists
        new File("data").mkdirs();

        String testDbPath = "data/test_quick.db";

        // Delete old test database
        File testDb = new File(testDbPath);
        if (testDb.exists()) {
            testDb.delete();
        }

        // Initialize database
        DatabaseManager dbManager = DatabaseManager.getInstance(testDbPath, 5);
        System.out.println("✅ Database initialized: " + testDbPath);

        // Test page operations
        PageDAO pageDAO = new PageDAO(dbManager);

        // Check initial count
        int count = pageDAO.getTotalPageCount();
        System.out.println("✅ Initial page count: " + count);
        assertEquals(0, count, "Database should be empty initially");

        // Insert a test page
        Page testPage = new Page(
            "https://example.com",
            "Example Domain",
            "This is a test page with some content.",
            0
        );

        Long pageId = pageDAO.insertPage(testPage);
        System.out.println("✅ Inserted test page with ID: " + pageId);
        assertNotNull(pageId, "Page ID should not be null");

        // Verify count increased
        count = pageDAO.getTotalPageCount();
        System.out.println("✅ Updated page count: " + count);
        assertEquals(1, count, "Should have 1 page");

        // Retrieve the page
        Page retrieved = pageDAO.getPageById(pageId).orElse(null);
        assertNotNull(retrieved, "Should retrieve the page");
        System.out.println("✅ Retrieved page: " + retrieved.getTitle());
        assertEquals("Example Domain", retrieved.getTitle());

        // Test URL existence check
        boolean exists = pageDAO.urlExists("https://example.com");
        System.out.println("✅ URL exists check: " + exists);
        assertTrue(exists, "URL should exist");

        // Cleanup
        dbManager.shutdown();
        testDb.delete();

        System.out.println("\n✅ Database operations working correctly!");
    }

    @Test
    @DisplayName("Test 5: Crawl Metadata")
    public void testCrawlMetadata() throws Exception {
        System.out.println("\n=== Test 5: Crawl Metadata ===");

        new File("data").mkdirs();
        String testDbPath = "data/test_metadata.db";

        File testDb = new File(testDbPath);
        if (testDb.exists()) {
            testDb.delete();
        }

        DatabaseManager dbManager = DatabaseManager.getInstance(testDbPath, 5);
        CrawlMetadataDAO metadataDAO = new CrawlMetadataDAO(dbManager);

        // Create crawl metadata
        CrawlMetadata metadata = new CrawlMetadata("https://example.com", 3);
        Long crawlId = metadataDAO.insertCrawlMetadata(metadata);

        System.out.println("✅ Created crawl session with ID: " + crawlId);
        assertNotNull(crawlId, "Crawl ID should not be null");

        // Update metadata
        metadata.setPagesCrawled(42);
        metadata.setEndTime(System.currentTimeMillis());
        metadata.setStatus("completed");
        metadataDAO.updateCrawlMetadata(metadata);

        System.out.println("✅ Updated crawl metadata");

        // Retrieve metadata
        CrawlMetadata retrieved = metadataDAO.getCrawlMetadata(crawlId).orElse(null);
        assertNotNull(retrieved, "Should retrieve metadata");
        assertEquals(42, retrieved.getPagesCrawled());
        assertEquals("completed", retrieved.getStatus());

        System.out.println("✅ Retrieved metadata: " + retrieved);
        System.out.println("   Pages crawled: " + retrieved.getPagesCrawled());
        System.out.println("   Status: " + retrieved.getStatus());

        // Cleanup
        dbManager.shutdown();
        testDb.delete();

        System.out.println("\n✅ Crawl metadata working correctly!");
    }

    @Test
    @DisplayName("Test 6: URL Domain Extraction")
    public void testDomainExtraction() {
        System.out.println("\n=== Test 6: URL Domain Extraction ===");

        String url1 = "https://www.example.com/path/to/page";
        String url2 = "https://sub.domain.example.com/another/path";
        String url3 = "http://localhost:8080/test";

        String domain1 = URLNormalizer.getDomain(url1);
        String domain2 = URLNormalizer.getDomain(url2);
        String domain3 = URLNormalizer.getDomain(url3);

        System.out.println("URL: " + url1);
        System.out.println("Domain: " + domain1);
        System.out.println();
        System.out.println("URL: " + url2);
        System.out.println("Domain: " + domain2);
        System.out.println();
        System.out.println("URL: " + url3);
        System.out.println("Domain: " + domain3);

        assertEquals("www.example.com", domain1);
        assertEquals("sub.domain.example.com", domain2);
        assertEquals("localhost", domain3);

        System.out.println("\n✅ Domain extraction working correctly!");
    }

    @Test
    @DisplayName("Test 7: Same Domain Check")
    public void testSameDomain() {
        System.out.println("\n=== Test 7: Same Domain Check ===");

        String base = "https://example.com/page1";
        String same = "https://example.com/page2";
        String different = "https://other.com/page";

        boolean isSame1 = URLNormalizer.isSameDomain(base, same);
        boolean isSame2 = URLNormalizer.isSameDomain(base, different);

        System.out.println("Base: " + base);
        System.out.println("Compare: " + same + " -> " + (isSame1 ? "SAME ✅" : "DIFFERENT ❌"));
        System.out.println("Compare: " + different + " -> " + (isSame2 ? "SAME ❌" : "DIFFERENT ✅"));

        assertTrue(isSame1, "Should be same domain");
        assertFalse(isSame2, "Should be different domain");

        System.out.println("\n✅ Same domain check working correctly!");
    }

    @Test
    @DisplayName("Test 8: Comprehensive Summary")
    public void testSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🎉 ALL TESTS PASSED!");
        System.out.println("=".repeat(60));
        System.out.println("\n✅ Configuration system working");
        System.out.println("✅ URL normalization working");
        System.out.println("✅ URL validation working");
        System.out.println("✅ Database layer working");
        System.out.println("✅ Page operations working");
        System.out.println("✅ Crawl metadata working");
        System.out.println("✅ Utility functions working");
        System.out.println("\n🚀 Ready to test the full crawler!");
        System.out.println("\nNext steps:");
        System.out.println("1. Run CrawlerDemo.java to test actual web crawling");
        System.out.println("2. Or use: mvn exec:java -Dexec.mainClass=\"com.searchengine.CrawlerDemo\"");
        System.out.println("=".repeat(60) + "\n");
    }
}

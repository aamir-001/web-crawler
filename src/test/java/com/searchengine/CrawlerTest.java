package com.searchengine;

import com.searchengine.crawler.WebCrawler;
import com.searchengine.database.DatabaseManager;
import com.searchengine.database.PageDAO;
import com.searchengine.utils.URLNormalizer;
import com.searchengine.utils.URLValidator;
import org.junit.jupiter.api.*;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for crawler functionality
 */
public class CrawlerTest {

    private static DatabaseManager dbManager;
    private static String testDbPath = "data/test_search_engine.db";

    @BeforeAll
    public static void setupClass() {
        // Ensure data directory exists
        new File("data").mkdirs();

        // Initialize database for testing
        dbManager = DatabaseManager.getInstance(testDbPath, 5);
    }

    @AfterAll
    public static void teardownClass() {
        if (dbManager != null) {
            dbManager.shutdown();
        }

        // Clean up test database
        File testDb = new File(testDbPath);
        if (testDb.exists()) {
            testDb.delete();
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        // Clear database before each test
        dbManager.clearAllData();
    }

    @Test
    public void testURLNormalizer() {
        String url1 = "https://Example.com/path/";
        String url2 = "https://example.com/path";

        String normalized1 = URLNormalizer.normalize(url1);
        String normalized2 = URLNormalizer.normalize(url2);

        assertEquals(normalized1, normalized2, "URLs should be normalized to same value");
    }

    @Test
    public void testURLValidator() {
        assertTrue(URLValidator.isValid("https://example.com"), "Valid HTTPS URL should pass");
        assertTrue(URLValidator.isValid("http://example.com/path"), "Valid HTTP URL should pass");

        assertFalse(URLValidator.isValid("ftp://example.com"), "FTP URL should fail");
        assertFalse(URLValidator.isValid("https://example.com/image.jpg"), "Image URL should fail");
        assertFalse(URLValidator.isValid("mailto:test@example.com"), "Mailto URL should fail");
        assertFalse(URLValidator.isValid(null), "Null URL should fail");
        assertFalse(URLValidator.isValid(""), "Empty URL should fail");
    }

    @Test
    public void testDatabaseConnection() throws Exception {
        PageDAO pageDAO = new PageDAO(dbManager);
        int count = pageDAO.getTotalPageCount();
        assertEquals(0, count, "Database should be empty initially");
    }

    @Test
    public void testWebCrawlerInitialization() {
        WebCrawler crawler = new WebCrawler(dbManager);
        assertNotNull(crawler, "Crawler should be initialized");
        assertFalse(crawler.isRunning(), "Crawler should not be running initially");
        assertEquals(0, crawler.getPagesCrawled(), "No pages should be crawled initially");
    }

    @Test
    public void testInvalidStartURL() {
        WebCrawler crawler = new WebCrawler(dbManager);

        assertThrows(IllegalArgumentException.class, () -> {
            crawler.startCrawl("not-a-valid-url", 2, 100);
        }, "Should throw exception for invalid URL");
    }

    // NOTE: Actual crawling tests would require a mock web server
    // or should be done as integration tests with real websites
}

# Build and Test Guide

## Prerequisites Check

Before building, verify you have the required tools installed:

```bash
# Check Java version (should be 17 or higher)
java -version

# Check Maven version (should be 3.6+)
mvn -version
```

If not installed, see [SETUP_GUIDE.md](SETUP_GUIDE.md) for installation instructions.

---

## Build Steps

### 1. Clean and Compile
```bash
mvn clean compile
```

**Expected output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

### 2. Run Tests
```bash
mvn test
```

**Tests included:**
- URL normalization tests
- URL validation tests
- Database connection tests
- Crawler initialization tests

### 3. Package as JAR
```bash
mvn package
```

**Creates:**
- `target/desktop-search-engine-1.0-SNAPSHOT.jar`
- `target/desktop-search-engine-1.0-SNAPSHOT-jar-with-dependencies.jar` (runnable)

---

## Running the Application

### Option 1: Using Maven
```bash
mvn exec:java -Dexec.mainClass="com.searchengine.Main"
```

### Option 2: Using the JAR
```bash
java -jar target/desktop-search-engine-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Option 3: From IDE
1. Open project in IntelliJ/Eclipse/VS Code
2. Navigate to `src/main/java/com/searchengine/Main.java`
3. Right-click → Run 'Main'

---

## Testing the Crawler

The current implementation has Phase 1 (Database) and Phase 2 (Crawler) complete. To test:

### Method 1: Uncomment Test Code in Main.java

Edit [src/main/java/com/searchengine/Main.java](src/main/java/com/searchengine/Main.java) and uncomment the test code in the `testCrawler()` method (around line 80):

```java
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
```

Then run the application.

### Method 2: Create a Test Class

Create your own test class:

```java
package com.searchengine;

import com.searchengine.crawler.WebCrawler;
import com.searchengine.database.DatabaseManager;

public class CrawlerDemo {
    public static void main(String[] args) {
        // Initialize database
        DatabaseManager dbManager = DatabaseManager.getInstance("data/demo.db", 5);

        // Create crawler
        WebCrawler crawler = new WebCrawler(dbManager);

        // Set up progress listener
        crawler.setProgressListener(new WebCrawler.CrawlerProgressListener() {
            @Override
            public void onCrawlStarted(String startUrl, int maxDepth) {
                System.out.println("Started crawling: " + startUrl);
            }

            @Override
            public void onPageCrawlSuccess(String url, int depth, Long pageId, int totalCrawled) {
                System.out.println("Crawled (" + totalCrawled + "): " + url);
            }

            @Override
            public void onCrawlCompleted(int totalPages) {
                System.out.println("Completed! Total pages: " + totalPages);
                System.exit(0);
            }

            // ... implement other methods
        });

        // Start crawling
        crawler.startCrawl("https://example.com", 2);
    }
}
```

### Method 3: Use Unit Tests

Run the existing unit tests:

```bash
mvn test -Dtest=CrawlerTest
```

---

## Verifying the Database

After running a crawl, you can inspect the SQLite database:

### Using SQLite CLI:
```bash
# Install sqlite3 if needed
# Windows: choco install sqlite
# Mac: brew install sqlite
# Linux: apt install sqlite3

# Open database
sqlite3 data/search_engine.db

# View crawled pages
SELECT page_id, url, title, depth FROM pages;

# View crawl sessions
SELECT * FROM crawl_metadata;

# Count pages
SELECT COUNT(*) FROM pages;

# Exit
.quit
```

### Using a GUI Tool:
- **DB Browser for SQLite** - https://sqlitebrowser.org/
- **DBeaver** - https://dbeaver.io/

---

## Configuration

Edit [src/main/resources/config.properties](src/main/resources/config.properties) to customize:

```properties
# Crawler settings
crawler.default.depth=3
crawler.thread.pool.size=10
crawler.request.timeout=30000
crawler.delay.between.requests=1000
crawler.max.pages=500

# Database
database.path=data/search_engine.db
database.connection.pool.size=5
```

---

## Troubleshooting

### Build Errors

**Error: "package org.jsoup does not exist"**
```bash
# Force dependency download
mvn clean install -U
```

**Error: "release version 17 not supported"**
- Update your JDK to version 17 or higher
- Or change pom.xml compiler settings to match your JDK version

### Runtime Errors

**Error: "Failed to initialize database"**
- Ensure `data/` directory exists
- Check file permissions
- Verify SQLite JDBC driver is in classpath

**Error: "Connection timeout"**
- Increase timeout in config.properties
- Check internet connection
- Try a different website

**Error: "Disallowed by robots.txt"**
- Normal behavior - the site blocks crawlers
- Try a different URL
- Or set `crawler.respect.robots.txt=false` (not recommended)

---

## Log Files

Logs are written to:
- **Console** - Real-time output
- **File** - `logs/application.log`

Log configuration: [src/main/resources/logback.xml](src/main/resources/logback.xml)

To change log level, edit logback.xml:
```xml
<root level="INFO">  <!-- Change to DEBUG for more detail -->
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

---

## Performance Tips

### For Large Crawls:
1. Increase thread pool size (but respect target servers)
2. Increase database connection pool
3. Enable database WAL mode for better concurrency
4. Consider batch inserts for indexing

### For Development:
1. Reduce thread pool to 2-3 for easier debugging
2. Set max pages to 10-20 for quick tests
3. Use DEBUG log level
4. Test with simple websites first (example.com)

---

## Next Steps

Once you've verified Phase 1 & 2 work:

1. **Implement Phase 3 (Indexer)**
   - See [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) for details

2. **Implement Phase 4 (Search Engine)**

3. **Implement Phase 5 (GUI)**

4. **Polish and test everything**

---

## Quick Test Checklist

- [ ] Project compiles without errors (`mvn compile`)
- [ ] Tests pass (`mvn test`)
- [ ] Application starts (`mvn exec:java`)
- [ ] Database is created in `data/` directory
- [ ] Crawler can crawl a test URL
- [ ] Pages are saved to database
- [ ] Logs are written to `logs/` directory
- [ ] Configuration is loaded from config.properties
- [ ] robots.txt is respected
- [ ] URL normalization works
- [ ] Duplicate URLs are detected

---

## Good Test URLs

Start with these simple, crawler-friendly sites:

1. **https://example.com** - Minimal, safe test site
2. **https://quotes.toscrape.com** - Designed for web scraping practice
3. **https://books.toscrape.com** - Another scraping practice site
4. **Your own website** - If you have one

**Avoid:**
- Large sites (Wikipedia, Reddit, etc.) for initial testing
- Sites with aggressive bot protection
- Dynamic JavaScript-heavy sites (crawler gets HTML only)
- Sites that explicitly block crawlers

---

## Summary

The current implementation provides a **fully functional web crawler** with:
- ✅ Multithreading
- ✅ Depth control
- ✅ robots.txt compliance
- ✅ URL normalization
- ✅ Database persistence
- ✅ Progress tracking
- ✅ Configurable settings

You're ready to test and then move on to Phase 3 (Indexer)!

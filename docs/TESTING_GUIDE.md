# Testing Guide - How to Test Phase 1 & 2

## Quick Start (3 Methods)

You have **3 ways** to test the crawler. Pick the one that works best for you:

---

## Method 1: Run Quick Tests (No Internet Required) ⚡

**Best for:** Verifying everything is set up correctly

### Steps:
```bash
# 1. Build the project
mvn clean install

# 2. Run the quick tests
mvn test -Dtest=QuickTest
```

### What This Tests:
- ✅ Configuration loading
- ✅ URL normalization
- ✅ URL validation
- ✅ Database initialization
- ✅ Page CRUD operations
- ✅ Crawl metadata
- ✅ Domain extraction

### Expected Output:
```
=== Test 1: Configuration Loading ===
✅ User Agent: DesktopSearchBot/1.0
✅ Thread Pool Size: 10
✅ Respect robots.txt: true

=== Test 2: URL Normalization ===
...

🎉 ALL TESTS PASSED!
```

**Time:** ~10 seconds
**Internet:** Not required

---

## Method 2: Run Full Crawler Demo (Internet Required) 🚀

**Best for:** Seeing the crawler in action with real websites

### Steps:

**Option A: Using Maven**
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.searchengine.CrawlerDemo"
```

**Option B: Using IDE**
1. Open `src/main/java/com/searchengine/CrawlerDemo.java`
2. Right-click → Run 'CrawlerDemo.main()'

### What This Does:
1. Initializes the database
2. Starts crawling `https://example.com` at depth 2
3. Shows real-time progress:
   ```
   🚀 CRAWL STARTED
      URL: https://example.com
      Max Depth: 2

   ⏳ Crawling [depth 0]: https://example.com
   ✅ SUCCESS [1 pages]: https://example.com
      Page ID: 1 | Depth: 0

   ⏳ Crawling [depth 1]: https://www.iana.org/domains/example
   ✅ SUCCESS [2 pages]: https://www.iana.org/domains/example

   🎉 CRAWL COMPLETED!
      Total pages crawled: 2

   📊 CRAWLED PAGES:
   1. Example Domain
      URL: https://example.com
      Depth: 0
      Content Length: 648 chars
      Preview: Example Domain This domain is for use in illustrative examples...
   ```

4. Saves results to `data/demo.db`
5. Creates logs in `logs/application.log`

### Try Different Websites:

Edit line 60 in `CrawlerDemo.java`:

```java
// Option 1: Simple (recommended)
String startUrl = "https://example.com";

// Option 2: More content
String startUrl = "https://quotes.toscrape.com";

// Option 3: Even more content
String startUrl = "https://books.toscrape.com";
```

**Time:** 10 seconds - 2 minutes (depending on site)
**Internet:** Required

---

## Method 3: Run All Unit Tests 🧪

**Best for:** Comprehensive testing

### Steps:
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CrawlerTest
mvn test -Dtest=QuickTest

# Run with verbose output
mvn test -X
```

### What This Tests:
- URL normalization edge cases
- URL validation rules
- Database operations
- Crawler initialization
- Error handling

**Time:** ~15 seconds
**Internet:** Not required

---

## Verifying Results

### 1. Check the Database

After running the crawler demo:

**Using SQLite CLI:**
```bash
# Install sqlite3 first if needed:
# Windows: choco install sqlite
# Mac: brew install sqlite
# Linux: apt install sqlite3

# Open the database
sqlite3 data/demo.db

# View all pages
SELECT page_id, url, title, depth FROM pages;

# Count pages
SELECT COUNT(*) FROM pages;

# View crawl sessions
SELECT * FROM crawl_metadata;

# Exit
.quit
```

**Using DB Browser for SQLite (GUI):**
1. Download from https://sqlitebrowser.org/
2. Open `data/demo.db`
3. Browse tables visually

### 2. Check the Logs

```bash
# View logs
cat logs/application.log

# Or on Windows
type logs\application.log

# Follow logs in real-time (Linux/Mac)
tail -f logs/application.log
```

### 3. Check Directory Structure

```bash
ls -la data/
ls -la logs/
```

You should see:
- `data/demo.db` (or `search_engine.db`)
- `logs/application.log`

---

## Expected Behavior

### Successful Crawl:
1. ✅ Database file created in `data/`
2. ✅ Log file created in `logs/`
3. ✅ Console shows progress messages
4. ✅ Pages saved in database
5. ✅ Crawl metadata recorded
6. ✅ No exceptions or errors

### Common Success Output:
```
🚀 CRAWL STARTED
⏳ Crawling...
✅ SUCCESS [1 pages]: https://example.com
✅ SUCCESS [2 pages]: https://www.iana.org/domains/example
🎉 CRAWL COMPLETED!
   Total pages crawled: 2

📊 CRAWLED PAGES:
1. Example Domain
   URL: https://example.com
   ...

💾 Database saved at: data/demo.db
📝 Logs saved at: logs/application.log
```

---

## Troubleshooting

### Problem: Maven command not found
**Solution:**
```bash
# Verify Maven is installed
mvn -version

# If not, install it (see SETUP_GUIDE.md)
```

### Problem: "Cannot find symbol" compile errors
**Solution:**
```bash
# Force dependency download
mvn clean install -U

# Or delete .m2 cache and retry
rm -rf ~/.m2/repository
mvn clean install
```

### Problem: "Connection timeout" during crawl
**Solution:**
1. Check internet connection
2. Try a simpler URL (example.com)
3. Increase timeout in `config.properties`:
   ```properties
   crawler.request.timeout=60000
   ```

### Problem: "Disallowed by robots.txt"
**Solution:**
This is normal! The site blocks crawlers. Try:
1. Different URL (example.com usually allows)
2. Or temporarily disable (not recommended):
   ```properties
   crawler.respect.robots.txt=false
   ```

### Problem: No output/logs
**Solution:**
1. Check `data/` and `logs/` directories exist
2. Check file permissions
3. Look for errors in console
4. Try running with `-X` flag: `mvn test -X`

### Problem: Tests fail
**Solution:**
```bash
# Clean and rebuild
mvn clean install

# Run tests with verbose output
mvn test -X

# Check the error message in console
```

---

## Performance Testing

### Test Different Configurations:

Edit `src/main/resources/config.properties`:

```properties
# Test with fewer threads (easier to debug)
crawler.thread.pool.size=2

# Test with more pages
crawler.max.pages=100

# Test with different depth
crawler.default.depth=3

# Test faster crawling (less polite)
crawler.delay.between.requests=500
```

Then run the demo again and compare results.

---

## Integration Testing Example

Want to test with your own code? Here's a minimal example:

```java
import com.searchengine.crawler.WebCrawler;
import com.searchengine.database.DatabaseManager;

public class MyTest {
    public static void main(String[] args) {
        // Setup
        DatabaseManager db = DatabaseManager.getInstance("data/my_test.db", 5);
        WebCrawler crawler = new WebCrawler(db);

        // Configure listener
        crawler.setProgressListener(new WebCrawler.CrawlerProgressListener() {
            public void onCrawlCompleted(int total) {
                System.out.println("Done! Crawled " + total + " pages");
                db.shutdown();
                System.exit(0);
            }
            // ... implement other methods ...
        });

        // Start crawling
        crawler.startCrawl("https://example.com", 1);
    }
}
```

---

## What to Look For (Success Checklist)

When testing, verify:

- [ ] Project compiles without errors
- [ ] Tests pass (all green)
- [ ] Database file is created
- [ ] Pages are saved in database
- [ ] URLs are normalized correctly
- [ ] Duplicate URLs are detected
- [ ] robots.txt is respected
- [ ] Logs are written
- [ ] No memory leaks (for long runs)
- [ ] Thread pool works correctly
- [ ] Crawl can be stopped gracefully
- [ ] Configuration is loaded
- [ ] Politeness delays are applied

---

## Sample Test Session

Here's what a complete test session looks like:

```bash
# 1. Build
$ mvn clean install
[INFO] BUILD SUCCESS

# 2. Run quick tests
$ mvn test -Dtest=QuickTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
🎉 ALL TESTS PASSED!

# 3. Run crawler demo
$ mvn exec:java -Dexec.mainClass="com.searchengine.CrawlerDemo"
🚀 CRAWL STARTED
✅ SUCCESS [1 pages]: https://example.com
✅ SUCCESS [2 pages]: https://www.iana.org/domains/example
🎉 CRAWL COMPLETED!
   Total pages crawled: 2

# 4. Check database
$ sqlite3 data/demo.db
sqlite> SELECT COUNT(*) FROM pages;
2
sqlite> SELECT url FROM pages;
https://example.com
https://www.iana.org/domains/example
sqlite> .quit

# 5. Check logs
$ tail logs/application.log
2024-12-12 10:30:15 INFO  - Crawl completed. Total pages: 2

# ✅ Everything works!
```

---

## Next Steps After Testing

Once you've verified Phase 1 & 2 work:

1. **Understand the code:**
   - Read through the crawler classes
   - Look at how threads are managed
   - See how URLs are processed

2. **Experiment:**
   - Try different websites
   - Adjust configuration
   - Modify depth and max pages
   - Change thread pool size

3. **Move to Phase 3:**
   - Implement the Indexer
   - Use the crawled data
   - Build the inverted index

---

## Quick Reference

| What | Command |
|------|---------|
| Build | `mvn clean install` |
| Quick Tests | `mvn test -Dtest=QuickTest` |
| All Tests | `mvn test` |
| Run Demo | `mvn exec:java -Dexec.mainClass="com.searchengine.CrawlerDemo"` |
| View DB | `sqlite3 data/demo.db` |
| View Logs | `cat logs/application.log` |
| Clean | `mvn clean` |

---

## Support

If you encounter issues:
1. Check this guide first
2. Look at logs: `logs/application.log`
3. Check [BUILD_AND_TEST.md](BUILD_AND_TEST.md)
4. Review [SETUP_GUIDE.md](SETUP_GUIDE.md)

---

**You're ready to test! Start with Method 1 (Quick Tests), then try Method 2 (Full Crawler Demo).** 🚀

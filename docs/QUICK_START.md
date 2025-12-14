# Quick Start - Test in 3 Steps

## Step 1: Build (30 seconds)
```bash
mvn clean install
```

Expected output: `BUILD SUCCESS`

---

## Step 2: Run Quick Tests (10 seconds)
```bash
mvn test -Dtest=QuickTest
```

Expected output: `🎉 ALL TESTS PASSED!`

---

## Step 3: Run Full Crawler Demo (1 minute)
```bash
mvn exec:java -Dexec.mainClass="com.searchengine.CrawlerDemo"
```

Expected output:
```
🚀 CRAWL STARTED
✅ SUCCESS [1 pages]: https://example.com
✅ SUCCESS [2 pages]: https://www.iana.org/domains/example
🎉 CRAWL COMPLETED!
   Total pages crawled: 2
```

---

## Verify Results

```bash
# Check database
sqlite3 data/demo.db
> SELECT url, title FROM pages;
> .quit

# Check logs
cat logs/application.log
```

---

## That's It! ✅

**You now have a working web crawler!**

### What It Does:
- ✅ Crawls websites with multiple threads
- ✅ Respects robots.txt
- ✅ Stores pages in SQLite database
- ✅ Tracks progress in real-time

### Next:
- Read [TESTING_GUIDE.md](TESTING_GUIDE.md) for detailed testing
- See [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md) for next phases
- Check [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) for the big picture

---

## Troubleshooting

**Problem:** `mvn: command not found`
**Fix:** Install Maven (see [SETUP_GUIDE.md](SETUP_GUIDE.md))

**Problem:** Compile errors
**Fix:** `mvn clean install -U`

**Problem:** No internet during demo
**Fix:** Just run quick tests: `mvn test -Dtest=QuickTest`

---

## Files Created

After testing, you'll see:
```
web-crawler/
├── data/
│   └── demo.db           ← Your crawled data!
├── logs/
│   └── application.log   ← Crawler activity logs
└── target/
    └── *.jar             ← Compiled application
```

---

**Ready? Run the 3 steps above!** 🚀

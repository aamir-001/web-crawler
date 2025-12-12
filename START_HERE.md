# START HERE - No Maven Installation Required! 🚀

You don't have Maven installed, so I've created a **Maven Wrapper** for you. This lets you use Maven without installing it!

---

## ✅ What You Need

Only **Java 17** is required. Check if you have it:

```powershell
java -version
```

**Expected output:**
```
java version "17.0.x" or higher
```

**Don't have Java?** Download from https://adoptium.net/

---

## 🎯 Quick Start (3 Steps)

### Step 1: Build the Project (First Time Only)

```powershell
.\mvnw.cmd clean install
```

This will:
- Download Maven automatically (first time only, ~10MB)
- Download all dependencies (JSoup, SQLite, etc.)
- Compile the project
- Run tests

**Time:** 1-2 minutes first time, 30 seconds after that

### Step 2: Run Quick Tests

```powershell
.\mvnw.cmd test -Dtest=QuickTest
```

**Expected output:**
```
=== Test 1: Configuration Loading ===
✅ User Agent: DesktopSearchBot/1.0
...
🎉 ALL TESTS PASSED!
```

### Step 3: Run the Crawler Demo

```powershell
.\mvnw.cmd exec:java -D"exec.mainClass=com.searchengine.CrawlerDemo"
```

**Expected output:**
```
🚀 CRAWL STARTED
✅ SUCCESS [1 pages]: https://example.com
🎉 CRAWL COMPLETED!
```

---

## 🎨 OR Use an IDE (Easier!)

### Option A: IntelliJ IDEA (Recommended)

1. **Download IntelliJ IDEA Community** (free): https://www.jetbrains.com/idea/download/
2. **Open the project:**
   - File → Open
   - Select `D:\web-crawler` folder
   - Click OK
3. **Wait for indexing** (bottom right progress bar)
4. **Run tests:**
   - Navigate to `src/test/java/com/searchengine/QuickTest.java`
   - Right-click → Run 'QuickTest'
5. **Run crawler:**
   - Navigate to `src/main/java/com/searchengine/CrawlerDemo.java`
   - Right-click → Run 'CrawlerDemo.main()'

IntelliJ handles everything automatically - no Maven needed!

### Option B: VS Code

1. **Install VS Code**: https://code.visualstudio.com/
2. **Install Extension Pack for Java** from Extensions marketplace
3. **Open folder:** File → Open Folder → Select `D:\web-crawler`
4. **Run:**
   - Click Testing icon (beaker) in left sidebar
   - Run tests from there
   - Or press F5 to run CrawlerDemo

---

## 📝 All Available Commands

Once you've run Step 1, you can use these commands:

```powershell
# Build and compile
.\mvnw.cmd clean compile

# Run all tests
.\mvnw.cmd test

# Run specific test
.\mvnw.cmd test -Dtest=QuickTest

# Run crawler demo
.\mvnw.cmd exec:java -D"exec.mainClass=com.searchengine.CrawlerDemo"

# Package as JAR
.\mvnw.cmd package

# Clean build artifacts
.\mvnw.cmd clean
```

---

## 🐛 Troubleshooting

### "java: command not found" or Java version error

**Fix:** Install Java 17 from https://adoptium.net/

After installing:
1. Close and reopen PowerShell
2. Verify: `java -version`

### "Failed to download maven-wrapper.jar"

**Fix:** Check internet connection, then:
```powershell
# Download manually
Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -OutFile ".mvn\wrapper\maven-wrapper.jar"

# Then retry
.\mvnw.cmd clean install
```

### PowerShell execution policy error

**Fix:**
```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

### Download is slow

This is normal for the first run. Maven is downloading:
- Itself (~10MB)
- All dependencies (~50MB total)

Subsequent runs will be fast!

---

## ✅ Verification

After Step 1 completes successfully, you should see:

```
D:\web-crawler\
├── .mvn\
│   └── wrapper\
│       ├── maven-wrapper.jar       ← Maven wrapper
│       └── maven-wrapper.properties
├── target\                          ← Compiled code
│   └── classes\
│       └── com\searchengine\        ← Your compiled classes
└── ...
```

---

## 🎯 Recommended Path

**For beginners:**
1. Use IntelliJ IDEA (easiest, most features)
2. Open the project
3. Run tests and demos from IDE

**For command line users:**
1. Run `.\mvnw.cmd clean install`
2. Run `.\mvnw.cmd test -Dtest=QuickTest`
3. Run `.\mvnw.cmd exec:java -D"exec.mainClass=com.searchengine.CrawlerDemo"`

**Both work great!** Pick what you're comfortable with.

---

## 📚 Next Steps

After you get it running:

1. **Understand what we built:**
   - Read [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)

2. **See detailed testing:**
   - Read [TESTING_GUIDE.md](TESTING_GUIDE.md)

3. **Check architecture:**
   - Read [ARCHITECTURE.md](ARCHITECTURE.md)

4. **Ready to implement next phase:**
   - See [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)

---

## 💡 Quick Tips

- **mvnw.cmd** = Maven Wrapper (use this instead of `mvn`)
- **First run** = Slow (downloads everything)
- **Later runs** = Fast (uses cache)
- **IntelliJ** = Easiest for development
- **PowerShell** = Use `.\mvnw.cmd` (not just `mvnw.cmd`)

---

## ❓ Still Stuck?

Check these in order:

1. ✅ Java 17 installed? → `java -version`
2. ✅ In correct directory? → `cd D:\web-crawler`
3. ✅ Internet working? → Needed for first run
4. ✅ Antivirus blocking? → Temporarily disable

If all else fails, use IntelliJ IDEA - it handles everything!

---

**You're ready! Run this now:**

```powershell
.\mvnw.cmd clean install
```

Then follow Steps 2 and 3 above! 🚀

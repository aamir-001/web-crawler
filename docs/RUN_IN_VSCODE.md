# How to Run in VS Code (Easiest Method!)

You already have VS Code with Java extensions installed. Here's how to use it:

---

## Method 1: Run Tests Directly ✅ (Recommended)

1. **Open the Testing panel:**
   - Click the **Testing icon** (🧪 beaker) in the left sidebar
   - OR press `Ctrl+Shift+T`

2. **You should see:**
   ```
   > QuickTest
     ✓ Test 1: Configuration Loading
     ✓ Test 2: URL Normalization
     ✓ Test 3: URL Validation
     ...
   > CrawlerTest
   ```

3. **Click the ▶️ play button** next to `QuickTest`

4. **See results** in the Test Results panel

---

## Method 2: Run CrawlerDemo Directly ✅

1. **In Explorer (left sidebar):**
   - Navigate to: `src/main/java/com/searchengine/CrawlerDemo.java`

2. **Right-click on `CrawlerDemo.java`**

3. **Select "Run Java"**

4. **See output** in the terminal below

---

## Method 3: Use Maven Panel 🔧

1. **Open Command Palette:**
   - Press `Ctrl+Shift+P`
   - Type: "Java: Configure Java Runtime"
   - Verify Java 17 is detected

2. **Look for Maven icon** in the left sidebar (or open Explorer)

3. **You should see "MAVEN" section** at the bottom of Explorer

4. **Expand your project** → Click on:
   - `clean` → then `install`
   - OR `test` to run tests

---

## Method 4: Terminal with Full Java Path 🔧

Since the wrapper isn't working, use Java directly:

```powershell
# Download Maven manually (one-time)
# We'll skip Maven and use VS Code's built-in build system instead!

# Just run the main class directly:
java -cp "target/classes;%USERPROFILE%\.m2\repository\*" com.searchengine.CrawlerDemo
```

But **Method 1 and 2 above are much easier!**

---

## 🎯 Try This Right Now:

1. Press `Ctrl+Shift+P`
2. Type: "Java: Clean Java Language Server Workspace"
3. Click it and select "Reload and delete"
4. VS Code will restart and rebuild everything
5. Then try Method 1 or Method 2 above

---

## If Tests Don't Appear:

VS Code needs to build the project first. Do this:

1. Open any Java file (like `CrawlerDemo.java`)
2. **Wait** for VS Code to finish "Importing projects..." (see bottom right)
3. Once done, the Testing panel will populate automatically

---

## 🚀 Expected Output:

When you run `QuickTest`, you'll see:

```
=== Test 1: Configuration Loading ===
✅ User Agent: DesktopSearchBot/1.0
✅ Thread Pool Size: 10
...

🎉 ALL TESTS PASSED!
```

When you run `CrawlerDemo`, you'll see:

```
🚀 CRAWL STARTED
⏳ Crawling [depth 0]: https://example.com
✅ SUCCESS [1 pages]: https://example.com
🎉 CRAWL COMPLETED!
```

---

**Try Method 1 or 2 now - they're the easiest!** ✅

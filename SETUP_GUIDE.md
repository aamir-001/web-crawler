# Quick Setup Guide for Team Members

Follow these steps to get the project running on your machine.

## Step 1: Install Prerequisites

### Java JDK 17
**Windows:**
```bash
# Using Chocolatey
choco install openjdk17

# Or download from: https://adoptium.net/
```

**macOS:**
```bash
# Using Homebrew
brew install openjdk@17
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

### Apache Maven
**Windows:**
```bash
# Using Chocolatey
choco install maven

# Or download from: https://maven.apache.org/download.cgi
```

**macOS:**
```bash
brew install maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
```

## Step 2: Verify Installation

Open a terminal/command prompt and run:
```bash
java -version
# Should show: openjdk version "17.x.x" or similar

mvn -version
# Should show: Apache Maven 3.x.x or higher
```

## Step 3: Clone and Setup Project

```bash
# Clone the repository
git clone <your-repo-url>
cd web-crawler

# Install all dependencies (this is like pip install -r requirements.txt)
mvn clean install
```

This single command will:
- Download all required libraries (JSoup, SQLite, HttpClient, etc.)
- Compile the project
- Run tests
- Package everything

## Step 4: Open in Your IDE

### IntelliJ IDEA (Recommended)
1. File → Open
2. Select the `web-crawler` folder (not pom.xml, the folder)
3. Wait for Maven to import dependencies (bottom right corner)
4. Done!

### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Browse to `web-crawler` folder
3. Click Finish
4. Wait for build to complete

### VS Code
1. Install "Extension Pack for Java" from marketplace
2. Open the `web-crawler` folder
3. Maven will auto-detect and import

## Step 5: Run the Application

### From IDE:
- Find `src/main/java/com/searchengine/Main.java`
- Right-click → Run

### From Command Line:
```bash
mvn compile exec:java -Dexec.mainClass="com.searchengine.Main"
```

## Common Issues

### "mvn: command not found"
- Maven not installed or not in PATH
- Restart terminal after installation
- On Windows, may need to add Maven bin folder to PATH manually

### "Java version mismatch"
- Make sure you have JDK 17 or higher
- Check `JAVA_HOME` environment variable points to correct JDK

### Dependencies not downloading
```bash
# Force refresh
mvn clean install -U

# Or delete .m2 cache and retry
# Windows: C:\Users\YourName\.m2\repository
# Mac/Linux: ~/.m2/repository
```

### OutOfMemoryError
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx1024m"  # Mac/Linux
set MAVEN_OPTS=-Xmx1024m       # Windows CMD
```

## Project Structure Overview

```
web-crawler/
├── pom.xml                     ← Maven config (like requirements.txt)
├── src/main/java/              ← Your Java source code goes here
│   └── com/searchengine/
│       ├── crawler/            ← Crawler components
│       ├── indexer/            ← Indexing logic
│       ├── search/             ← Search algorithms
│       ├── gui/                ← Swing/JavaFX GUI
│       ├── database/           ← SQLite handling
│       └── utils/              ← Helper classes
├── src/main/resources/         ← Config files
│   ├── config.properties       ← App settings
│   ├── logback.xml            ← Logging config
│   └── stopwords.txt          ← Stop words list
└── src/test/java/              ← Unit tests
```

## Useful Maven Commands

```bash
# Compile code only
mvn compile

# Run tests
mvn test

# Package as JAR (creates executable)
mvn package

# Clean build artifacts
mvn clean

# Clean + Install (fresh start)
mvn clean install

# Run application
mvn exec:java -Dexec.mainClass="com.searchengine.Main"

# Skip tests (faster build)
mvn clean install -DskipTests
```

## Development Workflow

1. **Pull latest changes**
   ```bash
   git pull origin main
   ```

2. **Create feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make changes and test**
   ```bash
   mvn clean test
   ```

4. **Commit and push**
   ```bash
   git add .
   git commit -m "Description of changes"
   git push origin feature/your-feature-name
   ```

5. **Create Pull Request** on GitHub

## Need Help?

- Check [README.md](README.md) for detailed documentation
- Maven issues: https://maven.apache.org/users/index.html
- Ask in team chat/Discord/Slack

## You're Ready!

Once `mvn clean install` completes successfully, you're all set to start coding!

# Desktop Search Engine - Web Crawler & Indexer

A multithreaded web crawler with local indexing and search capabilities built with Java.

## Team Members
- [Add team member names here]

## Project Overview

This application consists of three main components:
1. **Web Crawler** - Multithreaded crawler that downloads web pages up to a specified depth
2. **Indexer** - Builds an inverted index stored in SQLite database
3. **Search Engine** - GUI application to search the indexed content

## Prerequisites

- **Java Development Kit (JDK) 17** or higher
  - Download from: https://www.oracle.com/java/technologies/downloads/
  - Or use OpenJDK: https://adoptium.net/
- **Apache Maven 3.6+**
  - Download from: https://maven.apache.org/download.cgi
  - Or install via package manager (Chocolatey on Windows, Homebrew on Mac, apt on Linux)

### Verify Installation

```bash
java -version    # Should show Java 17 or higher
mvn -version     # Should show Maven 3.6 or higher
```

## Setup Instructions for Team Members

### 1. Clone the Repository

```bash
git clone <repository-url>
cd web-crawler
```

### 2. Install Dependencies

Maven will automatically download all required dependencies listed in `pom.xml`:

```bash
mvn clean install
```

This will download:
- JSoup (HTML parsing)
- SQLite JDBC Driver (database)
- Apache HttpClient (HTTP requests)
- Apache Commons Lang (utilities)
- SLF4J & Logback (logging)
- JUnit (testing)
- Gson (JSON handling)

### 3. Project Structure

```
web-crawler/
├── src/
│   ├── main/
│   │   ├── java/com/searchengine/
│   │   │   ├── crawler/        # Web crawler implementation
│   │   │   ├── indexer/        # Indexing logic
│   │   │   ├── search/         # Search algorithms
│   │   │   ├── gui/            # Swing/JavaFX GUI
│   │   │   ├── database/       # SQLite database management
│   │   │   ├── utils/          # Utility classes
│   │   │   └── Main.java       # Application entry point
│   │   └── resources/          # Configuration files, resources
│   └── test/
│       └── java/com/searchengine/  # Unit tests
├── data/                       # SQLite database and crawled data
├── pom.xml                     # Maven dependencies (like requirements.txt)
├── .gitignore
└── README.md
```

## Building the Project

### Compile
```bash
mvn compile
```

### Run Tests
```bash
mvn test
```

### Package (Create JAR)
```bash
mvn package
```

This creates `target/desktop-search-engine-1.0-SNAPSHOT-jar-with-dependencies.jar`

### Run the Application
```bash
mvn exec:java -Dexec.mainClass="com.searchengine.Main"
```

Or run the packaged JAR:
```bash
java -jar target/desktop-search-engine-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Development Workflow

### Using an IDE

**IntelliJ IDEA** (Recommended):
1. Open IntelliJ IDEA
2. File → Open → Select `pom.xml`
3. IntelliJ will automatically import Maven dependencies

**Eclipse**:
1. File → Import → Maven → Existing Maven Projects
2. Select the project directory

**VS Code**:
1. Install "Extension Pack for Java"
2. Open project folder
3. Maven will be detected automatically

### Running from IDE
- Simply run the `Main.java` class

## Key Dependencies (from pom.xml)

| Dependency | Version | Purpose |
|------------|---------|---------|
| JSoup | 1.17.2 | HTML parsing and link extraction |
| SQLite JDBC | 3.44.1.0 | Database for inverted index |
| Apache HttpClient5 | 5.3.1 | HTTP requests for crawling |
| Apache Commons Lang3 | 3.14.0 | Utility functions |
| SLF4J + Logback | 2.0.9 / 1.4.14 | Logging framework |
| JUnit Jupiter | 5.10.1 | Unit testing |
| Gson | 2.10.1 | JSON serialization |

## Configuration

Edit `src/main/resources/config.properties` to customize:
- Default crawl depth
- Thread pool size
- Database location
- Request timeout settings
- User agent string

## Features

### Crawler Features
- Multithreaded crawling with configurable thread pool
- Depth-limited crawling
- URL normalization and duplicate detection
- Robots.txt compliance
- Rate limiting to be polite to servers

### Indexer Features
- Inverted index: `Map<Word, List<PageInfo>>`
- Text tokenization and normalization
- Stop word removal
- Word stemming
- Persistent storage in SQLite

### Search Features
- Fast keyword search using inverted index
- Result ranking (TF-IDF or frequency-based)
- Multi-word query support
- Context snippets in results

### GUI Features
- Start/stop crawler with URL and depth input
- Real-time crawling progress display
- Search interface with results list
- Page preview

## Troubleshooting

### Maven dependency download issues
```bash
mvn clean install -U  # Force update dependencies
```

### Out of memory errors
```bash
export MAVEN_OPTS="-Xmx1024m"  # Linux/Mac
set MAVEN_OPTS=-Xmx1024m       # Windows
```

### Port conflicts / Database locked
- Close any running instances of the application
- Delete `data/search_engine.db` and restart

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Make changes and test: `mvn test`
3. Commit: `git commit -am "Add feature"`
4. Push: `git push origin feature/your-feature`
5. Create pull request

## License

Academic project - [Your University] - [Year]

## Resources

- [JSoup Documentation](https://jsoup.org/)
- [SQLite Java Tutorial](https://www.sqlitetutorial.net/sqlite-java/)
- [Maven Getting Started](https://maven.apache.org/guides/getting-started/)

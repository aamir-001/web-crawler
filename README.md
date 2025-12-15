# Desktop Search Engine

A multithreaded web crawler with indexing and search capabilities built in Java.

## Team Members

- **Sachin Adlakha**
- **Md Aamir**
- **Gaurav**

## University

- New York University

## Project Overview

This desktop application implements a complete search engine with three main components:

1. **Web Crawler** - Multithreaded crawler that downloads web pages with configurable depth and page limits
2. **Indexer** - Builds an inverted index with TF-IDF ranking, stored in SQLite
3. **Search Engine** - JavaFX GUI application to crawl websites and search indexed content

## Features

- Multithreaded web crawling (10 threads)
- Depth-limited and page-limited crawling
- robots.txt compliance
- URL normalization and deduplication
- Porter Stemmer for word normalization
- Stop word filtering (174 words)
- TF-IDF search ranking
- Snippet generation with keyword highlighting
- JavaFX GUI with real-time progress tracking

## Technologies Used

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Core language |
| JavaFX | 21.0.1 | GUI framework |
| JSoup | 1.17.2 | HTML parsing |
| SQLite JDBC | 3.44.1.0 | Database |
| Apache HttpClient5 | 5.3.1 | HTTP requests |
| Apache Commons Lang3 | 3.14.0 | Utilities |
| SLF4J + Logback | 2.0.9 | Logging |
| JUnit 5 | 5.10.1 | Testing |

## Prerequisites

- **Java JDK 17** or higher
- **Apache Maven 3.6+**

Verify installation:
```bash
java -version    # Should show Java 17+
mvn -version     # Should show Maven 3.6+
```

## Setup & Installation

### 1. Clone the Repository
```bash
git clone https://github.com/aamir-001/web-crawler.git
cd web-crawler
```

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run the Application

**Launch GUI:**
```bash
mvn javafx:run
```

**Run CLI demos:**
```bash
mvn exec:java -Dexec.mainClass="com.searchengine.CrawlerDemo"   # Crawler demo
mvn exec:java -Dexec.mainClass="com.searchengine.IndexerDemo"   # Indexer demo
mvn exec:java -Dexec.mainClass="com.searchengine.SearchDemo"    # Search demo
```

## How to Use

### Crawling
1. Launch the GUI with `mvn javafx:run`
2. Enter a URL (e.g., `https://quotes.toscrape.com`)
3. Set Max Depth (1-10) and Max Pages (10-1000)
4. Click **Start Crawl**
5. Watch real-time progress in the log area

### Searching
1. Switch to the **Search** tab
2. Click **Reindex** to index crawled pages
3. Enter search terms (e.g., `love`, `life`, `Einstein`)
4. View ranked results with snippets
5. Click result links to open in browser

## Project Structure

```
src/main/java/com/searchengine/
├── Main.java                 # Application entry point
├── CrawlerDemo.java          # CLI crawler demo
├── IndexerDemo.java          # CLI indexer demo
├── SearchDemo.java           # CLI search demo
├── crawler/                  # Web crawler components
│   ├── WebCrawler.java       # Main crawler orchestrator
│   ├── CrawlerTask.java      # Single page crawl task
│   ├── URLQueue.java         # Thread-safe URL queue
│   └── RobotsTxtParser.java  # robots.txt handler
├── indexer/                  # Indexing components
│   ├── Indexer.java          # Main indexer
│   ├── InvertedIndex.java    # In-memory inverted index
│   ├── Tokenizer.java        # Text tokenization
│   ├── Stemmer.java          # Porter Stemmer
│   └── StopWordFilter.java   # Stop word removal
├── search/                   # Search components
│   ├── SearchEngine.java     # Main search API
│   ├── QueryProcessor.java   # Query parsing
│   ├── TFIDFCalculator.java  # TF-IDF ranking
│   ├── SnippetGenerator.java # Snippet generation
│   └── SearchResult.java     # Result model
├── database/                 # Database layer
│   ├── DatabaseManager.java  # Connection pooling
│   ├── PageDAO.java          # Page operations
│   └── IndexDAO.java         # Index persistence
├── gui/                      # JavaFX GUI
│   ├── MainWindow.java       # Main application window
│   ├── CrawlerPanel.java     # Crawler controls
│   └── SearchPanel.java      # Search interface
└── utils/                    # Utilities
    ├── ConfigLoader.java     # Configuration
    ├── URLNormalizer.java    # URL normalization
    └── URLValidator.java     # URL validation
```

## Database Schema

```sql
pages (page_id, url, title, content, crawl_timestamp, word_count, depth)
words (word_id, word)
word_positions (word_id, page_id, frequency, positions)
crawl_metadata (crawl_id, start_url, max_depth, pages_crawled, start_time, end_time, status)
```

## Testing

Run all tests:
```bash
mvn test
```

**Test Coverage:** 21 tests across 3 test classes
- `QuickTest.java` - Database and indexer tests
- `CrawlerTest.java` - Crawler unit tests
- `SearchEngineTest.java` - Search functionality tests

## Configuration

Edit `src/main/resources/config.properties`:
```properties
crawler.thread.pool.size=10
crawler.max.pages=500
crawler.default.depth=3
crawler.delay.between.requests=1000
crawler.request.timeout=30000
database.path=data/demo.db
```

## Sample Test URLs

| URL | Description |
|-----|-------------|
| `https://quotes.toscrape.com` | Quotes website (recommended) |
| `https://books.toscrape.com` | Fake bookstore |
| `https://example.com` | Simple test page |

## Troubleshooting

**Maven dependency issues:**
```bash
mvn clean install -U
```

**JavaFX not found:**
```bash
mvn dependency:resolve
```

**Database locked:**
- Close any running instances
- Delete `data/demo.db` and restart

## Documentation

See the `docs/` folder for detailed documentation:
- `ARCHITECTURE.md` - System design
- `IMPLEMENTATION_STATUS.md` - Implementation details
- `SETUP_GUIDE.md` - Setup instructions
- `TESTING_GUIDE.md` - Testing guide

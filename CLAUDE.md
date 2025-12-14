# Desktop Search Engine - Project Memory

## Project Overview
A Java-based desktop search engine with web crawling, indexing, and search capabilities. Built with Maven, using SQLite for persistence.

## Current Status (Dec 2025)
- **Phase 1 (Database)**: Complete
- **Phase 2 (Crawler)**: Complete
- **Phase 3 (Indexer)**: Complete
- **Phase 4 (Search Engine)**: Complete - TF-IDF ranking, snippets, query processing
- **Phase 5 (GUI)**: Complete - JavaFX tabbed interface
- **Phase 6 (Polish/Testing)**: Not started

## Build Commands
```bash
mvn clean compile          # Build project
mvn test                   # Run all tests (21 tests)
mvn javafx:run             # Launch GUI application
mvn exec:java -Dexec.mainClass="com.searchengine.CrawlerDemo"  # CLI: Test crawler
mvn exec:java -Dexec.mainClass="com.searchengine.IndexerDemo"  # CLI: Test indexer
mvn exec:java -Dexec.mainClass="com.searchengine.SearchDemo"   # CLI: Interactive search
```

## Key Architecture Decisions
- **Database**: SQLite with connection pooling (5 connections default)
- **Crawler**: 10-thread pool, 1000ms politeness delay, respects robots.txt
- **Indexer**: Porter Stemmer, 174 stop words, inverted index with positions
- **Search**: TF-IDF ranking, snippet generation, query stemming

## Documentation (7 files in docs/)
- ARCHITECTURE.md - System design
- IMPLEMENTATION_STATUS.md - Current phase status
- NEXT_STEPS.md - Implementation guide
- PROJECT_OVERVIEW.md - High-level overview
- SETUP_GUIDE.md - Setup instructions
- TEAM_HANDOFF.md - Summary for teammates
- TESTING_GUIDE.md - Testing instructions

## Important Files
- `src/main/resources/config.properties` - All configurable settings
- `src/main/resources/stopwords.txt` - 174 English stop words
- `src/main/resources/logback.xml` - Logging configuration
- `data/demo.db` - SQLite database (created on first run)

## Bugs Fixed
1. **DatabaseManager singleton bug**: `shutdown()` method now resets static instance to null, allowing tests to create fresh instances with different database paths.

## Code Quality
- No AI code traces found
- Human-readable code style throughout
- Proper logging with SLF4J/Logback
- Good separation of concerns

## Nice-to-Have Features (Future)
1. Site-specific crawling mode
2. Crawl resume capability
3. Advanced query syntax (phrases, NOT, wildcards)
4. Auto-complete suggestions
5. Search history
6. Export to CSV/JSON
7. Duplicate content detection
8. Performance dashboard
9. Content type filtering
10. Bookmarks/favorites
11. Multi-language support
12. Page preview caching

## Test Coverage (21 tests passing)
- QuickTest.java: 8 tests
- CrawlerTest.java: 5 tests
- SearchEngineTest.java: 8 tests

## Database Schema
```sql
pages (page_id, url, title, content, crawl_timestamp, word_count, depth)
words (word_id, word)
word_positions (word_id, page_id, frequency, positions)
crawl_metadata (crawl_id, start_url, max_depth, pages_crawled, start_time, end_time, status)
```

## Dependencies (Java 17)
- JSoup 1.17.2 - HTML parsing
- SQLite JDBC 3.44.1.0 - Database
- Apache HttpClient5 5.3.1 - HTTP requests
- Apache Commons Lang3 3.14.0 - Utilities
- SLF4J + Logback - Logging
- JUnit 5 - Testing
- Gson - JSON handling
- JavaFX 21.0.1 - GUI framework

## Phase 4 Search Package (Complete)
```
src/main/java/com/searchengine/search/
├── QueryProcessor.java    - Query parsing and stemming
├── SearchEngine.java      - Main search API
├── SearchResult.java      - Result data model
├── SnippetGenerator.java  - Snippet generation with highlighting
└── TFIDFCalculator.java   - TF-IDF ranking algorithm
```

## Phase 5 GUI Package (Complete)
```
src/main/java/com/searchengine/gui/
├── MainWindow.java        - Main JavaFX application with tabs
├── CrawlerPanel.java      - URL input, controls, progress, log
└── SearchPanel.java       - Search input, TF-IDF results, clickable links
```

## Next Implementation: Phase 6 (Polish & Testing)
- Comprehensive unit tests
- Error handling improvements
- Input validation
- Performance optimization

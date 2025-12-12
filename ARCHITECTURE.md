# Desktop Search Engine - Architecture & Design

This document outlines the architecture and design decisions for the Desktop Search Engine project.

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         GUI Layer                            │
│  (Swing/JavaFX - User Interface for Crawling & Searching)   │
└────────────┬──────────────────────────────┬─────────────────┘
             │                              │
             ▼                              ▼
┌────────────────────────┐    ┌──────────────────────────────┐
│   Crawler Module       │    │    Search Module             │
│                        │    │                              │
│  - URL Queue           │    │  - Query Parser              │
│  - Thread Pool         │    │  - Ranking Algorithm         │
│  - HTML Parser         │    │  - Result Formatter          │
│  - Link Extractor      │    │  - Snippet Generator         │
└────────────┬───────────┘    └──────────────┬───────────────┘
             │                               │
             ▼                               │
┌────────────────────────┐                   │
│   Indexer Module       │                   │
│                        │                   │
│  - Tokenizer           │                   │
│  - Stop Word Remover   │                   │
│  - Stemmer             │                   │
│  - Inverted Index      │◄──────────────────┘
└────────────┬───────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────────┐
│                    Database Layer (SQLite)                    │
│                                                               │
│  Tables: pages, words, word_positions, crawl_metadata        │
└───────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Crawler Module (`com.searchengine.crawler`)

**Purpose:** Download web pages and extract content

**Key Classes:**
- `WebCrawler` - Main crawler orchestrator
- `CrawlerTask` - Runnable task for thread pool
- `URLQueue` - Thread-safe queue for URLs to crawl
- `RobotsTxtParser` - Respects robots.txt rules
- `URLNormalizer` - Normalizes URLs to avoid duplicates

**Flow:**
1. User provides starting URL and depth
2. URL added to queue, marked as depth 0
3. Worker threads fetch URLs from queue
4. Parse HTML, extract text content and links
5. Save page content to database
6. Add discovered links to queue (if depth < max)
7. Repeat until queue empty or max pages reached

**Thread Safety:**
- ConcurrentHashMap for visited URLs
- BlockingQueue for URL queue
- ExecutorService for thread pool management

---

### 2. Indexer Module (`com.searchengine.indexer`)

**Purpose:** Build inverted index from crawled pages

**Key Classes:**
- `Indexer` - Main indexing orchestrator
- `Tokenizer` - Splits text into words
- `StopWordFilter` - Removes common words
- `Stemmer` - Reduces words to root form (Porter Stemmer)
- `InvertedIndex` - In-memory index structure

**Inverted Index Structure:**
```java
Map<String, List<PostingEntry>>
    │         └─> PostingEntry { pageId, frequency, positions[] }
    └─> word (normalized, stemmed)
```

**Processing Pipeline:**
```
Raw HTML → Extract Text → Tokenize → Remove Stop Words →
Stem Words → Build Inverted Index → Save to Database
```

**Example:**
```
Page: "The quick brown fox jumps over the lazy dog"

After processing:
"quick" → [PageEntry(pageId=1, freq=1, pos=[1])]
"brown" → [PageEntry(pageId=1, freq=1, pos=[2])]
"fox"   → [PageEntry(pageId=1, freq=1, pos=[3])]
"jump"  → [PageEntry(pageId=1, freq=1, pos=[4])]  // stemmed
"lazi"  → [PageEntry(pageId=1, freq=1, pos=[6])]  // stemmed
"dog"   → [PageEntry(pageId=1, freq=1, pos=[7])]

// "the", "over" removed as stop words
```

---

### 3. Search Module (`com.searchengine.search`)

**Purpose:** Execute searches and rank results

**Key Classes:**
- `SearchEngine` - Main search interface
- `QueryProcessor` - Parses and processes queries
- `RankingAlgorithm` - Ranks results (TF-IDF)
- `SnippetGenerator` - Creates context snippets

**Search Algorithm:**
1. Parse query (normalize, stem)
2. Look up words in inverted index
3. Find pages containing query terms
4. Calculate relevance score (TF-IDF)
5. Sort by score
6. Generate snippets
7. Return top N results

**TF-IDF Ranking:**
- **TF (Term Frequency):** How often word appears in document
- **IDF (Inverse Document Frequency):** How rare word is across all documents
- **Score:** TF × IDF (higher = more relevant)

```
TF(word, doc) = frequency of word in doc / total words in doc
IDF(word) = log(total documents / documents containing word)
Score = TF × IDF
```

---

### 4. Database Layer (`com.searchengine.database`)

**Purpose:** Persistent storage with SQLite

**Key Classes:**
- `DatabaseManager` - Connection pool and setup
- `PageDAO` - Page CRUD operations
- `IndexDAO` - Index operations
- `SearchDAO` - Search queries

**Database Schema:**

```sql
-- Stores crawled pages
CREATE TABLE pages (
    page_id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT UNIQUE NOT NULL,
    title TEXT,
    content TEXT,
    crawl_timestamp INTEGER,
    word_count INTEGER
);

-- Stores unique words from all pages
CREATE TABLE words (
    word_id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT UNIQUE NOT NULL
);

-- Inverted index: maps words to pages
CREATE TABLE word_positions (
    word_id INTEGER,
    page_id INTEGER,
    frequency INTEGER,
    positions TEXT,  -- JSON array of positions
    FOREIGN KEY(word_id) REFERENCES words(word_id),
    FOREIGN KEY(page_id) REFERENCES pages(page_id),
    PRIMARY KEY(word_id, page_id)
);

-- Metadata about crawls
CREATE TABLE crawl_metadata (
    crawl_id INTEGER PRIMARY KEY AUTOINCREMENT,
    start_url TEXT,
    max_depth INTEGER,
    pages_crawled INTEGER,
    start_time INTEGER,
    end_time INTEGER
);

-- Indexes for performance
CREATE INDEX idx_words_word ON words(word);
CREATE INDEX idx_word_positions_word ON word_positions(word_id);
CREATE INDEX idx_word_positions_page ON word_positions(page_id);
CREATE INDEX idx_pages_url ON pages(url);
```

---

### 5. GUI Module (`com.searchengine.gui`)

**Purpose:** User interface

**Key Classes:**
- `MainWindow` - Main application window
- `CrawlerPanel` - Crawler controls and progress
- `SearchPanel` - Search interface and results
- `ResultsRenderer` - Custom renderer for results

**GUI Layout:**

```
┌───────────────────────────────────────────────────────┐
│  Desktop Search Engine                          [_][□][×] │
├───────────────────────────────────────────────────────┤
│  [Crawler] [Search]                              Tabs │
├───────────────────────────────────────────────────────┤
│                                                        │
│  Crawler Panel:                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │ Starting URL: [http://example.com        ]       │ │
│  │ Max Depth:    [3            ▼]                   │ │
│  │ [Start Crawl]  [Stop]                            │ │
│  │                                                   │ │
│  │ Progress: ████████░░░░░░░░  45/100 pages         │ │
│  │ Status: Crawling depth 2...                      │ │
│  │                                                   │ │
│  │ Log:                                             │ │
│  │ [2024-12-12 10:23:45] Started crawling...       │ │
│  │ [2024-12-12 10:23:46] Fetched: example.com      │ │
│  └──────────────────────────────────────────────────┘ │
│                                                        │
│  Search Panel:                                         │
│  ┌──────────────────────────────────────────────────┐ │
│  │ Search: [java programming      ] [Search]        │ │
│  │                                                   │ │
│  │ Results (234 found):                             │ │
│  │ ┌────────────────────────────────────────────┐  │ │
│  │ │ 1. Introduction to Java Programming         │  │ │
│  │ │    https://example.com/java-intro           │  │ │
│  │ │    Learn java programming basics...         │  │ │
│  │ │                                             │  │ │
│  │ │ 2. Advanced Java Concepts                   │  │ │
│  │ │    https://example.com/java-advanced        │  │ │
│  │ │    Deep dive into java programming...       │  │ │
│  │ └────────────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────┘
```

---

### 6. Utils Module (`com.searchengine.utils`)

**Purpose:** Shared utilities

**Key Classes:**
- `ConfigLoader` - Loads config.properties
- `Logger` - Logging utilities
- `URLValidator` - URL validation
- `TextProcessor` - Text processing utilities

---

## Data Flow

### Crawling Flow
```
User Input (URL, Depth)
    ↓
WebCrawler.start()
    ↓
URLQueue.add(startURL)
    ↓
ThreadPool → CrawlerTask
    ↓
HTTP Request → Download HTML
    ↓
Parse HTML (JSoup)
    ↓
Extract: Text Content + Links
    ↓
Save to Database (PageDAO)
    ↓
Trigger Indexer
    ↓
Add Links to Queue (if depth < max)
```

### Indexing Flow
```
New Page in Database
    ↓
Indexer.indexPage()
    ↓
Tokenize Text
    ↓
Remove Stop Words
    ↓
Stem Words
    ↓
Build Inverted Index (in memory)
    ↓
Save to Database (word_positions table)
```

### Search Flow
```
User Query
    ↓
QueryProcessor.process()
    ↓
Normalize + Stem Query Terms
    ↓
Look up in Inverted Index (IndexDAO)
    ↓
Get Matching Pages
    ↓
Calculate TF-IDF Scores
    ↓
Sort by Relevance
    ↓
Generate Snippets
    ↓
Display Results in GUI
```

---

## Design Patterns Used

1. **Singleton Pattern**
   - `DatabaseManager` - Single database connection pool

2. **Factory Pattern**
   - `CrawlerTaskFactory` - Creates crawler tasks

3. **Observer Pattern**
   - GUI observes crawler progress

4. **DAO Pattern**
   - `PageDAO`, `IndexDAO`, `SearchDAO` - Data access layer

5. **Thread Pool Pattern**
   - `ExecutorService` for crawler threads

---

## Performance Considerations

1. **Multithreading**
   - Thread pool size configurable (default: 10)
   - Non-blocking URL queue

2. **Database Indexing**
   - Indexes on frequently queried columns
   - Batch inserts for better performance

3. **Memory Management**
   - Stream large results
   - Limit in-memory index size
   - Periodic garbage collection

4. **Caching**
   - Cache frequently searched terms
   - Cache database connections

---

## Security & Politeness

1. **Robots.txt Compliance**
   - Check robots.txt before crawling
   - Respect Disallow directives

2. **Rate Limiting**
   - Configurable delay between requests (default: 1s)
   - Avoid overwhelming target servers

3. **User Agent**
   - Clear identification as educational bot

4. **Input Validation**
   - Validate URLs
   - Sanitize search queries

---

## Testing Strategy

1. **Unit Tests**
   - Tokenizer, Stemmer, URLNormalizer
   - Database operations

2. **Integration Tests**
   - Crawler + Indexer
   - Search + Database

3. **End-to-End Tests**
   - Full crawl → index → search workflow

---

## Future Enhancements

1. PageRank algorithm for better ranking
2. Support for PDF, DOC files
3. Image search
4. Distributed crawling
5. Web-based GUI
6. Real-time indexing
7. Autocomplete suggestions
8. Advanced query syntax (AND, OR, NOT, quotes)

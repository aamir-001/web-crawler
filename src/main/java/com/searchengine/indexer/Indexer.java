package com.searchengine.indexer;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.IndexDAO;
import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Indexer processes crawled pages and builds an inverted index.
 *
 * Flow:
 * 1. Tokenize text into words
 * 2. Filter stop words
 * 3. Stem words to their root form
 * 4. Build inverted index
 * 5. Save to database
 */
public class Indexer {
    private static final Logger logger = LoggerFactory.getLogger(Indexer.class);

    private final PageDAO pageDAO;
    private final IndexDAO indexDAO;
    private final Tokenizer tokenizer;
    private final StopWordFilter stopWordFilter;
    private final Stemmer stemmer;
    private final InvertedIndex invertedIndex;

    private final AtomicInteger pagesIndexed = new AtomicInteger(0);
    private final AtomicInteger wordsIndexed = new AtomicInteger(0);

    // Listener for indexing progress
    private IndexerProgressListener progressListener;

    public Indexer(DatabaseManager dbManager) {
        this.pageDAO = new PageDAO(dbManager);
        this.indexDAO = new IndexDAO(dbManager);
        this.tokenizer = new Tokenizer();
        this.stopWordFilter = new StopWordFilter();
        this.stemmer = new Stemmer();
        this.invertedIndex = new InvertedIndex();

        logger.info("Indexer initialized with {} stop words", stopWordFilter.getStopWordCount());
    }

    /**
     * Index a single page by its ID.
     *
     * @param pageId The ID of the page to index
     * @return true if successful, false otherwise
     */
    public boolean indexPage(Long pageId) {
        try {
            // Fetch page from database
            var pageOptional = pageDAO.getPageById(pageId);
            if (pageOptional.isEmpty()) {
                logger.warn("Page {} not found in database", pageId);
                return false;
            }

            return indexPage(pageOptional.get());

        } catch (SQLException e) {
            logger.error("Failed to index page {}: {}", pageId, e.getMessage());
            return false;
        }
    }

    /**
     * Index a single page object.
     *
     * @param page The page to index
     * @return true if successful, false otherwise
     */
    public boolean indexPage(Page page) {
        try {
            logger.debug("Indexing page: {}", page.getUrl());

            // Combine title and content for indexing
            String textToIndex = combineTextFields(page);

            // Step 1: Tokenize text
            List<Tokenizer.TokenPosition> tokens = tokenizer.tokenizeWithPositions(textToIndex);
            logger.debug("Tokenized {} words from page {}", tokens.size(), page.getPageId());

            // Step 2: Filter stop words
            List<Tokenizer.TokenPosition> filteredTokens = stopWordFilter.filterPositions(tokens);
            logger.debug("After stop word filtering: {} words remain", filteredTokens.size());

            // Step 3: Stem words and build index
            int wordCount = 0;
            for (Tokenizer.TokenPosition tp : filteredTokens) {
                String stemmedWord = stemmer.stem(tp.getToken());

                // Add to in-memory index
                invertedIndex.addWord(stemmedWord, page.getPageId(), tp.getPosition());

                wordCount++;
            }

            // Step 4: Save to database
            savePageIndexToDatabase(page.getPageId(), filteredTokens);

            // Update page word count
            pageDAO.updateWordCount(page.getPageId(), wordCount);

            pagesIndexed.incrementAndGet();
            wordsIndexed.addAndGet(wordCount);

            // Notify listener
            if (progressListener != null) {
                progressListener.onPageIndexed(page.getPageId(), page.getUrl(), wordCount);
            }

            logger.info("Indexed page {}: {} words", page.getUrl(), wordCount);
            return true;

        } catch (Exception e) {
            logger.error("Failed to index page {}: {}", page.getUrl(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Index all pages from the database.
     *
     * @return Number of pages indexed
     */
    public int indexAllPages() {
        try {
            logger.info("Starting indexing of all pages...");

            List<Page> pages = pageDAO.getAllPages();
            logger.info("Found {} pages to index", pages.size());

            int successCount = 0;
            for (Page page : pages) {
                if (indexPage(page)) {
                    successCount++;
                }
            }

            logger.info("Indexing complete: {}/{} pages indexed", successCount, pages.size());
            return successCount;

        } catch (SQLException e) {
            logger.error("Failed to index all pages: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Combine title and content for indexing.
     * Title words are weighted more heavily by appearing first.
     */
    private String combineTextFields(Page page) {
        StringBuilder combined = new StringBuilder();

        // Add title first (appears at positions 0-N)
        // This gives title words higher relevance in position-based ranking
        if (page.getTitle() != null && !page.getTitle().isEmpty()) {
            combined.append(page.getTitle()).append(" ");
        }

        // Add content
        if (page.getContent() != null && !page.getContent().isEmpty()) {
            combined.append(page.getContent());
        }

        return combined.toString();
    }

    /**
     * Save the index for a page to the database.
     */
    private void savePageIndexToDatabase(Long pageId, List<Tokenizer.TokenPosition> tokens) throws SQLException {
        // Group tokens by stemmed word
        var wordPositionsMap = new java.util.HashMap<String, java.util.List<Integer>>();

        for (Tokenizer.TokenPosition tp : tokens) {
            String stemmedWord = stemmer.stem(tp.getToken());
            wordPositionsMap.computeIfAbsent(stemmedWord, k -> new java.util.ArrayList<>())
                    .add(tp.getPosition());
        }

        // Save each word and its positions
        for (var entry : wordPositionsMap.entrySet()) {
            String word = entry.getKey();
            List<Integer> positions = entry.getValue();

            // Create a posting entry
            InvertedIndex.PostingEntry postingEntry = new InvertedIndex.PostingEntry(pageId);
            for (Integer position : positions) {
                postingEntry.addPosition(position);
            }

            // Save to database
            indexDAO.saveWordEntry(word, pageId, postingEntry);
        }
    }

    /**
     * Re-index a specific page (useful after page updates).
     */
    public boolean reindexPage(Long pageId) {
        try {
            // Delete old index entries
            indexDAO.deleteIndexForPage(pageId);

            // Index the page again
            return indexPage(pageId);

        } catch (SQLException e) {
            logger.error("Failed to re-index page {}: {}", pageId, e.getMessage());
            return false;
        }
    }

    /**
     * Get the in-memory inverted index.
     */
    public InvertedIndex getInvertedIndex() {
        return invertedIndex;
    }

    /**
     * Get indexing statistics.
     */
    public IndexingStats getStats() {
        return new IndexingStats(
                pagesIndexed.get(),
                wordsIndexed.get(),
                invertedIndex.getUniqueWordCount(),
                invertedIndex.getTotalWordCount()
        );
    }

    /**
     * Set progress listener.
     */
    public void setProgressListener(IndexerProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Listener interface for indexing progress.
     */
    public interface IndexerProgressListener {
        void onPageIndexed(Long pageId, String url, int wordCount);
    }

    /**
     * Indexing statistics.
     */
    public static class IndexingStats {
        private final int pagesIndexed;
        private final int totalWords;
        private final int uniqueWords;
        private final long totalWordOccurrences;

        public IndexingStats(int pagesIndexed, int totalWords, int uniqueWords, long totalWordOccurrences) {
            this.pagesIndexed = pagesIndexed;
            this.totalWords = totalWords;
            this.uniqueWords = uniqueWords;
            this.totalWordOccurrences = totalWordOccurrences;
        }

        public int getPagesIndexed() {
            return pagesIndexed;
        }

        public int getTotalWords() {
            return totalWords;
        }

        public int getUniqueWords() {
            return uniqueWords;
        }

        public long getTotalWordOccurrences() {
            return totalWordOccurrences;
        }

        @Override
        public String toString() {
            return String.format(
                    "Pages indexed: %d, Total words: %d, Unique words: %d, Total occurrences: %d",
                    pagesIndexed, totalWords, uniqueWords, totalWordOccurrences
            );
        }
    }
}

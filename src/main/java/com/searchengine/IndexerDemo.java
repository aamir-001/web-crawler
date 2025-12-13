package com.searchengine;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.IndexDAO;
import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import com.searchengine.indexer.Indexer;
import com.searchengine.indexer.InvertedIndex;
import com.searchengine.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Demo application to test the indexing functionality.
 * This indexes all crawled pages and shows search results.
 */
public class IndexerDemo {
    private static final Logger logger = LoggerFactory.getLogger(IndexerDemo.class);

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("📚 INDEXER DEMO - Building Search Index");
        System.out.println("=".repeat(60));
        System.out.println();

        try {
            // Load configuration
            String dbPath = ConfigLoader.getString("database.path", "data/demo.db");

            // Initialize database
            DatabaseManager dbManager = DatabaseManager.getInstance(dbPath, 5);
            PageDAO pageDAO = new PageDAO(dbManager);
            IndexDAO indexDAO = new IndexDAO(dbManager);

            // Check if we have pages to index
            int totalPages = pageDAO.getTotalPageCount();
            System.out.println("📄 Total pages in database: " + totalPages);

            if (totalPages == 0) {
                System.out.println("⚠️  No pages found! Please run CrawlerDemo first.");
                return;
            }

            System.out.println();
            System.out.println("🔨 Starting indexing process...");
            System.out.println("-".repeat(60));

            // Create indexer with progress listener
            Indexer indexer = new Indexer(dbManager);
            indexer.setProgressListener((pageId, url, wordCount) -> {
                System.out.printf("✅ Indexed page %d: %s (%d words)%n", pageId, url, wordCount);
            });

            // Index all pages
            long startTime = System.currentTimeMillis();
            indexer.indexAllPages();
            long endTime = System.currentTimeMillis();

            System.out.println("-".repeat(60));
            System.out.println("✅ Indexing complete!");
            System.out.println();

            // Show statistics
            Indexer.IndexingStats stats = indexer.getStats();
            System.out.println("📊 Indexing Statistics:");
            System.out.println("   - Pages indexed: " + stats.getPagesIndexed());
            System.out.println("   - Total words indexed: " + stats.getTotalWords());
            System.out.println("   - Unique words: " + stats.getUniqueWords());
            System.out.println("   - Total word occurrences: " + stats.getTotalWordOccurrences());
            System.out.println("   - Time taken: " + (endTime - startTime) + " ms");
            System.out.println();

            // Database statistics
            int dbUniqueWords = indexDAO.getTotalUniqueWords();
            int dbWordPositions = indexDAO.getTotalWordPositions();
            System.out.println("💾 Database Statistics:");
            System.out.println("   - Unique words in DB: " + dbUniqueWords);
            System.out.println("   - Word-page associations: " + dbWordPositions);
            System.out.println();

            // Demonstrate search functionality
            demonstrateSearch(indexer, pageDAO);

        } catch (Exception e) {
            logger.error("Error during indexing", e);
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void demonstrateSearch(Indexer indexer, PageDAO pageDAO) throws SQLException {
        System.out.println("=".repeat(60));
        System.out.println("🔍 SEARCH DEMONSTRATION");
        System.out.println("=".repeat(60));
        System.out.println();

        InvertedIndex index = indexer.getInvertedIndex();

        // Example searches
        String[] searchTerms = {"example", "domain", "information", "more", "test"};

        for (String term : searchTerms) {
            System.out.println("Searching for: \"" + term + "\"");

            List<InvertedIndex.PostingEntry> results = index.search(term);

            if (results.isEmpty()) {
                System.out.println("   No results found.");
            } else {
                System.out.println("   Found in " + results.size() + " page(s):");

                // Show top 5 results
                int count = 0;
                for (InvertedIndex.PostingEntry entry : results) {
                    if (count >= 5) break;

                    var pageOptional = pageDAO.getPageById(entry.getPageId());
                    if (pageOptional.isPresent()) {
                        Page page = pageOptional.get();
                        System.out.printf("   %d. [Page %d] %s (frequency: %d)%n",
                                count + 1, entry.getPageId(), page.getUrl(), entry.getFrequency());

                        // Show first few positions
                        List<Integer> positions = entry.getPositions();
                        if (positions.size() <= 5) {
                            System.out.println("      Positions: " + positions);
                        } else {
                            System.out.println("      Positions: " + positions.subList(0, 5) + "... (+" + (positions.size() - 5) + " more)");
                        }
                    }
                    count++;
                }

                if (results.size() > 5) {
                    System.out.println("   ... and " + (results.size() - 5) + " more results");
                }
            }
            System.out.println();
        }

        // Demonstrate AND search
        System.out.println("-".repeat(60));
        System.out.println("🔍 AND Search (pages containing ALL terms)");
        System.out.println("-".repeat(60));
        List<String> andTerms = List.of("example", "domain");
        System.out.println("Searching for pages containing: " + andTerms);

        List<Long> andResults = index.searchAnd(andTerms);
        if (andResults.isEmpty()) {
            System.out.println("   No pages contain all terms.");
        } else {
            System.out.println("   Found " + andResults.size() + " page(s):");
            for (int i = 0; i < Math.min(5, andResults.size()); i++) {
                Long pageId = andResults.get(i);
                var pageOptional = pageDAO.getPageById(pageId);
                if (pageOptional.isPresent()) {
                    Page page = pageOptional.get();
                    System.out.printf("   %d. [Page %d] %s%n", i + 1, pageId, page.getUrl());
                }
            }
        }
        System.out.println();

        // Demonstrate OR search
        System.out.println("-".repeat(60));
        System.out.println("🔍 OR Search (pages containing ANY term)");
        System.out.println("-".repeat(60));
        List<String> orTerms = List.of("example", "information");
        System.out.println("Searching for pages containing: " + orTerms);

        List<Long> orResults = index.searchOr(orTerms);
        if (orResults.isEmpty()) {
            System.out.println("   No pages contain any of the terms.");
        } else {
            System.out.println("   Found " + orResults.size() + " page(s):");
            for (int i = 0; i < Math.min(5, orResults.size()); i++) {
                Long pageId = orResults.get(i);
                var pageOptional = pageDAO.getPageById(pageId);
                if (pageOptional.isPresent()) {
                    Page page = pageOptional.get();
                    System.out.printf("   %d. [Page %d] %s%n", i + 1, pageId, page.getUrl());
                }
            }
        }
        System.out.println();

        System.out.println("=".repeat(60));
        System.out.println("✅ Indexing and search demonstration complete!");
        System.out.println("=".repeat(60));
    }
}

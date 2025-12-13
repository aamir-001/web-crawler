package com.searchengine;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import com.searchengine.indexer.Indexer;
import com.searchengine.indexer.InvertedIndex;
import com.searchengine.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Scanner;

/**
 * Interactive search demo - search for any word!
 */
public class SearchDemo {
    private static final Logger logger = LoggerFactory.getLogger(SearchDemo.class);

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("🔍 INTERACTIVE SEARCH DEMO");
        System.out.println("=".repeat(60));
        System.out.println();

        try {
            // Load configuration
            String dbPath = ConfigLoader.getString("database.path", "data/demo.db");

            // Initialize database
            DatabaseManager dbManager = DatabaseManager.getInstance(dbPath, 5);
            PageDAO pageDAO = new PageDAO(dbManager);

            // Create indexer (loads index from database into memory)
            System.out.println("Loading index from database...");
            Indexer indexer = new Indexer(dbManager);
            indexer.indexAllPages(); // Re-index to load into memory
            InvertedIndex index = indexer.getInvertedIndex();

            System.out.println("✅ Index loaded!");
            System.out.println("   - Total pages: " + pageDAO.getTotalPageCount());
            System.out.println("   - Unique words: " + index.getUniqueWordCount());
            System.out.println();
            System.out.println("Type words to search (or 'quit' to exit):");
            System.out.println("-".repeat(60));

            // Interactive search loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n🔍 Search: ");
                String query = scanner.nextLine().trim().toLowerCase();

                if (query.isEmpty()) {
                    continue;
                }

                if (query.equals("quit") || query.equals("exit")) {
                    System.out.println("\n👋 Goodbye!");
                    break;
                }

                // Handle multi-word queries (AND search)
                if (query.contains(" ")) {
                    String[] words = query.split("\\s+");
                    List<String> wordList = List.of(words);

                    System.out.println("\nSearching for pages with ALL words: " + wordList);
                    List<Long> results = index.searchAnd(wordList);

                    if (results.isEmpty()) {
                        System.out.println("❌ No pages contain all these words.");
                    } else {
                        System.out.println("✅ Found " + results.size() + " page(s):");
                        for (int i = 0; i < Math.min(10, results.size()); i++) {
                            var pageOpt = pageDAO.getPageById(results.get(i));
                            if (pageOpt.isPresent()) {
                                Page page = pageOpt.get();
                                System.out.printf("   %d. [Page %d] %s%n",
                                    i + 1, page.getPageId(), page.getUrl());
                                System.out.printf("      Title: %s%n",
                                    page.getTitle() != null ? page.getTitle() : "(no title)");
                            }
                        }
                        if (results.size() > 10) {
                            System.out.println("   ... and " + (results.size() - 10) + " more");
                        }
                    }
                } else {
                    // Single word search
                    List<InvertedIndex.PostingEntry> results = index.search(query);

                    if (results.isEmpty()) {
                        System.out.println("❌ No results found for: \"" + query + "\"");
                        System.out.println("💡 Try: protocol, internet, network, registry, etc.");
                    } else {
                        System.out.println("✅ Found in " + results.size() + " page(s):");

                        // Show top 10 results
                        for (int i = 0; i < Math.min(10, results.size()); i++) {
                            InvertedIndex.PostingEntry entry = results.get(i);
                            var pageOpt = pageDAO.getPageById(entry.getPageId());

                            if (pageOpt.isPresent()) {
                                Page page = pageOpt.get();
                                System.out.printf("\n   %d. [Page %d] %s%n",
                                    i + 1, entry.getPageId(), page.getUrl());
                                System.out.printf("      Title: %s%n",
                                    page.getTitle() != null ? page.getTitle() : "(no title)");
                                System.out.printf("      Frequency: %d occurrence(s)%n", entry.getFrequency());

                                // Show first few positions
                                List<Integer> positions = entry.getPositions();
                                if (positions.size() <= 5) {
                                    System.out.println("      Positions: " + positions);
                                } else {
                                    System.out.println("      Positions: " + positions.subList(0, 5) +
                                        "... (+" + (positions.size() - 5) + " more)");
                                }
                            }
                        }

                        if (results.size() > 10) {
                            System.out.println("\n   ... and " + (results.size() - 10) + " more results");
                        }
                    }
                }
            }

            scanner.close();

        } catch (Exception e) {
            logger.error("Error during search", e);
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

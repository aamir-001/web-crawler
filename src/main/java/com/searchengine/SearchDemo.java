package com.searchengine;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.PageDAO;
import com.searchengine.indexer.Indexer;
import com.searchengine.indexer.InvertedIndex;
import com.searchengine.search.SearchEngine;
import com.searchengine.search.SearchResult;
import com.searchengine.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive search demo with TF-IDF ranking and snippets.
 */
public class SearchDemo {
    private static final Logger logger = LoggerFactory.getLogger(SearchDemo.class);

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("INTERACTIVE SEARCH DEMO");
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
            indexer.indexAllPages();
            InvertedIndex index = indexer.getInvertedIndex();

            // Create search engine
            SearchEngine searchEngine = new SearchEngine(dbManager, indexer);

            System.out.println("Index loaded!");
            System.out.println("   - Total pages: " + pageDAO.getTotalPageCount());
            System.out.println("   - Unique words: " + index.getUniqueWordCount());
            System.out.println();
            System.out.println("Type words to search (or 'quit' to exit):");
            System.out.println("Results are ranked by TF-IDF relevance.");
            System.out.println("-".repeat(60));

            // Interactive search loop
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\nSearch: ");
                String query = scanner.nextLine().trim();

                if (query.isEmpty()) {
                    continue;
                }

                if (query.equals("quit") || query.equals("exit")) {
                    System.out.println("\nGoodbye!");
                    break;
                }

                // Use the new SearchEngine with TF-IDF ranking
                List<SearchResult> results = searchEngine.search(query);

                if (results.isEmpty()) {
                    System.out.println("No results found for: \"" + query + "\"");
                    System.out.println("Try: protocol, internet, network, domain, etc.");
                } else {
                    System.out.println("\nFound " + results.size() + " result(s):\n");

                    for (SearchResult result : results) {
                        System.out.printf("%d. %s%n", result.getRank(), result.getTitle());
                        System.out.printf("   URL: %s%n", result.getUrl());
                        System.out.printf("   Score: %.4f%n", result.getScore());

                        String snippet = result.getSnippet();
                        if (snippet != null && !snippet.isEmpty()) {
                            System.out.printf("   Snippet: %s%n", snippet);
                        }
                        System.out.println();
                    }
                }
            }

            scanner.close();
            dbManager.shutdown();

        } catch (Exception e) {
            logger.error("Error during search", e);
            System.err.println("Error: " + e.getMessage());
        }
    }
}

package com.searchengine.search;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import com.searchengine.indexer.Indexer;
import com.searchengine.indexer.InvertedIndex;
import com.searchengine.utils.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Main search engine that integrates query processing, ranking, and snippet generation.
 */
public class SearchEngine {
    private static final Logger logger = LoggerFactory.getLogger(SearchEngine.class);

    private final DatabaseManager dbManager;
    private final PageDAO pageDAO;
    private final Indexer indexer;
    private final InvertedIndex index;
    private final QueryProcessor queryProcessor;
    private final SnippetGenerator snippetGenerator;
    private final int maxResults;

    public SearchEngine(DatabaseManager dbManager, Indexer indexer) {
        this.dbManager = dbManager;
        this.pageDAO = new PageDAO(dbManager);
        this.indexer = indexer;
        this.index = indexer.getInvertedIndex();
        this.queryProcessor = new QueryProcessor();
        this.snippetGenerator = new SnippetGenerator();
        this.maxResults = ConfigLoader.getInt("search.max.results", 50);
    }

    /**
     * Search for pages matching the query.
     *
     * @param query The search query
     * @return List of ranked search results
     */
    public List<SearchResult> search(String query) {
        return search(query, maxResults);
    }

    /**
     * Search for pages matching the query with limit.
     *
     * @param query The search query
     * @param limit Maximum number of results to return
     * @return List of ranked search results
     */
    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        logger.info("Searching for: {}", query);

        // Process query into terms
        List<String> stemmedTerms = queryProcessor.process(query);
        List<String> originalTerms = queryProcessor.getOriginalTerms(query);

        if (stemmedTerms.isEmpty()) {
            logger.debug("No valid search terms after processing");
            return new ArrayList<>();
        }

        // Find matching pages using AND search
        List<Long> matchingPageIds = index.searchAnd(stemmedTerms);

        if (matchingPageIds.isEmpty()) {
            logger.debug("No pages found matching all terms");
            return new ArrayList<>();
        }

        // Get word counts for TF calculation
        Set<Long> pageIdSet = new HashSet<>(matchingPageIds);
        Map<Long, Integer> wordCounts = getWordCounts(pageIdSet);

        // Calculate TF-IDF scores
        int totalDocs = getTotalDocumentCount();
        TFIDFCalculator calculator = new TFIDFCalculator(index, totalDocs);
        Map<Long, Double> scores = calculator.rankPages(pageIdSet, stemmedTerms, wordCounts);

        // Build results
        List<SearchResult> results = new ArrayList<>();
        for (Long pageId : matchingPageIds) {
            try {
                Optional<Page> pageOpt = pageDAO.getPageById(pageId);
                if (pageOpt.isPresent()) {
                    Page page = pageOpt.get();
                    SearchResult result = new SearchResult(pageId, page.getUrl(), page.getTitle());
                    result.setScore(scores.getOrDefault(pageId, 0.0));

                    // Generate snippet
                    String snippet = snippetGenerator.generate(page.getContent(), originalTerms);
                    result.setSnippet(snippet);

                    results.add(result);
                }
            } catch (SQLException e) {
                logger.error("Error fetching page {}: {}", pageId, e.getMessage());
            }
        }

        // Sort by score (descending) and assign ranks
        Collections.sort(results);
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        // Apply limit
        if (results.size() > limit) {
            results = results.subList(0, limit);
        }

        logger.info("Found {} results for query: {}", results.size(), query);
        return results;
    }

    /**
     * Search with pagination support.
     *
     * @param query  The search query
     * @param page   Page number (1-based)
     * @param pageSize Number of results per page
     * @return List of search results for the requested page
     */
    public List<SearchResult> searchPaginated(String query, int page, int pageSize) {
        List<SearchResult> allResults = search(query, page * pageSize);

        int start = (page - 1) * pageSize;
        if (start >= allResults.size()) {
            return new ArrayList<>();
        }

        int end = Math.min(start + pageSize, allResults.size());
        return allResults.subList(start, end);
    }

    /**
     * Get word counts for pages (for TF normalization).
     */
    private Map<Long, Integer> getWordCounts(Set<Long> pageIds) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Long pageId : pageIds) {
            try {
                Optional<Page> pageOpt = pageDAO.getPageById(pageId);
                pageOpt.ifPresent(page -> counts.put(pageId, page.getWordCount()));
            } catch (SQLException e) {
                logger.error("Error getting word count for page {}", pageId, e);
            }
        }
        return counts;
    }

    /**
     * Get total document count for IDF calculation.
     */
    private int getTotalDocumentCount() {
        try {
            return pageDAO.getTotalPageCount();
        } catch (SQLException e) {
            logger.error("Error getting total page count", e);
            return 1;
        }
    }

    /**
     * Get statistics about the search engine.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("uniqueWords", index.getUniqueWordCount());
        stats.put("totalOccurrences", index.getTotalWordCount());

        try {
            stats.put("totalPages", pageDAO.getTotalPageCount());
        } catch (SQLException e) {
            stats.put("totalPages", 0);
        }

        return stats;
    }
}

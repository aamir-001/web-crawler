package com.searchengine.search;

import com.searchengine.indexer.Stemmer;
import com.searchengine.indexer.StopWordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Processes search queries by normalizing, stemming, and filtering.
 */
public class QueryProcessor {
    private static final Logger logger = LoggerFactory.getLogger(QueryProcessor.class);
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]{2,}");

    private final Stemmer stemmer;
    private final StopWordFilter stopWordFilter;

    public QueryProcessor() {
        this.stemmer = new Stemmer();
        this.stopWordFilter = new StopWordFilter();
    }

    /**
     * Process a search query into normalized, stemmed terms.
     *
     * @param query The raw search query
     * @return List of processed query terms
     */
    public List<String> process(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> terms = new ArrayList<>();
        String normalized = query.toLowerCase().trim();

        // Extract words using regex
        var matcher = WORD_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String word = matcher.group();

            // Skip stop words
            if (stopWordFilter.isStopWord(word)) {
                continue;
            }

            // Apply stemming
            String stemmed = stemmer.stem(word);
            if (stemmed != null && stemmed.length() >= 2) {
                terms.add(stemmed);
            }
        }

        logger.debug("Processed query '{}' into terms: {}", query, terms);
        return terms;
    }

    /**
     * Get original terms without stemming (for snippet highlighting).
     *
     * @param query The raw search query
     * @return List of original query terms (lowercase, no stop words)
     */
    public List<String> getOriginalTerms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> terms = new ArrayList<>();
        String normalized = query.toLowerCase().trim();

        var matcher = WORD_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String word = matcher.group();
            if (!stopWordFilter.isStopWord(word)) {
                terms.add(word);
            }
        }

        return terms;
    }
}

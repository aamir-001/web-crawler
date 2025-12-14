package com.searchengine.search;

import com.searchengine.indexer.InvertedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates TF-IDF scores for ranking search results.
 *
 * TF-IDF = Term Frequency * Inverse Document Frequency
 * - TF: How often term appears in document / total terms in document
 * - IDF: log(total documents / documents containing term)
 */
public class TFIDFCalculator {
    private static final Logger logger = LoggerFactory.getLogger(TFIDFCalculator.class);

    private final InvertedIndex index;
    private final int totalDocuments;

    public TFIDFCalculator(InvertedIndex index, int totalDocuments) {
        this.index = index;
        this.totalDocuments = Math.max(1, totalDocuments);
    }

    /**
     * Calculate TF-IDF score for a page given query terms.
     *
     * @param pageId     The page ID to score
     * @param queryTerms The stemmed query terms
     * @param wordCounts Map of pageId to word count (for TF normalization)
     * @return The TF-IDF score
     */
    public double calculateScore(long pageId, List<String> queryTerms, Map<Long, Integer> wordCounts) {
        if (queryTerms == null || queryTerms.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        int wordCount = wordCounts.getOrDefault(pageId, 1);

        for (String term : queryTerms) {
            double tf = calculateTF(pageId, term, wordCount);
            double idf = calculateIDF(term);
            totalScore += tf * idf;
        }

        return totalScore;
    }

    /**
     * Calculate Term Frequency for a term in a document.
     * TF = term frequency in document / total terms in document
     */
    private double calculateTF(long pageId, String term, int totalWords) {
        int termFrequency = getTermFrequency(pageId, term);
        if (termFrequency == 0 || totalWords == 0) {
            return 0.0;
        }
        return (double) termFrequency / totalWords;
    }

    /**
     * Calculate Inverse Document Frequency for a term.
     * IDF = log(total documents / documents containing term)
     */
    private double calculateIDF(String term) {
        int docFrequency = index.getDocumentFrequency(term);
        if (docFrequency == 0) {
            return 0.0;
        }
        return Math.log((double) totalDocuments / docFrequency);
    }

    /**
     * Get frequency of a term in a specific document.
     */
    private int getTermFrequency(long pageId, String term) {
        List<InvertedIndex.PostingEntry> postings = index.search(term);
        if (postings == null) {
            return 0;
        }

        for (InvertedIndex.PostingEntry entry : postings) {
            if (entry.getPageId() == pageId) {
                return entry.getFrequency();
            }
        }
        return 0;
    }

    /**
     * Calculate and rank all results for given query terms.
     *
     * @param pageIds    Set of page IDs to rank
     * @param queryTerms Stemmed query terms
     * @param wordCounts Map of pageId to word count
     * @return Map of pageId to TF-IDF score
     */
    public Map<Long, Double> rankPages(Iterable<Long> pageIds, List<String> queryTerms,
                                       Map<Long, Integer> wordCounts) {
        Map<Long, Double> scores = new HashMap<>();

        for (Long pageId : pageIds) {
            double score = calculateScore(pageId, queryTerms, wordCounts);
            if (score > 0) {
                scores.put(pageId, score);
            }
        }

        return scores;
    }
}

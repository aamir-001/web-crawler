package com.searchengine.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * StopWordFilter removes common words (stop words) from token lists.
 * Stop words are loaded from stopwords.txt in resources.
 */
public class StopWordFilter {

    private static final Logger logger = LoggerFactory.getLogger(StopWordFilter.class);
    private static final String STOPWORDS_FILE = "/stopwords.txt";

    private final Set<String> stopWords;

    /**
     * Constructor loads stop words from resources.
     */
    public StopWordFilter() {
        this.stopWords = loadStopWords();
        logger.info("Loaded {} stop words", stopWords.size());
    }

    /**
     * Load stop words from the stopwords.txt file in resources.
     *
     * @return Set of stop words (lowercase)
     */
    private Set<String> loadStopWords() {
        Set<String> words = new HashSet<>();

        try (InputStream is = getClass().getResourceAsStream(STOPWORDS_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    words.add(line);
                }
            }

        } catch (IOException | NullPointerException e) {
            logger.warn("Could not load stop words from {}: {}", STOPWORDS_FILE, e.getMessage());
            // Return default stop words if file can't be loaded
            return getDefaultStopWords();
        }

        return words;
    }

    /**
     * Default stop words in case file loading fails.
     *
     * @return Set of common English stop words
     */
    private Set<String> getDefaultStopWords() {
        Set<String> defaults = new HashSet<>();
        String[] commonStopWords = {
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "will", "with"
        };
        for (String word : commonStopWords) {
            defaults.add(word);
        }
        return defaults;
    }

    /**
     * Check if a word is a stop word.
     *
     * @param word The word to check (case-insensitive)
     * @return true if the word is a stop word
     */
    public boolean isStopWord(String word) {
        return word != null && stopWords.contains(word.toLowerCase());
    }

    /**
     * Filter stop words from a list of tokens.
     *
     * @param tokens List of tokens to filter
     * @return New list with stop words removed
     */
    public List<String> filter(List<String> tokens) {
        if (tokens == null) {
            return List.of();
        }

        return tokens.stream()
                .filter(token -> !isStopWord(token))
                .collect(Collectors.toList());
    }

    /**
     * Filter stop words from a list of TokenPosition objects.
     *
     * @param tokenPositions List of TokenPosition objects
     * @return New list with stop words removed
     */
    public List<Tokenizer.TokenPosition> filterPositions(List<Tokenizer.TokenPosition> tokenPositions) {
        if (tokenPositions == null) {
            return List.of();
        }

        return tokenPositions.stream()
                .filter(tp -> !isStopWord(tp.getToken()))
                .collect(Collectors.toList());
    }

    /**
     * Get the number of stop words loaded.
     *
     * @return Number of stop words
     */
    public int getStopWordCount() {
        return stopWords.size();
    }

    /**
     * Check if stop words were loaded successfully.
     *
     * @return true if stop words are available
     */
    public boolean isLoaded() {
        return !stopWords.isEmpty();
    }
}

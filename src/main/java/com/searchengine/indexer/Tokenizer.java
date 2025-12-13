package com.searchengine.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizer splits text into individual words/tokens.
 * Handles basic text normalization and filtering.
 */
public class Tokenizer {

    // Pattern to match words (alphanumeric sequences)
    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

    // Minimum word length to consider (filters out single characters)
    private static final int MIN_WORD_LENGTH = 2;

    // Maximum word length to consider (filters out garbage)
    private static final int MAX_WORD_LENGTH = 50;

    /**
     * Tokenize text into a list of normalized words.
     *
     * @param text The text to tokenize
     * @return List of lowercase words
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return tokens;
        }

        // Convert to lowercase for case-insensitive indexing
        String lowerText = text.toLowerCase();

        // Extract words using regex
        Matcher matcher = WORD_PATTERN.matcher(lowerText);

        while (matcher.find()) {
            String word = matcher.group();

            // Filter by length
            if (word.length() >= MIN_WORD_LENGTH && word.length() <= MAX_WORD_LENGTH) {
                // Filter out pure numbers (optional - you can remove this if you want to index numbers)
                if (!word.matches("\\d+")) {
                    tokens.add(word);
                }
            }
        }

        return tokens;
    }

    /**
     * Tokenize text and return tokens with their positions.
     * Position is useful for phrase queries and proximity ranking.
     *
     * @param text The text to tokenize
     * @return List of TokenPosition objects
     */
    public List<TokenPosition> tokenizeWithPositions(String text) {
        List<TokenPosition> tokenPositions = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return tokenPositions;
        }

        String lowerText = text.toLowerCase();
        Matcher matcher = WORD_PATTERN.matcher(lowerText);

        int position = 0;
        while (matcher.find()) {
            String word = matcher.group();

            if (word.length() >= MIN_WORD_LENGTH && word.length() <= MAX_WORD_LENGTH) {
                if (!word.matches("\\d+")) {
                    tokenPositions.add(new TokenPosition(word, position, matcher.start()));
                    position++;
                }
            }
        }

        return tokenPositions;
    }

    /**
     * Represents a token with its position in the text.
     */
    public static class TokenPosition {
        private final String token;
        private final int position;      // Sequential position (0, 1, 2, ...)
        private final int charOffset;    // Character offset in original text

        public TokenPosition(String token, int position, int charOffset) {
            this.token = token;
            this.position = position;
            this.charOffset = charOffset;
        }

        public String getToken() {
            return token;
        }

        public int getPosition() {
            return position;
        }

        public int getCharOffset() {
            return charOffset;
        }

        @Override
        public String toString() {
            return String.format("%s@%d", token, position);
        }
    }
}

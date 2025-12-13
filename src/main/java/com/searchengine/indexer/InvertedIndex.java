package com.searchengine.indexer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InvertedIndex is the core data structure for fast keyword search.
 *
 * Structure: Map<Word, List<PostingEntry>>
 * - Word: A stemmed, normalized keyword
 * - PostingEntry: Contains page ID, term frequency, and positions
 *
 * Example:
 * "java" -> [
 *   {pageId: 1, frequency: 5, positions: [10, 25, 42, 88, 120]},
 *   {pageId: 3, frequency: 2, positions: [5, 67]},
 *   {pageId: 7, frequency: 1, positions: [33]}
 * ]
 */
public class InvertedIndex {

    // Thread-safe map for concurrent access during indexing
    private final Map<String, List<PostingEntry>> index;

    // Statistics
    private long totalWords = 0;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }

    /**
     * Add a word occurrence to the index.
     *
     * @param word The word (already stemmed and normalized)
     * @param pageId The ID of the page containing the word
     * @param position The position of the word in the page
     */
    public synchronized void addWord(String word, long pageId, int position) {
        if (word == null || word.isEmpty()) {
            return;
        }

        // Get or create the posting list for this word
        List<PostingEntry> postings = index.computeIfAbsent(word, k -> new ArrayList<>());

        // Find existing entry for this page, or create new one
        PostingEntry entry = findOrCreateEntry(postings, pageId);

        // Add position and increment frequency
        entry.addPosition(position);

        totalWords++;
    }

    /**
     * Add multiple words from a page to the index.
     *
     * @param words List of words with their positions
     * @param pageId The ID of the page
     */
    public void addWords(List<Tokenizer.TokenPosition> words, long pageId) {
        for (Tokenizer.TokenPosition tp : words) {
            addWord(tp.getToken(), pageId, tp.getPosition());
        }
    }

    /**
     * Find an existing PostingEntry for a page, or create a new one.
     */
    private PostingEntry findOrCreateEntry(List<PostingEntry> postings, long pageId) {
        for (PostingEntry entry : postings) {
            if (entry.getPageId() == pageId) {
                return entry;
            }
        }

        // Create new entry
        PostingEntry newEntry = new PostingEntry(pageId);
        postings.add(newEntry);
        return newEntry;
    }

    /**
     * Search for pages containing a word.
     *
     * @param word The word to search for
     * @return List of PostingEntry objects, or empty list if not found
     */
    public List<PostingEntry> search(String word) {
        if (word == null || word.isEmpty()) {
            return Collections.emptyList();
        }

        List<PostingEntry> postings = index.get(word.toLowerCase());
        return postings != null ? new ArrayList<>(postings) : Collections.emptyList();
    }

    /**
     * Search for pages containing ALL the given words (AND query).
     *
     * @param words List of words to search for
     * @return List of page IDs that contain all words
     */
    public List<Long> searchAnd(List<String> words) {
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        // Get postings for the first word
        List<PostingEntry> firstPostings = search(words.get(0));
        if (firstPostings.isEmpty()) {
            return Collections.emptyList();
        }

        // Start with pages from first word
        Set<Long> resultPages = new HashSet<>();
        for (PostingEntry entry : firstPostings) {
            resultPages.add(entry.getPageId());
        }

        // Intersect with pages from remaining words
        for (int i = 1; i < words.size(); i++) {
            List<PostingEntry> postings = search(words.get(i));
            Set<Long> currentPages = new HashSet<>();
            for (PostingEntry entry : postings) {
                currentPages.add(entry.getPageId());
            }

            // Keep only pages that exist in both sets
            resultPages.retainAll(currentPages);

            if (resultPages.isEmpty()) {
                return Collections.emptyList();
            }
        }

        return new ArrayList<>(resultPages);
    }

    /**
     * Search for pages containing ANY of the given words (OR query).
     *
     * @param words List of words to search for
     * @return List of page IDs that contain any of the words
     */
    public List<Long> searchOr(List<String> words) {
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> resultPages = new HashSet<>();

        for (String word : words) {
            List<PostingEntry> postings = search(word);
            for (PostingEntry entry : postings) {
                resultPages.add(entry.getPageId());
            }
        }

        return new ArrayList<>(resultPages);
    }

    /**
     * Get the number of unique words in the index.
     */
    public int getUniqueWordCount() {
        return index.size();
    }

    /**
     * Get the total number of word occurrences indexed.
     */
    public long getTotalWordCount() {
        return totalWords;
    }

    /**
     * Get the number of pages containing a specific word.
     */
    public int getDocumentFrequency(String word) {
        List<PostingEntry> postings = index.get(word);
        return postings != null ? postings.size() : 0;
    }

    /**
     * Clear the index.
     */
    public void clear() {
        index.clear();
        totalWords = 0;
    }

    /**
     * Get statistics about the index.
     */
    public IndexStats getStats() {
        return new IndexStats(
            index.size(),
            totalWords,
            calculateAveragePostingListSize()
        );
    }

    private double calculateAveragePostingListSize() {
        if (index.isEmpty()) {
            return 0.0;
        }

        long totalPostings = 0;
        for (List<PostingEntry> postings : index.values()) {
            totalPostings += postings.size();
        }

        return (double) totalPostings / index.size();
    }

    /**
     * PostingEntry represents a single occurrence of a word in a document.
     */
    public static class PostingEntry {
        private final long pageId;
        private int frequency;
        private final List<Integer> positions;

        public PostingEntry(long pageId) {
            this.pageId = pageId;
            this.frequency = 0;
            this.positions = new ArrayList<>();
        }

        public void addPosition(int position) {
            positions.add(position);
            frequency++;
        }

        public long getPageId() {
            return pageId;
        }

        public int getFrequency() {
            return frequency;
        }

        public List<Integer> getPositions() {
            return Collections.unmodifiableList(positions);
        }

        @Override
        public String toString() {
            return String.format("Page %d: freq=%d, positions=%s", pageId, frequency, positions);
        }
    }

    /**
     * Statistics about the inverted index.
     */
    public static class IndexStats {
        private final int uniqueWords;
        private final long totalWords;
        private final double avgPostingListSize;

        public IndexStats(int uniqueWords, long totalWords, double avgPostingListSize) {
            this.uniqueWords = uniqueWords;
            this.totalWords = totalWords;
            this.avgPostingListSize = avgPostingListSize;
        }

        public int getUniqueWords() {
            return uniqueWords;
        }

        public long getTotalWords() {
            return totalWords;
        }

        public double getAvgPostingListSize() {
            return avgPostingListSize;
        }

        @Override
        public String toString() {
            return String.format(
                "Unique words: %d, Total words: %d, Avg posting list size: %.2f",
                uniqueWords, totalWords, avgPostingListSize
            );
        }
    }
}

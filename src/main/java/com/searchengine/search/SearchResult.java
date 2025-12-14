package com.searchengine.search;

/**
 * Represents a search result with relevance scoring.
 */
public class SearchResult implements Comparable<SearchResult> {
    private final long pageId;
    private final String url;
    private final String title;
    private String snippet;
    private double score;
    private int rank;

    public SearchResult(long pageId, String url, String title) {
        this.pageId = pageId;
        this.url = url;
        this.title = title != null ? title : "Untitled";
        this.snippet = "";
        this.score = 0.0;
        this.rank = 0;
    }

    public long getPageId() {
        return pageId;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public int compareTo(SearchResult other) {
        // Higher score = better, so reverse order
        return Double.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("SearchResult{rank=%d, score=%.4f, title='%s', url='%s'}",
                rank, score, title, url);
    }
}

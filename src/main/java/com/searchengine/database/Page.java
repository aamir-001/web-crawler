package com.searchengine.database;

/**
 * Represents a crawled web page
 */
public class Page {
    private Long pageId;
    private String url;
    private String title;
    private String content;
    private long crawlTimestamp;
    private int wordCount;
    private int depth;

    public Page() {
    }

    public Page(String url, String title, String content, int depth) {
        this.url = url;
        this.title = title;
        this.content = content;
        this.crawlTimestamp = System.currentTimeMillis();
        this.depth = depth;
        this.wordCount = 0;
    }

    // Getters and setters
    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCrawlTimestamp() {
        return crawlTimestamp;
    }

    public void setCrawlTimestamp(long crawlTimestamp) {
        this.crawlTimestamp = crawlTimestamp;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        return "Page{" +
                "pageId=" + pageId +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", depth=" + depth +
                ", wordCount=" + wordCount +
                '}';
    }
}

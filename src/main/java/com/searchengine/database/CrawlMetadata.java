package com.searchengine.database;

/**
 * Represents metadata about a crawl session
 */
public class CrawlMetadata {
    private Long crawlId;
    private String startUrl;
    private int maxDepth;
    private int pagesCrawled;
    private long startTime;
    private Long endTime;
    private String status; // "running", "completed", "stopped", "error"

    public CrawlMetadata() {
    }

    public CrawlMetadata(String startUrl, int maxDepth) {
        this.startUrl = startUrl;
        this.maxDepth = maxDepth;
        this.startTime = System.currentTimeMillis();
        this.pagesCrawled = 0;
        this.status = "running";
    }

    // Getters and setters
    public Long getCrawlId() {
        return crawlId;
    }

    public void setCrawlId(Long crawlId) {
        this.crawlId = crawlId;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getPagesCrawled() {
        return pagesCrawled;
    }

    public void setPagesCrawled(int pagesCrawled) {
        this.pagesCrawled = pagesCrawled;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "CrawlMetadata{" +
                "crawlId=" + crawlId +
                ", startUrl='" + startUrl + '\'' +
                ", maxDepth=" + maxDepth +
                ", pagesCrawled=" + pagesCrawled +
                ", status='" + status + '\'' +
                '}';
    }
}

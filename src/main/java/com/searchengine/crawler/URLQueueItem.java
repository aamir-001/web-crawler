package com.searchengine.crawler;

/**
 * Represents a URL in the crawl queue with its depth
 */
public class URLQueueItem {
    private final String url;
    private final int depth;

    public URLQueueItem(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    public String getUrl() {
        return url;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return "URLQueueItem{url='" + url + "', depth=" + depth + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        URLQueueItem that = (URLQueueItem) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}

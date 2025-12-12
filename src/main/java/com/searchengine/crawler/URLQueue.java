package com.searchengine.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * Thread-safe URL queue for the crawler
 * Tracks visited URLs to avoid duplicates
 */
public class URLQueue {
    private static final Logger logger = LoggerFactory.getLogger(URLQueue.class);

    private final BlockingQueue<URLQueueItem> queue;
    private final Set<String> visited;
    private final Set<String> queued;

    public URLQueue() {
        this.queue = new LinkedBlockingQueue<>();
        this.visited = ConcurrentHashMap.newKeySet();
        this.queued = ConcurrentHashMap.newKeySet();
    }

    /**
     * Add a URL to the queue if not already visited or queued
     * @return true if URL was added, false if already seen
     */
    public boolean add(String url, int depth) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // Check if already visited or queued
        if (visited.contains(url) || queued.contains(url)) {
            return false;
        }

        // Mark as queued and add to queue
        if (queued.add(url)) {
            queue.offer(new URLQueueItem(url, depth));
            logger.debug("Added to queue: {} (depth: {})", url, depth);
            return true;
        }

        return false;
    }

    /**
     * Get the next URL from the queue (blocking)
     * @return next URL or null if interrupted
     */
    public URLQueueItem poll() {
        try {
            URLQueueItem item = queue.take();
            // Move from queued to visited
            queued.remove(item.getUrl());
            visited.add(item.getUrl());
            return item;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Queue poll interrupted");
            return null;
        }
    }

    /**
     * Check if URL has been visited
     */
    public boolean isVisited(String url) {
        return visited.contains(url);
    }

    /**
     * Check if URL is in queue
     */
    public boolean isQueued(String url) {
        return queued.contains(url);
    }

    /**
     * Get current queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Check if queue is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Get number of visited URLs
     */
    public int getVisitedCount() {
        return visited.size();
    }

    /**
     * Clear the queue and visited set
     */
    public void clear() {
        queue.clear();
        visited.clear();
        queued.clear();
        logger.info("Queue cleared");
    }
}

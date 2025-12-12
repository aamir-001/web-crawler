package com.searchengine.crawler;

import com.searchengine.database.*;
import com.searchengine.utils.ConfigLoader;
import com.searchengine.utils.URLNormalizer;
import com.searchengine.utils.URLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main web crawler orchestrator
 * Manages thread pool and crawling process
 */
public class WebCrawler implements CrawlerTask.CrawlerListener {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    private final DatabaseManager dbManager;
    private final PageDAO pageDAO;
    private final CrawlMetadataDAO metadataDAO;
    private final URLQueue urlQueue;
    private final RobotsTxtParser robotsParser;

    private ExecutorService executorService;
    private final int threadPoolSize;
    private final int maxPages;
    private final AtomicInteger pagesCrawled;
    private final AtomicBoolean isRunning;
    private CrawlMetadata currentCrawlMetadata;

    private CrawlerProgressListener progressListener;

    public WebCrawler(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.pageDAO = new PageDAO(dbManager);
        this.metadataDAO = new CrawlMetadataDAO(dbManager);
        this.urlQueue = new URLQueue();
        this.robotsParser = new RobotsTxtParser();

        this.threadPoolSize = ConfigLoader.getInt("crawler.thread.pool.size", 10);
        this.maxPages = ConfigLoader.getInt("crawler.max.pages", 500);
        this.pagesCrawled = new AtomicInteger(0);
        this.isRunning = new AtomicBoolean(false);
    }

    /**
     * Start crawling from a starting URL
     */
    public void startCrawl(String startUrl, int maxDepth) {
        if (isRunning.get()) {
            logger.warn("Crawler is already running");
            return;
        }

        // Validate and normalize URL
        if (!URLValidator.isValid(startUrl)) {
            logger.error("Invalid starting URL: {}", startUrl);
            throw new IllegalArgumentException("Invalid starting URL: " + startUrl);
        }

        String normalizedUrl = URLNormalizer.normalize(startUrl);
        if (normalizedUrl == null) {
            logger.error("Failed to normalize URL: {}", startUrl);
            throw new IllegalArgumentException("Failed to normalize URL: " + startUrl);
        }

        logger.info("Starting crawl from: {} with max depth: {}", normalizedUrl, maxDepth);

        // Reset state
        urlQueue.clear();
        pagesCrawled.set(0);
        isRunning.set(true);

        // Create crawl metadata
        currentCrawlMetadata = new CrawlMetadata(normalizedUrl, maxDepth);
        try {
            metadataDAO.insertCrawlMetadata(currentCrawlMetadata);
        } catch (SQLException e) {
            logger.error("Failed to save crawl metadata", e);
        }

        // Initialize thread pool
        executorService = Executors.newFixedThreadPool(threadPoolSize);

        // Add starting URL to queue
        urlQueue.add(normalizedUrl, 0);

        // Start crawler threads
        for (int i = 0; i < threadPoolSize; i++) {
            executorService.submit(new CrawlerWorker(maxDepth));
        }

        logger.info("Crawler started with {} threads", threadPoolSize);

        // Notify progress listener
        if (progressListener != null) {
            progressListener.onCrawlStarted(normalizedUrl, maxDepth);
        }
    }

    /**
     * Stop the crawler
     */
    public void stopCrawl() {
        if (!isRunning.get()) {
            logger.warn("Crawler is not running");
            return;
        }

        logger.info("Stopping crawler...");
        isRunning.set(false);

        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for crawler to stop", e);
            }
        }

        // Update crawl metadata
        if (currentCrawlMetadata != null) {
            currentCrawlMetadata.setEndTime(System.currentTimeMillis());
            currentCrawlMetadata.setStatus("stopped");
            currentCrawlMetadata.setPagesCrawled(pagesCrawled.get());
            try {
                metadataDAO.updateCrawlMetadata(currentCrawlMetadata);
            } catch (SQLException e) {
                logger.error("Failed to update crawl metadata", e);
            }
        }

        logger.info("Crawler stopped. Pages crawled: {}", pagesCrawled.get());

        // Notify progress listener
        if (progressListener != null) {
            progressListener.onCrawlStopped(pagesCrawled.get());
        }
    }

    /**
     * Check if crawler is running
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Get number of pages crawled
     */
    public int getPagesCrawled() {
        return pagesCrawled.get();
    }

    /**
     * Get current queue size
     */
    public int getQueueSize() {
        return urlQueue.size();
    }

    /**
     * Set progress listener
     */
    public void setProgressListener(CrawlerProgressListener listener) {
        this.progressListener = listener;
    }

    // CrawlerListener implementation
    @Override
    public void onCrawlStart(String url, int depth) {
        if (progressListener != null) {
            progressListener.onPageCrawlStart(url, depth);
        }
    }

    @Override
    public void onCrawlSuccess(String url, int depth, Long pageId) {
        int count = pagesCrawled.incrementAndGet();
        logger.info("Progress: {}/{} pages crawled", count, maxPages);

        if (progressListener != null) {
            progressListener.onPageCrawlSuccess(url, depth, pageId, count);
        }

        // Check if we've reached max pages
        if (count >= maxPages) {
            logger.info("Reached maximum pages limit: {}", maxPages);
            stopCrawl();
        }
    }

    @Override
    public void onCrawlError(String url, int depth, Exception e) {
        if (progressListener != null) {
            progressListener.onPageCrawlError(url, depth, e);
        }
    }

    @Override
    public void onCrawlSkipped(String url, String reason) {
        if (progressListener != null) {
            progressListener.onPageCrawlSkipped(url, reason);
        }
    }

    /**
     * Worker thread that continuously processes URLs from the queue
     */
    private class CrawlerWorker implements Runnable {
        private final int maxDepth;

        public CrawlerWorker(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        @Override
        public void run() {
            logger.debug("Crawler worker started: {}", Thread.currentThread().getName());

            while (isRunning.get() && pagesCrawled.get() < maxPages) {
                URLQueueItem item = urlQueue.poll();

                if (item == null) {
                    // Queue is empty, check if we should stop
                    if (urlQueue.isEmpty() && urlQueue.size() == 0) {
                        logger.info("Queue is empty, worker finishing");
                        break;
                    }
                    continue;
                }

                // Create and run crawler task
                CrawlerTask task = new CrawlerTask(
                        item,
                        urlQueue,
                        pageDAO,
                        robotsParser,
                        maxDepth,
                        WebCrawler.this
                );

                task.run();
            }

            logger.debug("Crawler worker finished: {}", Thread.currentThread().getName());

            // Check if all workers are done
            if (urlQueue.isEmpty() && isRunning.get()) {
                isRunning.set(false);

                // Update crawl metadata
                if (currentCrawlMetadata != null) {
                    currentCrawlMetadata.setEndTime(System.currentTimeMillis());
                    currentCrawlMetadata.setStatus("completed");
                    currentCrawlMetadata.setPagesCrawled(pagesCrawled.get());
                    try {
                        metadataDAO.updateCrawlMetadata(currentCrawlMetadata);
                    } catch (SQLException e) {
                        logger.error("Failed to update crawl metadata", e);
                    }
                }

                logger.info("Crawl completed. Total pages: {}", pagesCrawled.get());

                // Notify progress listener
                if (progressListener != null) {
                    progressListener.onCrawlCompleted(pagesCrawled.get());
                }
            }
        }
    }

    /**
     * Interface for listening to crawler progress
     */
    public interface CrawlerProgressListener {
        void onCrawlStarted(String startUrl, int maxDepth);
        void onPageCrawlStart(String url, int depth);
        void onPageCrawlSuccess(String url, int depth, Long pageId, int totalCrawled);
        void onPageCrawlError(String url, int depth, Exception e);
        void onPageCrawlSkipped(String url, String reason);
        void onCrawlCompleted(int totalPages);
        void onCrawlStopped(int totalPages);
    }
}

package com.searchengine.crawler;

import com.searchengine.database.Page;
import com.searchengine.database.PageDAO;
import com.searchengine.utils.ConfigLoader;
import com.searchengine.utils.URLNormalizer;
import com.searchengine.utils.URLValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Runnable task for crawling a single URL
 */
public class CrawlerTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerTask.class);

    private final URLQueueItem item;
    private final URLQueue urlQueue;
    private final PageDAO pageDAO;
    private final RobotsTxtParser robotsParser;
    private final int maxDepth;
    private final int timeout;
    private final String userAgent;
    private final CrawlerListener listener;

    public CrawlerTask(URLQueueItem item,
                       URLQueue urlQueue,
                       PageDAO pageDAO,
                       RobotsTxtParser robotsParser,
                       int maxDepth,
                       CrawlerListener listener) {
        this.item = item;
        this.urlQueue = urlQueue;
        this.pageDAO = pageDAO;
        this.robotsParser = robotsParser;
        this.maxDepth = maxDepth;
        this.listener = listener;
        this.timeout = ConfigLoader.getInt("crawler.request.timeout", 30000);
        this.userAgent = ConfigLoader.getString("crawler.user.agent", "DesktopSearchBot/1.0");
    }

    @Override
    public void run() {
        String url = item.getUrl();
        int depth = item.getDepth();

        try {
            logger.info("Crawling: {} (depth: {})", url, depth);

            // Notify listener
            if (listener != null) {
                listener.onCrawlStart(url, depth);
            }

            // Check robots.txt
            if (!robotsParser.isAllowed(url)) {
                logger.info("URL disallowed by robots.txt: {}", url);
                if (listener != null) {
                    listener.onCrawlSkipped(url, "Disallowed by robots.txt");
                }
                return;
            }

            // Fetch and parse the page
            Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(timeout)
                    .get();

            // Extract content
            String title = doc.title();
            String content = doc.body().text();

            // Create and save page
            Page page = new Page(url, title, content, depth);
            try {
                Long pageId = pageDAO.insertPage(page);
                logger.info("Saved page: {} (ID: {})", url, pageId);

                // Notify listener
                if (listener != null) {
                    listener.onCrawlSuccess(url, depth, pageId);
                }

            } catch (SQLException e) {
                // Check if it's a duplicate URL error
                if (e.getMessage().contains("UNIQUE constraint failed")) {
                    logger.debug("Page already exists: {}", url);
                } else {
                    logger.error("Failed to save page: {}", url, e);
                    if (listener != null) {
                        listener.onCrawlError(url, depth, e);
                    }
                }
            }

            // Extract and queue links if not at max depth
            if (depth < maxDepth) {
                Set<String> links = extractLinks(doc, url);
                int addedCount = 0;

                for (String link : links) {
                    if (urlQueue.add(link, depth + 1)) {
                        addedCount++;
                    }
                }

                logger.debug("Found {} links, added {} to queue", links.size(), addedCount);
            }

            // Politeness delay
            int delay = ConfigLoader.getInt("crawler.delay.between.requests", 1000);
            if (delay > 0) {
                Thread.sleep(delay);
            }

        } catch (Exception e) {
            logger.error("Error crawling URL: {}", url, e);
            if (listener != null) {
                listener.onCrawlError(url, depth, e);
            }
        }
    }

    /**
     * Extract all valid links from a document
     */
    private Set<String> extractLinks(Document doc, String baseUrl) {
        Set<String> links = new HashSet<>();

        Elements linkElements = doc.select("a[href]");
        for (Element link : linkElements) {
            String href = link.attr("abs:href");

            if (href.isEmpty()) {
                continue;
            }

            // Normalize URL
            String normalized = URLNormalizer.normalize(href);
            if (normalized == null) {
                continue;
            }

            // Validate URL
            if (!URLValidator.isValid(normalized)) {
                continue;
            }

            // Only crawl same domain (optional - can be configured)
            // For now, we allow all domains
            // if (!URLNormalizer.isSameDomain(baseUrl, normalized)) {
            //     continue;
            // }

            links.add(normalized);
        }

        return links;
    }

    /**
     * Listener interface for crawler events
     */
    public interface CrawlerListener {
        void onCrawlStart(String url, int depth);
        void onCrawlSuccess(String url, int depth, Long pageId);
        void onCrawlError(String url, int depth, Exception e);
        void onCrawlSkipped(String url, String reason);
    }
}

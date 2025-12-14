package com.searchengine.gui;

import com.searchengine.crawler.WebCrawler;
import com.searchengine.database.DatabaseManager;
import com.searchengine.database.PageDAO;
import com.searchengine.utils.ConfigLoader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Crawler control panel with URL input, progress tracking, and status log.
 */
public class CrawlerPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerPanel.class);

    private final DatabaseManager dbManager;
    private final Consumer<String> statusUpdater;

    private TextField urlField;
    private Spinner<Integer> depthSpinner;
    private Spinner<Integer> maxPagesSpinner;
    private Button startButton;
    private Button stopButton;
    private ProgressBar progressBar;
    private Label progressLabel;
    private TextArea logArea;
    private Label statsLabel;

    private WebCrawler crawler;
    private Thread crawlerThread;
    private volatile boolean isCrawling = false;

    public CrawlerPanel(DatabaseManager dbManager, Consumer<String> statusUpdater) {
        this.dbManager = dbManager;
        this.statusUpdater = statusUpdater;

        setPadding(new Insets(15));
        setSpacing(15);

        getChildren().addAll(
            createInputSection(),
            createControlSection(),
            createProgressSection(),
            createLogSection(),
            createStatsSection()
        );

        updateStats();
    }

    private VBox createInputSection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("Web Crawler");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // URL input
        HBox urlBox = new HBox(10);
        urlBox.setAlignment(Pos.CENTER_LEFT);
        Label urlLabel = new Label("Start URL:");
        urlLabel.setMinWidth(80);
        urlField = new TextField("https://example.com");
        urlField.setPrefWidth(400);
        urlField.setPromptText("Enter URL to crawl");
        urlBox.getChildren().addAll(urlLabel, urlField);

        // Depth input
        HBox depthBox = new HBox(10);
        depthBox.setAlignment(Pos.CENTER_LEFT);
        Label depthLabel = new Label("Max Depth:");
        depthLabel.setMinWidth(80);
        depthSpinner = new Spinner<>(1, 10, ConfigLoader.getInt("crawler.default.depth", 3));
        depthSpinner.setEditable(true);
        depthSpinner.setPrefWidth(80);
        depthBox.getChildren().addAll(depthLabel, depthSpinner);

        // Max pages input
        HBox pagesBox = new HBox(10);
        pagesBox.setAlignment(Pos.CENTER_LEFT);
        Label pagesLabel = new Label("Max Pages:");
        pagesLabel.setMinWidth(80);
        maxPagesSpinner = new Spinner<>(10, 1000, ConfigLoader.getInt("crawler.max.pages", 100), 10);
        maxPagesSpinner.setEditable(true);
        maxPagesSpinner.setPrefWidth(80);
        pagesBox.getChildren().addAll(pagesLabel, maxPagesSpinner);

        section.getChildren().addAll(titleLabel, urlBox, depthBox, pagesBox);
        return section;
    }

    private HBox createControlSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);

        startButton = new Button("Start Crawl");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> startCrawl());

        stopButton = new Button("Stop");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopCrawl());

        Button clearButton = new Button("Clear Database");
        clearButton.setOnAction(e -> clearDatabase());

        section.getChildren().addAll(startButton, stopButton, clearButton);
        return section;
    }

    private VBox createProgressSection() {
        VBox section = new VBox(5);

        progressLabel = new Label("Ready to crawl");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);

        section.getChildren().addAll(progressLabel, progressBar);
        return section;
    }

    private VBox createLogSection() {
        VBox section = new VBox(5);
        VBox.setVgrow(section, Priority.ALWAYS);

        Label logLabel = new Label("Crawl Log:");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(12);
        logArea.setStyle("-fx-font-family: monospace;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        section.getChildren().addAll(logLabel, logArea);
        return section;
    }

    private HBox createStatsSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(10, 0, 0, 0));
        section.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10;");

        statsLabel = new Label("Pages: 0");
        Button refreshButton = new Button("Refresh Stats");
        refreshButton.setOnAction(e -> updateStats());

        section.getChildren().addAll(statsLabel, refreshButton);
        return section;
    }

    private void startCrawl() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showAlert("Error", "Please enter a URL to crawl");
            return;
        }

        int depth = depthSpinner.getValue();
        int maxPages = maxPagesSpinner.getValue();

        isCrawling = true;
        startButton.setDisable(true);
        stopButton.setDisable(false);
        logArea.clear();
        progressBar.setProgress(0);

        log("Starting crawl: " + url);
        log("Max depth: " + depth + ", Max pages: " + maxPages);
        statusUpdater.accept("Crawling: " + url);

        crawlerThread = new Thread(() -> {
            try {
                crawler = new WebCrawler(dbManager);

                crawler.setProgressListener(new WebCrawler.CrawlerProgressListener() {
                    @Override
                    public void onCrawlStarted(String startUrl, int maxDepth) {
                        Platform.runLater(() -> log("Crawl started: " + startUrl));
                    }

                    @Override
                    public void onPageCrawlStart(String pageUrl, int pageDepth) {
                        Platform.runLater(() -> log("Crawling: " + pageUrl));
                    }

                    @Override
                    public void onPageCrawlSuccess(String pageUrl, int pageDepth, Long pageId, int totalCrawled) {
                        Platform.runLater(() -> {
                            double progress = Math.min(1.0, (double) totalCrawled / maxPages);
                            progressBar.setProgress(progress);
                            progressLabel.setText("Pages crawled: " + totalCrawled);
                        });
                    }

                    @Override
                    public void onPageCrawlError(String pageUrl, int pageDepth, Exception e) {
                        Platform.runLater(() -> log("Error: " + pageUrl + " - " + e.getMessage()));
                    }

                    @Override
                    public void onPageCrawlSkipped(String pageUrl, String reason) {
                        // Skip logging for performance
                    }

                    @Override
                    public void onCrawlCompleted(int totalPages) {
                        Platform.runLater(() -> {
                            log("Crawl complete! Total pages: " + totalPages);
                            progressBar.setProgress(1.0);
                            crawlFinished();
                        });
                    }

                    @Override
                    public void onCrawlStopped(int totalPages) {
                        Platform.runLater(() -> {
                            log("Crawl stopped. Pages crawled: " + totalPages);
                            crawlFinished();
                        });
                    }
                });

                crawler.startCrawl(url, depth, maxPages);

                // Wait for crawl to complete
                while (crawler.isRunning() && isCrawling) {
                    Thread.sleep(500);
                }

                if (!isCrawling) {
                    crawler.stopCrawl();
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    log("Error: " + e.getMessage());
                    crawlFinished();
                });
                logger.error("Crawl error", e);
            }
        });

        crawlerThread.setDaemon(true);
        crawlerThread.start();
    }

    private void stopCrawl() {
        isCrawling = false;
        if (crawler != null) {
            crawler.stopCrawl();
        }
        log("Crawl stopped by user");
        crawlFinished();
    }

    private void crawlFinished() {
        isCrawling = false;
        startButton.setDisable(false);
        stopButton.setDisable(true);
        statusUpdater.accept("Crawl finished");
        updateStats();
    }

    private void clearDatabase() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Clear");
        confirm.setHeaderText("Clear all crawled data?");
        confirm.setContentText("This will delete all pages and indexes from the database.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                dbManager.clearAllData();
                log("Database cleared");
                updateStats();
                statusUpdater.accept("Database cleared");
            } catch (Exception e) {
                log("Error clearing database: " + e.getMessage());
            }
        }
    }

    private void updateStats() {
        try {
            PageDAO pageDAO = new PageDAO(dbManager);
            int pageCount = pageDAO.getTotalPageCount();
            statsLabel.setText("Pages in database: " + pageCount);
        } catch (Exception e) {
            statsLabel.setText("Pages: Error loading");
        }
    }

    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

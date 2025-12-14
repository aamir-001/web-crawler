package com.searchengine.gui;

import com.searchengine.database.DatabaseManager;
import com.searchengine.database.PageDAO;
import com.searchengine.indexer.Indexer;
import com.searchengine.search.SearchEngine;
import com.searchengine.search.SearchResult;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

/**
 * Search panel with query input and ranked results display.
 */
public class SearchPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(SearchPanel.class);

    private final DatabaseManager dbManager;
    private final Indexer indexer;
    private final SearchEngine searchEngine;
    private final Consumer<String> statusUpdater;

    private TextField searchField;
    private Button searchButton;
    private Button reindexButton;
    private ListView<SearchResult> resultsListView;
    private Label statsLabel;
    private Label resultCountLabel;

    public SearchPanel(DatabaseManager dbManager, Indexer indexer,
                       SearchEngine searchEngine, Consumer<String> statusUpdater) {
        this.dbManager = dbManager;
        this.indexer = indexer;
        this.searchEngine = searchEngine;
        this.statusUpdater = statusUpdater;

        setPadding(new Insets(15));
        setSpacing(15);

        getChildren().addAll(
            createSearchSection(),
            createResultsSection(),
            createStatsSection()
        );

        updateStats();
    }

    private VBox createSearchSection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("Search Engine");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Enter search query...");
        searchField.setPrefWidth(400);
        searchField.setOnAction(e -> performSearch());

        searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        searchButton.setOnAction(e -> performSearch());

        reindexButton = new Button("Reindex");
        reindexButton.setOnAction(e -> reindexPages());

        searchBox.getChildren().addAll(searchField, searchButton, reindexButton);

        resultCountLabel = new Label("");

        section.getChildren().addAll(titleLabel, searchBox, resultCountLabel);
        return section;
    }

    private VBox createResultsSection() {
        VBox section = new VBox(5);
        VBox.setVgrow(section, Priority.ALWAYS);

        Label resultsLabel = new Label("Results:");

        resultsListView = new ListView<>();
        resultsListView.setCellFactory(param -> new SearchResultCell());
        VBox.setVgrow(resultsListView, Priority.ALWAYS);

        section.getChildren().addAll(resultsLabel, resultsListView);
        return section;
    }

    private HBox createStatsSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);
        section.setPadding(new Insets(10, 0, 0, 0));
        section.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10;");

        statsLabel = new Label("Index: Loading...");
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> updateStats());

        section.getChildren().addAll(statsLabel, refreshButton);
        return section;
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        searchButton.setDisable(true);
        statusUpdater.accept("Searching: " + query);
        resultsListView.getItems().clear();
        resultCountLabel.setText("Searching...");

        new Thread(() -> {
            try {
                List<SearchResult> results = searchEngine.search(query);

                Platform.runLater(() -> {
                    resultsListView.getItems().addAll(results);
                    resultCountLabel.setText("Found " + results.size() + " result(s)");
                    statusUpdater.accept("Found " + results.size() + " results for: " + query);
                    searchButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    resultCountLabel.setText("Error: " + e.getMessage());
                    searchButton.setDisable(false);
                });
                logger.error("Search error", e);
            }
        }).start();
    }

    private void reindexPages() {
        reindexButton.setDisable(true);
        statusUpdater.accept("Reindexing pages...");

        new Thread(() -> {
            try {
                int count = indexer.indexAllPages();
                Platform.runLater(() -> {
                    statusUpdater.accept("Reindexed " + count + " pages");
                    updateStats();
                    reindexButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusUpdater.accept("Reindex error: " + e.getMessage());
                    reindexButton.setDisable(false);
                });
                logger.error("Reindex error", e);
            }
        }).start();
    }

    private void updateStats() {
        try {
            PageDAO pageDAO = new PageDAO(dbManager);
            int pageCount = pageDAO.getTotalPageCount();
            int wordCount = indexer.getInvertedIndex().getUniqueWordCount();
            statsLabel.setText("Pages: " + pageCount + " | Unique words: " + wordCount);
        } catch (Exception e) {
            statsLabel.setText("Stats: Error loading");
        }
    }

    /**
     * Custom cell for displaying search results.
     */
    private static class SearchResultCell extends ListCell<SearchResult> {
        @Override
        protected void updateItem(SearchResult result, boolean empty) {
            super.updateItem(result, empty);

            if (empty || result == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox container = new VBox(3);
                container.setPadding(new Insets(8));

                // Title as hyperlink
                Hyperlink titleLink = new Hyperlink(result.getRank() + ". " + result.getTitle());
                titleLink.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                titleLink.setOnAction(e -> openUrl(result.getUrl()));

                // URL
                Label urlLabel = new Label(result.getUrl());
                urlLabel.setStyle("-fx-text-fill: #006621; -fx-font-size: 12px;");

                // Score
                Label scoreLabel = new Label(String.format("Score: %.4f", result.getScore()));
                scoreLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

                // Snippet
                String snippet = result.getSnippet();
                if (snippet != null && !snippet.isEmpty()) {
                    // Convert **bold** markers to styled text
                    Label snippetLabel = new Label(snippet.replaceAll("\\*\\*", ""));
                    snippetLabel.setWrapText(true);
                    snippetLabel.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");
                    container.getChildren().addAll(titleLink, urlLabel, scoreLabel, snippetLabel);
                } else {
                    container.getChildren().addAll(titleLink, urlLabel, scoreLabel);
                }

                setGraphic(container);
            }
        }

        private void openUrl(String url) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception e) {
                logger.error("Failed to open URL: {}", url, e);
            }
        }
    }
}

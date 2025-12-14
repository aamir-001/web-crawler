package com.searchengine.gui;

import com.searchengine.database.DatabaseManager;
import com.searchengine.indexer.Indexer;
import com.searchengine.search.SearchEngine;
import com.searchengine.utils.ConfigLoader;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX application window with tabbed interface.
 */
public class MainWindow extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);

    private DatabaseManager dbManager;
    private Indexer indexer;
    private SearchEngine searchEngine;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        try {
            initializeBackend();

            primaryStage.setTitle("Desktop Search Engine");

            // Create main layout
            BorderPane root = new BorderPane();

            // Create menu bar
            MenuBar menuBar = createMenuBar(primaryStage);
            root.setTop(menuBar);

            // Create tab pane
            TabPane tabPane = new TabPane();

            // Crawler tab
            Tab crawlerTab = new Tab("Crawler");
            crawlerTab.setClosable(false);
            CrawlerPanel crawlerPanel = new CrawlerPanel(dbManager, this::updateStatus);
            crawlerTab.setContent(crawlerPanel);

            // Search tab
            Tab searchTab = new Tab("Search");
            searchTab.setClosable(false);
            SearchPanel searchPanel = new SearchPanel(dbManager, indexer, searchEngine, this::updateStatus);
            searchTab.setContent(searchPanel);

            tabPane.getTabs().addAll(crawlerTab, searchTab);
            root.setCenter(tabPane);

            // Status bar
            statusLabel = new Label("Ready");
            statusLabel.setPadding(new Insets(5, 10, 5, 10));
            statusLabel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 1 0 0 0;");
            root.setBottom(statusLabel);

            // Create scene
            Scene scene = new Scene(root, 900, 650);
            primaryStage.setScene(scene);
            primaryStage.show();

            updateStatus("Application started");
            logger.info("GUI application started");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showError("Startup Error", "Failed to start application: " + e.getMessage());
        }
    }

    private void initializeBackend() {
        String dbPath = ConfigLoader.getString("database.path", "data/demo.db");
        dbManager = DatabaseManager.getInstance(dbPath, 5);
        indexer = new Indexer(dbManager);
        searchEngine = new SearchEngine(dbManager, indexer);
        logger.info("Backend initialized with database: {}", dbPath);
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            shutdown();
            stage.close();
        });
        fileMenu.getItems().add(exitItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, helpMenu);
        return menuBar;
    }

    private void updateStatus(String message) {
        if (statusLabel != null) {
            javafx.application.Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Desktop Search Engine");
        alert.setContentText(
            "A web crawler and search engine.\n\n" +
            "Features:\n" +
            "- Multi-threaded web crawling\n" +
            "- TF-IDF search ranking\n" +
            "- Snippet generation\n\n" +
            "Built with Java 17 and JavaFX"
        );
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void shutdown() {
        if (dbManager != null) {
            dbManager.shutdown();
        }
        logger.info("Application shutdown");
    }

    @Override
    public void stop() {
        shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

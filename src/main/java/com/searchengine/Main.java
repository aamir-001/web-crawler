package com.searchengine;

import com.searchengine.gui.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Desktop Search Engine application.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Check for command-line arguments
        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return;
            } else if (arg.equals("--cli")) {
                printCliOptions();
                return;
            }
        }

        // Launch JavaFX GUI
        System.out.println("Starting Desktop Search Engine GUI...");
        logger.info("Launching GUI application");

        try {
            MainWindow.main(args);
        } catch (Exception e) {
            logger.error("Failed to launch GUI", e);
            System.err.println("Error launching GUI: " + e.getMessage());
            System.err.println("\nFallback to CLI mode. Use these commands:");
            printCliOptions();
        }
    }

    private static void printHelp() {
        System.out.println("=".repeat(60));
        System.out.println("Desktop Search Engine");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar desktop-search-engine.jar          Launch GUI");
        System.out.println("  java -jar desktop-search-engine.jar --cli    Show CLI options");
        System.out.println("  java -jar desktop-search-engine.jar --help   Show this help");
        System.out.println();
        System.out.println("Or use Maven:");
        System.out.println("  mvn javafx:run                               Launch GUI");
        System.out.println();
    }

    private static void printCliOptions() {
        System.out.println("=".repeat(60));
        System.out.println("Desktop Search Engine - CLI Mode");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Available command-line demos:");
        System.out.println();
        System.out.println("1. CrawlerDemo  - Test web crawling");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"com.searchengine.CrawlerDemo\"");
        System.out.println();
        System.out.println("2. IndexerDemo  - Test indexing");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"com.searchengine.IndexerDemo\"");
        System.out.println();
        System.out.println("3. SearchDemo   - Interactive search");
        System.out.println("   mvn exec:java -Dexec.mainClass=\"com.searchengine.SearchDemo\"");
        System.out.println();
        System.out.println("To launch GUI:");
        System.out.println("   mvn javafx:run");
        System.out.println("=".repeat(60));
    }
}

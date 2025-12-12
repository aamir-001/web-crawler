package com.searchengine.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Manages SQLite database connections and schema initialization.
 * Implements connection pooling for thread-safe database access.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;

    private final String databasePath;
    private final BlockingQueue<Connection> connectionPool;
    private final int poolSize;

    private DatabaseManager(String databasePath, int poolSize) {
        this.databasePath = databasePath;
        this.poolSize = poolSize;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);

        initializeDatabase();
        initializeConnectionPool();
    }

    /**
     * Get singleton instance of DatabaseManager
     */
    public static synchronized DatabaseManager getInstance(String databasePath, int poolSize) {
        if (instance == null) {
            instance = new DatabaseManager(databasePath, poolSize);
        }
        return instance;
    }

    /**
     * Initialize database schema
     */
    private void initializeDatabase() {
        logger.info("Initializing database at: {}", databasePath);

        try (Connection conn = createNewConnection()) {
            createTables(conn);
            createIndexes(conn);
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Create database tables
     */
    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Pages table - stores crawled web pages
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pages (
                    page_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    url TEXT UNIQUE NOT NULL,
                    title TEXT,
                    content TEXT,
                    crawl_timestamp INTEGER NOT NULL,
                    word_count INTEGER DEFAULT 0,
                    depth INTEGER DEFAULT 0
                )
            """);

            // Words table - stores unique words from all pages
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS words (
                    word_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    word TEXT UNIQUE NOT NULL
                )
            """);

            // Word positions table - inverted index
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS word_positions (
                    word_id INTEGER NOT NULL,
                    page_id INTEGER NOT NULL,
                    frequency INTEGER DEFAULT 1,
                    positions TEXT,
                    FOREIGN KEY(word_id) REFERENCES words(word_id) ON DELETE CASCADE,
                    FOREIGN KEY(page_id) REFERENCES pages(page_id) ON DELETE CASCADE,
                    PRIMARY KEY(word_id, page_id)
                )
            """);

            // Crawl metadata table - stores information about crawl sessions
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS crawl_metadata (
                    crawl_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    start_url TEXT NOT NULL,
                    max_depth INTEGER NOT NULL,
                    pages_crawled INTEGER DEFAULT 0,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER,
                    status TEXT DEFAULT 'running'
                )
            """);

            logger.debug("Database tables created/verified");
        }
    }

    /**
     * Create indexes for performance
     */
    private void createIndexes(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_words_word ON words(word)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_word_positions_word ON word_positions(word_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_word_positions_page ON word_positions(page_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pages_url ON pages(url)");

            logger.debug("Database indexes created/verified");
        }
    }

    /**
     * Initialize connection pool with available connections
     */
    private void initializeConnectionPool() {
        try {
            for (int i = 0; i < poolSize; i++) {
                connectionPool.add(createNewConnection());
            }
            logger.info("Connection pool initialized with {} connections", poolSize);
        } catch (SQLException e) {
            logger.error("Failed to initialize connection pool", e);
            throw new RuntimeException("Connection pool initialization failed", e);
        }
    }

    /**
     * Create a new database connection
     */
    private Connection createNewConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        Connection conn = DriverManager.getConnection(url);
        // Enable foreign keys
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /**
     * Get a connection from the pool (blocking if none available)
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = connectionPool.take();
            // Check if connection is still valid
            if (conn.isClosed()) {
                conn = createNewConnection();
            }
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        }
    }

    /**
     * Return a connection to the pool
     */
    public void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    connectionPool.offer(conn);
                } else {
                    // If connection is closed, create a new one
                    connectionPool.offer(createNewConnection());
                }
            } catch (SQLException e) {
                logger.error("Error releasing connection", e);
            }
        }
    }

    /**
     * Close all connections and clean up
     */
    public void shutdown() {
        logger.info("Shutting down database manager");

        while (!connectionPool.isEmpty()) {
            try {
                Connection conn = connectionPool.poll();
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("Error closing connection", e);
            }
        }

        logger.info("Database manager shutdown complete");
    }

    /**
     * Clear all data from database (useful for testing)
     */
    public void clearAllData() throws SQLException {
        logger.warn("Clearing all data from database");

        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM word_positions");
            stmt.execute("DELETE FROM words");
            stmt.execute("DELETE FROM pages");
            stmt.execute("DELETE FROM crawl_metadata");
            logger.info("All data cleared from database");
        } finally {
            releaseConnection(conn);
        }
    }
}

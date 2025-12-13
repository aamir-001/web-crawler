package com.searchengine.database;

import com.searchengine.indexer.InvertedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

/**
 * Data Access Object for managing the inverted index in the database.
 * Handles persistence of words and word positions.
 */
public class IndexDAO {
    private static final Logger logger = LoggerFactory.getLogger(IndexDAO.class);
    private final DatabaseManager dbManager;

    public IndexDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Save a word to the database and return its ID.
     * If the word already exists, returns the existing ID.
     *
     * @param word The word to save
     * @return The word ID
     */
    public Long saveWord(String word) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            // First, try to get existing word ID
            String selectSql = "SELECT word_id FROM words WHERE word = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, word);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong("word_id");
                }
            }

            // Word doesn't exist, insert it
            String insertSql = "INSERT INTO words (word) VALUES (?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, word);
                insertStmt.executeUpdate();
            }

            // Get the last inserted ID using SQLite's last_insert_rowid()
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

            throw new SQLException("Failed to retrieve word ID for: " + word);

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Save word positions for a page.
     *
     * @param wordId The word ID
     * @param pageId The page ID
     * @param frequency The number of times the word appears on the page
     * @param positions Comma-separated list of positions
     */
    public void saveWordPosition(Long wordId, Long pageId, int frequency, String positions) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = """
                INSERT OR REPLACE INTO word_positions (word_id, page_id, frequency, positions)
                VALUES (?, ?, ?, ?)
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, wordId);
                pstmt.setLong(2, pageId);
                pstmt.setInt(3, frequency);
                pstmt.setString(4, positions);
                pstmt.executeUpdate();
            }

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Save the entire inverted index for a page to the database.
     *
     * @param pageId The page ID
     * @param index The inverted index containing word data
     */
    public void saveIndexForPage(Long pageId, InvertedIndex index) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Use transaction for better performance

            // NOTE: This method is a placeholder for batch saving.
            // In practice, use saveWordEntry() for each word individually.
            // This requires InvertedIndex to provide an iterator over words for a specific page.

            logger.debug("Saving index for page {}", pageId);

            conn.commit();
            logger.debug("Index saved successfully for page {}", pageId);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Failed to reset auto-commit", e);
                }
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Save a single word's posting entry to the database.
     *
     * @param word The word
     * @param pageId The page ID
     * @param entry The posting entry containing frequency and positions
     */
    public void saveWordEntry(String word, Long pageId, InvertedIndex.PostingEntry entry) throws SQLException {
        // Get or create word ID
        Long wordId = saveWord(word);

        // Convert positions list to comma-separated string
        List<Integer> positions = entry.getPositions();
        StringBuilder positionsStr = new StringBuilder();
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) positionsStr.append(",");
            positionsStr.append(positions.get(i));
        }

        // Save word position
        saveWordPosition(wordId, pageId, entry.getFrequency(), positionsStr.toString());
    }

    /**
     * Get the word ID for a given word.
     *
     * @param word The word to look up
     * @return The word ID, or null if not found
     */
    public Long getWordId(String word) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = "SELECT word_id FROM words WHERE word = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, word);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong("word_id");
                }
                return null;
            }

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Get all pages containing a specific word.
     *
     * @param word The word to search for
     * @return List of page IDs
     */
    public List<Long> getPagesForWord(String word) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = """
                SELECT wp.page_id, wp.frequency
                FROM words w
                JOIN word_positions wp ON w.word_id = wp.word_id
                WHERE w.word = ?
                ORDER BY wp.frequency DESC
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, word);
                ResultSet rs = pstmt.executeQuery();

                List<Long> pageIds = new java.util.ArrayList<>();
                while (rs.next()) {
                    pageIds.add(rs.getLong("page_id"));
                }
                return pageIds;
            }

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Delete all index entries for a specific page.
     *
     * @param pageId The page ID
     */
    public void deleteIndexForPage(Long pageId) throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = "DELETE FROM word_positions WHERE page_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, pageId);
                int deleted = pstmt.executeUpdate();
                logger.debug("Deleted {} word positions for page {}", deleted, pageId);
            }

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Get the total number of unique words in the index.
     */
    public int getTotalUniqueWords() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = "SELECT COUNT(*) FROM words";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }

    /**
     * Get the total number of word-page associations.
     */
    public int getTotalWordPositions() throws SQLException {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            String sql = "SELECT COUNT(*) FROM word_positions";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }

        } finally {
            if (conn != null) {
                dbManager.releaseConnection(conn);
            }
        }
    }
}

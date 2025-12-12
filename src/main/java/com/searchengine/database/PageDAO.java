package com.searchengine.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Page operations
 */
public class PageDAO {
    private static final Logger logger = LoggerFactory.getLogger(PageDAO.class);
    private final DatabaseManager dbManager;

    public PageDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insert a new page into the database
     */
    public Long insertPage(Page page) throws SQLException {
        String sql = """
            INSERT INTO pages (url, title, content, crawl_timestamp, word_count, depth)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        Connection conn = dbManager.getConnection();
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, page.getUrl());
                pstmt.setString(2, page.getTitle());
                pstmt.setString(3, page.getContent());
                pstmt.setLong(4, page.getCrawlTimestamp());
                pstmt.setInt(5, page.getWordCount());
                pstmt.setInt(6, page.getDepth());

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Inserting page failed, no rows affected.");
                }
            }

            // Get the last inserted ID using SQLite's last_insert_rowid()
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    Long pageId = rs.getLong(1);
                    page.setPageId(pageId);
                    logger.debug("Inserted page: {} with ID: {}", page.getUrl(), pageId);
                    return pageId;
                } else {
                    throw new SQLException("Inserting page failed, no ID obtained.");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Check if a URL already exists in the database
     */
    public boolean urlExists(String url) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pages WHERE url = ?";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, url);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return false;
    }

    /**
     * Get a page by its URL
     */
    public Optional<Page> getPageByUrl(String url) throws SQLException {
        String sql = "SELECT * FROM pages WHERE url = ?";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, url);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPage(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return Optional.empty();
    }

    /**
     * Get a page by its ID
     */
    public Optional<Page> getPageById(Long pageId) throws SQLException {
        String sql = "SELECT * FROM pages WHERE page_id = ?";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPage(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return Optional.empty();
    }

    /**
     * Get all pages
     */
    public List<Page> getAllPages() throws SQLException {
        String sql = "SELECT * FROM pages ORDER BY crawl_timestamp DESC";
        List<Page> pages = new ArrayList<>();

        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                pages.add(mapResultSetToPage(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return pages;
    }

    /**
     * Get total number of pages
     */
    public int getTotalPageCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM pages";

        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return 0;
    }

    /**
     * Update word count for a page
     */
    public void updateWordCount(Long pageId, int wordCount) throws SQLException {
        String sql = "UPDATE pages SET word_count = ? WHERE page_id = ?";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, wordCount);
            pstmt.setLong(2, pageId);
            pstmt.executeUpdate();
            logger.debug("Updated word count for page {}: {}", pageId, wordCount);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Delete a page by ID
     */
    public void deletePage(Long pageId) throws SQLException {
        String sql = "DELETE FROM pages WHERE page_id = ?";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, pageId);
            pstmt.executeUpdate();
            logger.debug("Deleted page with ID: {}", pageId);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Map ResultSet to Page object
     */
    private Page mapResultSetToPage(ResultSet rs) throws SQLException {
        Page page = new Page();
        page.setPageId(rs.getLong("page_id"));
        page.setUrl(rs.getString("url"));
        page.setTitle(rs.getString("title"));
        page.setContent(rs.getString("content"));
        page.setCrawlTimestamp(rs.getLong("crawl_timestamp"));
        page.setWordCount(rs.getInt("word_count"));
        page.setDepth(rs.getInt("depth"));
        return page;
    }
}

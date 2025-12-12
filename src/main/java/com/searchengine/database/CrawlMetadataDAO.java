package com.searchengine.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for CrawlMetadata operations
 */
public class CrawlMetadataDAO {
    private static final Logger logger = LoggerFactory.getLogger(CrawlMetadataDAO.class);
    private final DatabaseManager dbManager;

    public CrawlMetadataDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insert a new crawl metadata record
     */
    public Long insertCrawlMetadata(CrawlMetadata metadata) throws SQLException {
        String sql = """
            INSERT INTO crawl_metadata (start_url, max_depth, pages_crawled, start_time, status)
            VALUES (?, ?, ?, ?, ?)
        """;

        Connection conn = dbManager.getConnection();
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, metadata.getStartUrl());
                pstmt.setInt(2, metadata.getMaxDepth());
                pstmt.setInt(3, metadata.getPagesCrawled());
                pstmt.setLong(4, metadata.getStartTime());
                pstmt.setString(5, metadata.getStatus());

                pstmt.executeUpdate();
            }

            // Get the last inserted ID using SQLite's last_insert_rowid()
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    Long crawlId = rs.getLong(1);
                    metadata.setCrawlId(crawlId);
                    logger.info("Created new crawl session with ID: {}", crawlId);
                    return crawlId;
                } else {
                    throw new SQLException("Inserting crawl metadata failed, no ID obtained.");
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Update crawl metadata
     */
    public void updateCrawlMetadata(CrawlMetadata metadata) throws SQLException {
        String sql = """
            UPDATE crawl_metadata
            SET pages_crawled = ?, end_time = ?, status = ?
            WHERE crawl_id = ?
        """;

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, metadata.getPagesCrawled());
            if (metadata.getEndTime() != null) {
                pstmt.setLong(2, metadata.getEndTime());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, metadata.getStatus());
            pstmt.setLong(4, metadata.getCrawlId());

            pstmt.executeUpdate();
            logger.debug("Updated crawl metadata for ID: {}", metadata.getCrawlId());
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    /**
     * Get crawl metadata by ID
     */
    public Optional<CrawlMetadata> getCrawlMetadata(Long crawlId) throws SQLException {
        String sql = "SELECT * FROM crawl_metadata WHERE crawl_id = ?";

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, crawlId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCrawlMetadata(rs));
                }
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return Optional.empty();
    }

    /**
     * Get all crawl metadata records
     */
    public List<CrawlMetadata> getAllCrawlMetadata() throws SQLException {
        String sql = "SELECT * FROM crawl_metadata ORDER BY start_time DESC";
        List<CrawlMetadata> metadataList = new ArrayList<>();

        Connection conn = dbManager.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                metadataList.add(mapResultSetToCrawlMetadata(rs));
            }
        } finally {
            dbManager.releaseConnection(conn);
        }
        return metadataList;
    }

    /**
     * Map ResultSet to CrawlMetadata object
     */
    private CrawlMetadata mapResultSetToCrawlMetadata(ResultSet rs) throws SQLException {
        CrawlMetadata metadata = new CrawlMetadata();
        metadata.setCrawlId(rs.getLong("crawl_id"));
        metadata.setStartUrl(rs.getString("start_url"));
        metadata.setMaxDepth(rs.getInt("max_depth"));
        metadata.setPagesCrawled(rs.getInt("pages_crawled"));
        metadata.setStartTime(rs.getLong("start_time"));

        long endTime = rs.getLong("end_time");
        if (!rs.wasNull()) {
            metadata.setEndTime(endTime);
        }

        metadata.setStatus(rs.getString("status"));
        return metadata;
    }
}

package com.contractiq.dao;

import com.contractiq.config.DatabaseConfig;
import com.contractiq.model.Obligation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ObligationDao {
    private static final Logger logger = LoggerFactory.getLogger(ObligationDao.class);

    public Long create(Obligation o) throws SQLException {
        String sql = """
            INSERT INTO obligations (
                contract_id, obligation_title, description, responsible_party,
                deadline_date, frequency, category, source_excerpt, page_number,
                confidence, approval_status, approved_by, review_comment, reviewed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setObligationParams(ps, o);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    o.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated ID for obligation.");
    }

    /**
     * Inserts a list of obligations in an atomic transaction.
     */
    public List<Obligation> createBatch(List<Obligation> list, Long contractId) throws SQLException {
        String sql = """
            INSERT INTO obligations (
                contract_id, obligation_title, description, responsible_party,
                deadline_date, frequency, category, source_excerpt, page_number,
                confidence, approval_status, approved_by, review_comment, reviewed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (Obligation o : list) {
                    o.setContractId(contractId);
                    setObligationParams(ps, o);
                    ps.addBatch();
                }
                ps.executeBatch();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    int idx = 0;
                    while (rs.next() && idx < list.size()) {
                        list.get(idx).setId(rs.getLong(1));
                        idx++;
                    }
                }
                conn.commit();
                return list;
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Error creating obligations batch for contract {}: {}", contractId, e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public Optional<Obligation> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM obligations WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToObligation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Obligation> findByContractId(Long contractId) throws SQLException {
        String sql = "SELECT * FROM obligations WHERE contract_id = ? ORDER BY deadline_date ASC, id ASC";
        List<Obligation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToObligation(rs));
                }
            }
        }
        return list;
    }

    public List<Obligation> findAll(String approvalStatus, String category, String search) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM obligations WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (approvalStatus != null && !approvalStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(approvalStatus)) {
            sql.append("AND approval_status = ? ");
            params.add(approvalStatus.trim());
        }

        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND category = ? ");
            params.add(category.trim());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (obligation_title LIKE ? OR description LIKE ? OR responsible_party LIKE ?) ");
            String wild = "%" + search.trim() + "%";
            params.add(wild);
            params.add(wild);
            params.add(wild);
        }

        sql.append("ORDER BY deadline_date ASC, id ASC");

        List<Obligation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToObligation(rs));
                }
            }
        }
        return list;
    }

    public void update(Obligation o) throws SQLException {
        String sql = """
            UPDATE obligations SET
                obligation_title = ?, description = ?, responsible_party = ?,
                deadline_date = ?, frequency = ?, category = ?, review_comment = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getObligationTitle());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getResponsibleParty());
            ps.setObject(4, o.getDeadlineDate());
            ps.setString(5, o.getFrequency());
            ps.setString(6, o.getCategory());
            ps.setString(7, o.getReviewComment());
            ps.setLong(8, o.getId());

            ps.executeUpdate();
        }
    }

    public void updateApprovalStatus(Long id, String status, String approvedBy, String comment) throws SQLException {
        String sql = """
            UPDATE obligations SET
                approval_status = ?,
                approved_by = ?,
                review_comment = ?,
                reviewed_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, approvedBy);
            ps.setString(3, comment);
            ps.setLong(4, id);

            ps.executeUpdate();
        }
    }

    public long countApproved() throws SQLException {
        String sql = "SELECT COUNT(*) FROM obligations WHERE approval_status = 'APPROVED'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public long countPending() throws SQLException {
        String sql = "SELECT COUNT(*) FROM obligations WHERE approval_status = 'PENDING'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public Map<String, Integer> countByCategory() throws SQLException {
        String sql = "SELECT category, COUNT(*) as cnt FROM obligations GROUP BY category";
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public Map<String, Integer> countByStatus() throws SQLException {
        String sql = "SELECT approval_status, COUNT(*) as cnt FROM obligations GROUP BY approval_status";
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("approval_status"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM obligations WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void setObligationParams(PreparedStatement ps, Obligation o) throws SQLException {
        ps.setLong(1, o.getContractId());
        ps.setString(2, o.getObligationTitle());
        ps.setString(3, o.getDescription());
        ps.setString(4, o.getResponsibleParty());
        ps.setObject(5, o.getDeadlineDate());
        ps.setString(6, o.getFrequency() != null ? o.getFrequency() : "ONE_TIME");
        ps.setString(7, o.getCategory() != null ? o.getCategory() : "OTHER");
        ps.setString(8, o.getSourceExcerpt());
        ps.setObject(9, o.getPageNumber() != null ? o.getPageNumber() : 1);
        ps.setBigDecimal(10, o.getConfidence() != null ? o.getConfidence() : new BigDecimal("0.85"));
        ps.setString(11, o.getApprovalStatus() != null ? o.getApprovalStatus() : "PENDING");
        ps.setString(12, o.getApprovedBy());
        ps.setString(13, o.getReviewComment());
        ps.setTimestamp(14, o.getReviewedAt() != null ? Timestamp.valueOf(o.getReviewedAt()) : null);
    }

    private Obligation mapResultSetToObligation(ResultSet rs) throws SQLException {
        Obligation o = new Obligation();
        o.setId(rs.getLong("id"));
        o.setContractId(rs.getLong("contract_id"));
        o.setObligationTitle(rs.getString("obligation_title"));
        o.setDescription(rs.getString("description"));
        o.setResponsibleParty(rs.getString("responsible_party"));

        Date d = rs.getDate("deadline_date");
        if (d != null) o.setDeadlineDate(d.toLocalDate());

        o.setFrequency(rs.getString("frequency"));
        o.setCategory(rs.getString("category"));
        o.setSourceExcerpt(rs.getString("source_excerpt"));
        o.setPageNumber(rs.getInt("page_number"));
        o.setConfidence(rs.getBigDecimal("confidence"));
        o.setApprovalStatus(rs.getString("approval_status"));
        o.setApprovedBy(rs.getString("approved_by"));
        o.setReviewComment(rs.getString("review_comment"));

        Timestamp revAt = rs.getTimestamp("reviewed_at");
        if (revAt != null) o.setReviewedAt(revAt.toLocalDateTime());

        Timestamp cAt = rs.getTimestamp("created_at");
        if (cAt != null) o.setCreatedAt(cAt.toLocalDateTime());

        Timestamp uAt = rs.getTimestamp("updated_at");
        if (uAt != null) o.setUpdatedAt(uAt.toLocalDateTime());

        return o;
    }
}

package com.contractiq.dao;

import com.contractiq.config.DatabaseConfig;
import com.contractiq.model.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContractDao {
    private static final Logger logger = LoggerFactory.getLogger(ContractDao.class);

    public Long create(Contract contract) throws SQLException {
        String sql = """
            INSERT INTO contracts (
                file_name, stored_file_path, file_size_bytes, file_hash_sha256,
                contract_title, contract_type, party_first, party_second,
                effective_date, expiration_date, auto_renewal, renewal_notice_period_days,
                renewal_notice_deadline, renewal_terms, payment_terms, penalties_clause,
                termination_conditions, extraction_status, review_status, confidence_score,
                review_notes, raw_text_excerpt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, contract.getFileName());
            ps.setString(2, contract.getStoredFilePath());
            ps.setLong(3, contract.getFileSizeBytes() != null ? contract.getFileSizeBytes() : 0);
            ps.setString(4, contract.getFileHashSha256());

            ps.setString(5, contract.getContractTitle() != null ? contract.getContractTitle() : "Untitled Contract");
            ps.setString(6, contract.getContractType() != null ? contract.getContractType() : "General Agreement");
            ps.setString(7, contract.getPartyFirst());
            ps.setString(8, contract.getPartySecond());

            ps.setObject(9, contract.getEffectiveDate());
            ps.setObject(10, contract.getExpirationDate());
            ps.setBoolean(11, contract.getAutoRenewal() != null && contract.getAutoRenewal());
            ps.setObject(12, contract.getRenewalNoticePeriodDays());
            ps.setObject(13, contract.getRenewalNoticeDeadline());
            ps.setString(14, contract.getRenewalTerms());

            ps.setString(15, contract.getPaymentTerms());
            ps.setString(16, contract.getPenaltiesClause());
            ps.setString(17, contract.getTerminationConditions());

            ps.setString(18, contract.getExtractionStatus() != null ? contract.getExtractionStatus() : "PENDING");
            ps.setString(19, contract.getReviewStatus() != null ? contract.getReviewStatus() : "NEEDS_REVIEW");
            ps.setBigDecimal(20, contract.getConfidenceScore() != null ? contract.getConfidenceScore() : BigDecimal.ZERO);
            ps.setString(21, contract.getReviewNotes());
            ps.setString(22, contract.getRawTextExcerpt());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    contract.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated ID for contract.");
    }

    public Optional<Contract> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM contracts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToContract(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Contract> findAll(String reviewStatus, String search) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM contracts WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (reviewStatus != null && !reviewStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(reviewStatus)) {
            sql.append("AND review_status = ? ");
            params.add(reviewStatus.trim());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (contract_title LIKE ? OR party_first LIKE ? OR party_second LIKE ? OR contract_type LIKE ?) ");
            String wild = "%" + search.trim() + "%";
            params.add(wild);
            params.add(wild);
            params.add(wild);
            params.add(wild);
        }

        sql.append("ORDER BY created_at DESC");

        List<Contract> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToContract(rs));
                }
            }
        }
        return list;
    }

    public void update(Contract contract) throws SQLException {
        String sql = """
            UPDATE contracts SET
                contract_title = ?, contract_type = ?, party_first = ?, party_second = ?,
                effective_date = ?, expiration_date = ?, auto_renewal = ?,
                renewal_notice_period_days = ?, renewal_notice_deadline = ?,
                renewal_terms = ?, payment_terms = ?, penalties_clause = ?,
                termination_conditions = ?, extraction_status = ?, review_status = ?,
                confidence_score = ?, review_notes = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contract.getContractTitle());
            ps.setString(2, contract.getContractType());
            ps.setString(3, contract.getPartyFirst());
            ps.setString(4, contract.getPartySecond());
            ps.setObject(5, contract.getEffectiveDate());
            ps.setObject(6, contract.getExpirationDate());
            ps.setBoolean(7, contract.getAutoRenewal() != null && contract.getAutoRenewal());
            ps.setObject(8, contract.getRenewalNoticePeriodDays());
            ps.setObject(9, contract.getRenewalNoticeDeadline());
            ps.setString(10, contract.getRenewalTerms());
            ps.setString(11, contract.getPaymentTerms());
            ps.setString(12, contract.getPenaltiesClause());
            ps.setString(13, contract.getTerminationConditions());
            ps.setString(14, contract.getExtractionStatus());
            ps.setString(15, contract.getReviewStatus());
            ps.setBigDecimal(16, contract.getConfidenceScore());
            ps.setString(17, contract.getReviewNotes());
            ps.setLong(18, contract.getId());

            ps.executeUpdate();
        }
    }

    public void updateReviewStatus(Long contractId, String reviewStatus, String notes) throws SQLException {
        String sql = "UPDATE contracts SET review_status = ?, review_notes = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reviewStatus);
            ps.setString(2, notes);
            ps.setLong(3, contractId);
            ps.executeUpdate();
        }
    }

    public List<Contract> findUpcomingRenewals(int limit) throws SQLException {
        String sql = """
            SELECT * FROM contracts 
            WHERE expiration_date IS NOT NULL 
              AND expiration_date >= CURRENT_DATE()
            ORDER BY expiration_date ASC 
            LIMIT ?
        """;
        List<Contract> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToContract(rs));
                }
            }
        }
        return list;
    }

    public long countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM contracts";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public long countPendingReview() throws SQLException {
        String sql = "SELECT COUNT(*) FROM contracts WHERE review_status IN ('NEEDS_REVIEW', 'PARTIALLY_APPROVED')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public long countUpcomingRenewals() throws SQLException {
        String sql = "SELECT COUNT(*) FROM contracts WHERE expiration_date IS NOT NULL AND expiration_date >= CURRENT_DATE()";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    public Map<String, Integer> countByType() throws SQLException {
        String sql = "SELECT contract_type, COUNT(*) AS cnt FROM contracts GROUP BY contract_type";
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String type = rs.getString("contract_type");
                map.put(type != null ? type : "Other", rs.getInt("cnt"));
            }
        }
        return map;
    }

    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM contracts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Contract mapResultSetToContract(ResultSet rs) throws SQLException {
        Contract c = new Contract();
        c.setId(rs.getLong("id"));
        c.setFileName(rs.getString("file_name"));
        c.setStoredFilePath(rs.getString("stored_file_path"));
        c.setFileSizeBytes(rs.getLong("file_size_bytes"));
        c.setFileHashSha256(rs.getString("file_hash_sha256"));

        c.setContractTitle(rs.getString("contract_title"));
        c.setContractType(rs.getString("contract_type"));
        c.setPartyFirst(rs.getString("party_first"));
        c.setPartySecond(rs.getString("party_second"));

        Date effDate = rs.getDate("effective_date");
        if (effDate != null) c.setEffectiveDate(effDate.toLocalDate());

        Date expDate = rs.getDate("expiration_date");
        if (expDate != null) c.setExpirationDate(expDate.toLocalDate());

        c.setAutoRenewal(rs.getBoolean("auto_renewal"));
        int noticePeriod = rs.getInt("renewal_notice_period_days");
        if (!rs.wasNull()) c.setRenewalNoticePeriodDays(noticePeriod);

        Date noticeDeadline = rs.getDate("renewal_notice_deadline");
        if (noticeDeadline != null) c.setRenewalNoticeDeadline(noticeDeadline.toLocalDate());

        c.setRenewalTerms(rs.getString("renewal_terms"));
        c.setPaymentTerms(rs.getString("payment_terms"));
        c.setPenaltiesClause(rs.getString("penalties_clause"));
        c.setTerminationConditions(rs.getString("termination_conditions"));

        c.setExtractionStatus(rs.getString("extraction_status"));
        c.setReviewStatus(rs.getString("review_status"));
        c.setConfidenceScore(rs.getBigDecimal("confidence_score"));
        c.setReviewNotes(rs.getString("review_notes"));
        c.setRawTextExcerpt(rs.getString("raw_text_excerpt"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) c.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) c.setUpdatedAt(updatedAt.toLocalDateTime());

        return c;
    }
}

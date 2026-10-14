package com.contractiq.dao;

import com.contractiq.config.DatabaseConfig;
import com.contractiq.model.ActivityLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityDao {
    private static final Logger logger = LoggerFactory.getLogger(ActivityDao.class);

    public void log(ActivityLog log) {
        String sql = "INSERT INTO activity_logs (contract_id, action_type, description, actor, ip_address) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (log.getContractId() != null) {
                ps.setLong(1, log.getContractId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, log.getActionType());
            ps.setString(3, log.getDescription());
            ps.setString(4, log.getActor() != null ? log.getActor() : "System");
            ps.setString(5, log.getIpAddress());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to write activity log: {}", e.getMessage());
        }
    }

    public List<ActivityLog> findRecent(int limit) throws SQLException {
        String sql = "SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToActivityLog(rs));
                }
            }
        }
        return list;
    }

    public List<ActivityLog> findByContractId(Long contractId) throws SQLException {
        String sql = "SELECT * FROM activity_logs WHERE contract_id = ? ORDER BY created_at DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToActivityLog(rs));
                }
            }
        }
        return list;
    }

    private ActivityLog mapResultSetToActivityLog(ResultSet rs) throws SQLException {
        ActivityLog l = new ActivityLog();
        l.setId(rs.getLong("id"));
        long cId = rs.getLong("contract_id");
        if (!rs.wasNull()) l.setContractId(cId);
        l.setActionType(rs.getString("action_type"));
        l.setDescription(rs.getString("description"));
        l.setActor(rs.getString("actor"));
        l.setIpAddress(rs.getString("ip_address"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) l.setCreatedAt(ts.toLocalDateTime());
        return l;
    }
}

package com.minestorm.reportsystem.common.db;

import com.minestorm.reportsystem.common.model.Report;
import com.minestorm.reportsystem.common.model.ReportCategory;
import com.minestorm.reportsystem.common.model.ChatRecord;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public final class DatabaseManager {
    private final Logger logger;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "MSRS-DB"); t.setDaemon(true); return t;
    });
    private HikariDataSource ds;

    public DatabaseManager(Logger logger) { this.logger = logger; }

    public void connect(String host, int port, String name, String user, String pass,
                        int poolSize, boolean ssl) throws SQLException {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(5000);
        cfg.setPoolName("MineStormRS");
        this.ds = new HikariDataSource(cfg);
        createTables();
        logger.info("[MSRS] MySQL connected to " + host + ":" + port + "/" + name);
    }

    public void shutdown() {
        executor.shutdown();
        if (ds != null && !ds.isClosed()) ds.close();
    }

    private void createTables() throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_reports (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "reporter_uuid VARCHAR(36) NOT NULL," +
                "reporter_name VARCHAR(16) NOT NULL," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "category VARCHAR(32) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "INDEX idx_reporter (reporter_uuid)," +
                "INDEX idx_target (target_uuid)," +
                "INDEX idx_created (created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_chat_logs (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "uuid VARCHAR(36) NOT NULL," +
                "name VARCHAR(16) NOT NULL," +
                "channel VARCHAR(16) NOT NULL," +
                "target VARCHAR(16) NULL," +
                "message TEXT NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "INDEX idx_chat_uuid (uuid, created_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private <T> CompletableFuture<T> submit(SqlTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = ds.getConnection()) {
                return task.run(c);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    // ─── Reports ───────────────────────────────────────────
    public CompletableFuture<Integer> insertReport(UUID reporterUuid, String reporterName,
                                                   UUID targetUuid, String targetName,
                                                   ReportCategory category, long createdAt,
                                                   boolean rejectDuplicates) {
        return submit(c -> {
            if (rejectDuplicates) {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM rs_reports WHERE reporter_uuid=? AND target_uuid=?")) {
                    ps.setString(1, reporterUuid.toString());
                    ps.setString(2, targetUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return -1;
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rs_reports (reporter_uuid, reporter_name, target_uuid, target_name, category, created_at) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, reporterUuid.toString());
                ps.setString(2, reporterName);
                ps.setString(3, targetUuid.toString());
                ps.setString(4, targetName);
                ps.setString(5, category.name());
                ps.setLong(6, createdAt);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    return rs.next() ? rs.getInt(1) : -1;
                }
            }
        });
    }

    public CompletableFuture<List<Report>> getReports() {
        return submit(c -> {
            List<Report> list = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT id, reporter_uuid, reporter_name, target_uuid, target_name, category, created_at " +
                    "FROM rs_reports ORDER BY created_at ASC, id ASC")) {
                while (rs.next()) {
                    ReportCategory cat = ReportCategory.fromName(rs.getString("category"));
                    if (cat == null) continue;
                    list.add(new Report(rs.getInt("id"),
                            UUID.fromString(rs.getString("reporter_uuid")),
                            rs.getString("reporter_name"),
                            UUID.fromString(rs.getString("target_uuid")),
                            rs.getString("target_name"),
                            cat, rs.getLong("created_at")));
                }
            }
            return list;
        });
    }

    public CompletableFuture<Boolean> deleteReport(int id) {
        return submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM rs_reports WHERE id=?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    // ─── Chat ──────────────────────────────────────────────
    public CompletableFuture<Void> insertChatBatch(List<ChatRecord> batch) {
        return submit(c -> {
            boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rs_chat_logs (uuid, name, channel, target, message, created_at) VALUES (?,?,?,?,?,?)")) {
                for (ChatRecord r : batch) {
                    ps.setString(1, r.uuid().toString());
                    ps.setString(2, r.name());
                    ps.setString(3, r.channel());
                    ps.setString(4, r.target());
                    ps.setString(5, r.message());
                    ps.setLong(6, r.time());
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(prev);
            }
            return null;
        });
    }

    // ─── Cleanup ───────────────────────────────────────────
    public CompletableFuture<Integer> cleanup(long reportCutoff, long chatCutoff) {
        return submit(c -> {
            int total = 0;
            if (reportCutoff > 0) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM rs_reports WHERE created_at < ?")) {
                    ps.setLong(1, reportCutoff);
                    total += ps.executeUpdate();
                }
            }
            if (chatCutoff > 0) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM rs_chat_logs WHERE created_at < ?")) {
                    ps.setLong(1, chatCutoff);
                    total += ps.executeUpdate();
                }
            }
            return total;
        });
    }
}

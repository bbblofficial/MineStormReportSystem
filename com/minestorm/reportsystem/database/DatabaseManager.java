package com.minestorm.reportsystem.database;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.ChatChannel;
import com.minestorm.reportsystem.model.ChatRecord;
import com.minestorm.reportsystem.model.LogEntry;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportCategory;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * DatabaseManager — خودکار جدول‌ها رو می‌سازه و در صورت قطعی وصل می‌شه.
 *
 * ویژگی‌ها:
 *  • createTables() هنگام connect() و هر بار reconnect()
 *  • اگر MySQL قطع شد، خودکار دوباره وصل می‌شه
 *  • اگر SQLite driver نبود، پیام واضح می‌ده
 *  • اگر جدول‌ها نبودن، دوباره ساخته می‌شن
 */
public final class DatabaseManager {

    private final ReportSystem plugin;
    private final ExecutorService executor;
    private volatile Connection connection;
    private volatile boolean mysql;

    public DatabaseManager(ReportSystem plugin) {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ReportSystem-DB");
                t.setDaemon(true);
                return t;
            }
        });
        this.plugin = plugin;
    }

    /**
     * اتصال و ساخت خودکار جدول‌ها.
     * در صورت خطا، استثنا پرتاب می‌شود تا plugin تصمیم بگیرد.
     */
    public void connect() throws SQLException {
        this.mysql = plugin.getConfig().getString("database.type", "SQLITE")
                .equalsIgnoreCase("MYSQL");
        open();
        createTables();
        plugin.getLogger().info("Database connected (" + (mysql ? "MySQL" : "SQLite") + "). Tables verified.");
    }

    private void open() throws SQLException {
        FileConfiguration cfg = plugin.getConfig();
        if (mysql) {
            String host = cfg.getString("database.mysql.host", "localhost");
            int port = cfg.getInt("database.mysql.port", 3306);
            String db = cfg.getString("database.mysql.database", "reports");
            String user = cfg.getString("database.mysql.username", "root");
            String pass = cfg.getString("database.mysql.password", "");
            boolean ssl = cfg.getBoolean("database.mysql.use-ssl", false);

            String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=" + ssl
                    + "&autoReconnect=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=UTC";

            try {
                // اطمینان از بارگذاری درایور
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                plugin.getLogger().warning("MySQL driver not found on classpath. Trying legacy driver...");
                try { Class.forName("com.mysql.jdbc.Driver"); }
                catch (ClassNotFoundException ignored) {}
            }

            plugin.getLogger().info("Connecting to MySQL " + host + ":" + port + "/" + db + " as " + user + "...");
            this.connection = DriverManager.getConnection(url, user, pass);
        } else {
            try { Class.forName("org.sqlite.JDBC"); }
            catch (ClassNotFoundException ex) {
                throw new SQLException(
                        "SQLite JDBC driver not found. Install sqlite-jdbc or switch to MySQL.", ex);
            }
            File folder = plugin.getDataFolder();
            if (!folder.exists() && !folder.mkdirs())
                throw new SQLException("Could not create plugin data folder.");
            File file = new File(folder, cfg.getString("database.sqlite-file", "reports.db"));
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        }
    }

    private synchronized Connection conn() throws SQLException {
        if (connection == null || connection.isClosed()
                || (mysql && !connection.isValid(2))) {
            plugin.getLogger().warning("Database connection lost. Reconnecting...");
            try {
                open();
                createTables();
                plugin.getLogger().info("Database reconnected.");
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Reconnect failed", ex);
                throw ex;
            }
        }
        return connection;
    }

    /**
     * ساخت تمام جدول‌های موردنیاز. اگر از قبل موجود باشن، دوباره ساخته نمی‌شن
     * (IF NOT EXISTS).
     */
    public void createTables() throws SQLException {
        String pk = mysql ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String chatIndex = mysql ? ", INDEX idx_chat_uuid (uuid, created_at)" : "";
        String grimIndex = mysql ? ", INDEX idx_grim_uuid (uuid, created_at)" : "";

        Statement st = conn().createStatement();
        try {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_reports ("
                    + "id " + pk + ","
                    + " reporter_uuid VARCHAR(36) NOT NULL,"
                    + " reporter_name VARCHAR(16) NOT NULL,"
                    + " target_uuid VARCHAR(36) NOT NULL,"
                    + " target_name VARCHAR(16) NOT NULL,"
                    + " category VARCHAR(32) NOT NULL,"
                    + " created_at BIGINT NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_chat_logs ("
                    + "id " + pk + ","
                    + " uuid VARCHAR(36) NOT NULL,"
                    + " name VARCHAR(16) NOT NULL,"
                    + " channel VARCHAR(16) NOT NULL,"
                    + " target VARCHAR(16) NULL,"
                    + " message TEXT NOT NULL,"
                    + " created_at BIGINT NOT NULL"
                    + chatIndex + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_grim_alerts ("
                    + "id " + pk + ","
                    + " uuid VARCHAR(36) NOT NULL,"
                    + " name VARCHAR(16) NOT NULL,"
                    + " check_name VARCHAR(64) NOT NULL,"
                    + " verbose TEXT NOT NULL,"
                    + " created_at BIGINT NOT NULL"
                    + grimIndex + ")");

            if (!mysql) {
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chat_uuid ON rs_chat_logs (uuid, created_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_grim_uuid ON rs_grim_alerts (uuid, created_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reports_target ON rs_reports (target_uuid)");
            }
        } finally { closeQuietly(st); }
    }

    /** دستی جدول‌ها رو دوباره چک می‌کنه (برای /my-task reload). */
    public void verify() {
        try {
            createTables();
            plugin.getLogger().info("Database tables verified.");
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not verify tables: " + ex.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10L, TimeUnit.SECONDS))
                plugin.getLogger().warning("Database queue did not finish in time.");
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Error closing database: " + ex.getMessage());
        }
    }

    private <T> CompletableFuture<T> submit(final SqlTask<T> task) {
        return CompletableFuture.supplyAsync(new java.util.function.Supplier<T>() {
            public T get() {
                try { return task.run(conn()); }
                catch (SQLException ex) { throw new CompletionException(ex); }
            }
        }, executor);
    }

    // ─────────────── Reports ───────────────

    public CompletableFuture<Integer> insertReport(final UUID reporterUuid, final String reporterName,
            final UUID targetUuid, final String targetName, final ReportCategory category,
            final long createdAt, final boolean rejectDuplicates) {
        return submit(new SqlTask<Integer>() {
            public Integer run(Connection c) throws SQLException {
                if (rejectDuplicates) {
                    PreparedStatement ps = c.prepareStatement(
                            "SELECT 1 FROM rs_reports WHERE reporter_uuid = ? AND target_uuid = ?");
                    try {
                        ps.setString(1, reporterUuid.toString());
                        ps.setString(2, targetUuid.toString());
                        ResultSet rs = ps.executeQuery();
                        try { if (rs.next()) return Integer.valueOf(-1); }
                        finally { closeQuietly(rs); }
                    } finally { closeQuietly(ps); }
                }
                PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO rs_reports (reporter_uuid, reporter_name, target_uuid, target_name, category, created_at) VALUES (?, ?, ?, ?, ?, ?)");
                try {
                    ps.setString(1, reporterUuid.toString());
                    ps.setString(2, reporterName);
                    ps.setString(3, targetUuid.toString());
                    ps.setString(4, targetName);
                    ps.setString(5, category.name());
                    ps.setLong(6, createdAt);
                    ps.executeUpdate();
                } finally { closeQuietly(ps); }

                Statement st = c.createStatement();
                try {
                    ResultSet rs = st.executeQuery(mysql
                            ? "SELECT LAST_INSERT_ID()" : "SELECT last_insert_rowid()");
                    try { return Integer.valueOf(rs.next() ? rs.getInt(1) : -1); }
                    finally { closeQuietly(rs); }
                } finally { closeQuietly(st); }
            }
        });
    }

    public CompletableFuture<List<Report>> getReports() {
        return submit(new SqlTask<List<Report>>() {
            public List<Report> run(Connection c) throws SQLException {
                List<Report> reports = new ArrayList<Report>();
                Statement st = c.createStatement();
                try {
                    ResultSet rs = st.executeQuery(
                            "SELECT id, reporter_uuid, reporter_name, target_uuid, target_name, category, created_at FROM rs_reports ORDER BY created_at ASC, id ASC");
                    try {
                        while (rs.next()) {
                            ReportCategory cat = ReportCategory.fromName(rs.getString("category"));
                            if (cat == null) continue;
                            reports.add(new Report(rs.getInt("id"),
                                    UUID.fromString(rs.getString("reporter_uuid")),
                                    rs.getString("reporter_name"),
                                    UUID.fromString(rs.getString("target_uuid")),
                                    rs.getString("target_name"),
                                    cat, rs.getLong("created_at")));
                        }
                    } finally { closeQuietly(rs); }
                } finally { closeQuietly(st); }
                return reports;
            }
        });
    }

    public CompletableFuture<Boolean> deleteReport(final int id) {
        return submit(new SqlTask<Boolean>() {
            public Boolean run(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("DELETE FROM rs_reports WHERE id = ?");
                try {
                    ps.setInt(1, id);
                    return Boolean.valueOf(ps.executeUpdate() > 0);
                } finally { closeQuietly(ps); }
            }
        });
    }

    // ─────────────── Chat ───────────────

    public CompletableFuture<Void> insertChatBatch(final List<ChatRecord> batch) {
        return submit(new SqlTask<Void>() {
            public Void run(Connection c) throws SQLException {
                boolean prevAuto = c.getAutoCommit();
                c.setAutoCommit(false);
                try {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO rs_chat_logs (uuid, name, channel, target, message, created_at) VALUES (?, ?, ?, ?, ?, ?)");
                    try {
                        for (ChatRecord r : batch) {
                            ps.setString(1, r.uuid().toString());
                            ps.setString(2, r.name());
                            ps.setString(3, r.channel().name());
                            ps.setString(4, r.target());
                            ps.setString(5, r.message());
                            ps.setLong(6, r.time());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                        c.commit();
                    } finally { closeQuietly(ps); }
                } catch (SQLException ex) {
                    try { c.rollback(); } catch (SQLException ignored) {}
                    throw ex;
                } finally { c.setAutoCommit(prevAuto); }
                return null;
            }
        });
    }

    public CompletableFuture<List<LogEntry>> getChatLogs(final UUID uuid, final String playerName,
            final ChatChannel channel, final int limit) {
        return submit(new SqlTask<List<LogEntry>>() {
            public List<LogEntry> run(Connection c) throws SQLException {
                String select = "SELECT uuid, name, channel, target, message, created_at FROM rs_chat_logs WHERE ";
                String order = " ORDER BY created_at DESC, id DESC LIMIT ?";
                String sql;
                if (channel == null) sql = select + "uuid = ?" + order;
                else if (channel == ChatChannel.PRIVATE)
                    sql = select + "channel = 'PRIVATE' AND (uuid = ? OR LOWER(target) = LOWER(?))" + order;
                else sql = select + "uuid = ? AND channel = ?" + order;

                List<LogEntry> entries = new ArrayList<LogEntry>();
                PreparedStatement ps = c.prepareStatement(sql);
                try {
                    int idx = 1;
                    ps.setString(idx++, uuid.toString());
                    if (channel == ChatChannel.PRIVATE) ps.setString(idx++, playerName);
                    else if (channel != null) ps.setString(idx++, channel.name());
                    ps.setInt(idx, limit);
                    ResultSet rs = ps.executeQuery();
                    try {
                        while (rs.next()) entries.add(toChatEntry(rs, uuid));
                    } finally { closeQuietly(rs); }
                } finally { closeQuietly(ps); }
                return entries;
            }
        });
    }

    private LogEntry toChatEntry(ResultSet rs, UUID viewed) throws SQLException {
        String channel = rs.getString("channel");
        String target = rs.getString("target");
        String title;
        if ("PRIVATE".equals(channel)) {
            boolean sent = viewed.toString().equals(rs.getString("uuid"));
            title = sent ? ("DM to " + (target == null ? "(reply)" : target))
                         : ("DM from " + rs.getString("name"));
        } else if ("GUILD".equals(channel)) title = "Guild Chat";
        else title = "Public Chat";
        return new LogEntry(rs.getLong("created_at"), title, rs.getString("message"));
    }

    // ─────────────── Grim ───────────────

    public CompletableFuture<Void> insertGrimAlert(final UUID uuid, final String name,
            final String checkName, final String verbose, final long time) {
        return submit(new SqlTask<Void>() {
            public Void run(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO rs_grim_alerts (uuid, name, check_name, verbose, created_at) VALUES (?, ?, ?, ?, ?)");
                try {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name == null ? "unknown" : name);
                    ps.setString(3, checkName.length() > 64 ? checkName.substring(0, 64) : checkName);
                    ps.setString(4, verbose);
                    ps.setLong(5, time);
                    ps.executeUpdate();
                } finally { closeQuietly(ps); }
                return null;
            }
        });
    }

    public CompletableFuture<List<LogEntry>> getGrimAlerts(final UUID uuid, final int limit) {
        return submit(new SqlTask<List<LogEntry>>() {
            public List<LogEntry> run(Connection c) throws SQLException {
                List<LogEntry> entries = new ArrayList<LogEntry>();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT check_name, verbose, created_at FROM rs_grim_alerts WHERE uuid = ? ORDER BY created_at DESC, id DESC LIMIT ?");
                try {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, limit);
                    ResultSet rs = ps.executeQuery();
                    try {
                        while (rs.next()) entries.add(new LogEntry(
                                rs.getLong("created_at"),
                                rs.getString("check_name"),
                                rs.getString("verbose")));
                    } finally { closeQuietly(rs); }
                } finally { closeQuietly(ps); }
                return entries;
            }
        });
    }

    // ─────────────── Cleanup ───────────────

    public CompletableFuture<int[]> cleanup(final long reportCutoff, final long chatCutoff, final long grimCutoff) {
        return submit(new SqlTask<int[]>() {
            public int[] run(Connection c) throws SQLException {
                return new int[] {
                        deleteOlderThan(c, "rs_reports", reportCutoff),
                        deleteOlderThan(c, "rs_chat_logs", chatCutoff),
                        deleteOlderThan(c, "rs_grim_alerts", grimCutoff)
                };
            }
        });
    }

    private int deleteOlderThan(Connection c, String table, long cutoff) throws SQLException {
        if (cutoff <= 0L) return 0;
        PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE created_at < ?");
        try {
            ps.setLong(1, cutoff);
            return ps.executeUpdate();
        } finally { closeQuietly(ps); }
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c != null) try { c.close(); } catch (Exception ignored) {}
    }
}

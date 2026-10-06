package com.minestorm.reportsystem.database;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.ChatChannel;
import com.minestorm.reportsystem.model.ChatRecord;
import com.minestorm.reportsystem.model.LogEntry;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportCategory;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public final class DatabaseManager {
    private final ReportSystem plugin;
    private final ExecutorService executor;
    private volatile Connection connection;
    private volatile boolean mysql;

    public DatabaseManager(ReportSystem plugin) {
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "MSRS-DB");
                t.setDaemon(true);
                return t;
            }
        });
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        this.mysql = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("MYSQL");
        open();
        createTables();
        plugin.getLogger().info("Database connected (" + (mysql ? "MySQL" : "SQLite") + "). Tables verified.");
    }

    private void open() throws SQLException {
        FileConfiguration c = plugin.getConfig();
        if (mysql) {
            String host = c.getString("database.mysql.host", "localhost");
            int port = c.getInt("database.mysql.port", 3306);
            String db = c.getString("database.mysql.database", "reports");
            String user = c.getString("database.mysql.username", "root");
            String pass = c.getString("database.mysql.password", "");
            boolean ssl = c.getBoolean("database.mysql.use-ssl", false);
            String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8&serverTimezone=UTC";
            try { Class.forName("com.mysql.cj.jdbc.Driver"); }
            catch (ClassNotFoundException e) {
                try { Class.forName("com.mysql.jdbc.Driver"); } catch (ClassNotFoundException ignored) {}
            }
            plugin.getLogger().info("Connecting MySQL " + host + ":" + port + "/" + db);
            connection = DriverManager.getConnection(url, user, pass);
        } else {
            try { Class.forName("org.sqlite.JDBC"); }
            catch (ClassNotFoundException e) {
                throw new SQLException("SQLite driver missing. Use MySQL or add sqlite-jdbc.", e);
            }
            File folder = plugin.getDataFolder();
            if (!folder.exists() && !folder.mkdirs()) throw new SQLException("Cannot create data folder.");
            File f = new File(folder, c.getString("database.sqlite-file", "reports.db"));
            connection = DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath());
        }
    }

    private synchronized Connection conn() throws SQLException {
        if (connection == null || connection.isClosed() || (mysql && !connection.isValid(2))) {
            plugin.getLogger().warning("DB connection lost — reconnecting...");
            open();
            createTables();
            plugin.getLogger().info("DB reconnected.");
        }
        return connection;
    }

    public void createTables() throws SQLException {
        String pk = mysql ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String chatIdx = mysql ? ", INDEX idx_chat_uuid (uuid, created_at)" : "";
        String grimIdx = mysql ? ", INDEX idx_grim_uuid (uuid, created_at)" : "";
        Statement st = conn().createStatement();
        try {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_reports (id " + pk
                + ", reporter_uuid VARCHAR(36) NOT NULL, reporter_name VARCHAR(16) NOT NULL,"
                + " target_uuid VARCHAR(36) NOT NULL, target_name VARCHAR(16) NOT NULL,"
                + " category VARCHAR(32) NOT NULL, created_at BIGINT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_chat_logs (id " + pk
                + ", uuid VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL,"
                + " channel VARCHAR(16) NOT NULL, target VARCHAR(16) NULL,"
                + " message TEXT NOT NULL, created_at BIGINT NOT NULL" + chatIdx + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_grim_alerts (id " + pk
                + ", uuid VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL,"
                + " check_name VARCHAR(64) NOT NULL, verbose TEXT NOT NULL,"
                + " created_at BIGINT NOT NULL" + grimIdx + ")");
            if (!mysql) {
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_chat_uuid ON rs_chat_logs (uuid, created_at)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_grim_uuid ON rs_grim_alerts (uuid, created_at)");
            }
        } finally { close(st); }
    }

    public void verify() {
        try { createTables(); plugin.getLogger().info("Tables verified."); }
        catch (SQLException e) { plugin.getLogger().warning("Verify failed: " + e.getMessage()); }
    }

    public void shutdown() {
        executor.shutdown();
        try { if (!executor.awaitTermination(10, TimeUnit.SECONDS))
            plugin.getLogger().warning("DB queue timeout."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { plugin.getLogger().warning("Close err: " + e.getMessage()); }
    }

    private <T> CompletableFuture<T> submit(final SqlTask<T> task) {
        return CompletableFuture.supplyAsync(new Supplier<T>() {
            public T get() {
                try { return task.run(conn()); }
                catch (SQLException e) { throw new CompletionException(e); }
            }
        }, executor);
    }

    public CompletableFuture<Integer> insertReport(final UUID rU, final String rN, final UUID tU,
            final String tN, final ReportCategory cat, final long at, final boolean noDup) {
        return submit(new SqlTask<Integer>() {
            public Integer run(Connection c) throws SQLException {
                if (noDup) {
                    PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM rs_reports WHERE reporter_uuid=? AND target_uuid=?");
                    try {
                        ps.setString(1, rU.toString()); ps.setString(2, tU.toString());
                        ResultSet rs = ps.executeQuery();
                        try { if (rs.next()) return -1; } finally { close(rs); }
                    } finally { close(ps); }
                }
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rs_reports (reporter_uuid, reporter_name, target_uuid, target_name, category, created_at) VALUES (?,?,?,?,?,?)");
                try {
                    ps.setString(1, rU.toString()); ps.setString(2, rN);
                    ps.setString(3, tU.toString()); ps.setString(4, tN);
                    ps.setString(5, cat.name()); ps.setLong(6, at);
                    ps.executeUpdate();
                } finally { close(ps); }
                Statement st = c.createStatement();
                try {
                    ResultSet rs = st.executeQuery(mysql ? "SELECT LAST_INSERT_ID()" : "SELECT last_insert_rowid()");
                    try { return rs.next() ? rs.getInt(1) : -1; } finally { close(rs); }
                } finally { close(st); }
            }
        });
    }

    public CompletableFuture<List<Report>> getReports() {
        return submit(new SqlTask<List<Report>>() {
            public List<Report> run(Connection c) throws SQLException {
                List<Report> list = new ArrayList<Report>();
                Statement st = c.createStatement();
                try {
                    ResultSet rs = st.executeQuery(
                        "SELECT id,reporter_uuid,reporter_name,target_uuid,target_name,category,created_at FROM rs_reports ORDER BY created_at ASC, id ASC");
                    try {
                        while (rs.next()) {
                            ReportCategory cat = ReportCategory.fromName(rs.getString("category"));
                            if (cat == null) continue;
                            list.add(new Report(rs.getInt("id"),
                                UUID.fromString(rs.getString("reporter_uuid")),
                                rs.getString("reporter_name"),
                                UUID.fromString(rs.getString("target_uuid")),
                                rs.getString("target_name"), cat, rs.getLong("created_at")));
                        }
                    } finally { close(rs); }
                } finally { close(st); }
                return list;
            }
        });
    }

    public CompletableFuture<Boolean> deleteReport(final int id) {
        return submit(new SqlTask<Boolean>() {
            public Boolean run(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement("DELETE FROM rs_reports WHERE id=?");
                try { ps.setInt(1, id); return ps.executeUpdate() > 0; }
                finally { close(ps); }
            }
        });
    }

    public CompletableFuture<Void> insertChatBatch(final List<ChatRecord> batch) {
        return submit(new SqlTask<Void>() {
            public Void run(Connection c) throws SQLException {
                boolean prev = c.getAutoCommit();
                c.setAutoCommit(false);
                try {
                    PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO rs_chat_logs (uuid,name,channel,target,message,created_at) VALUES (?,?,?,?,?,?)");
                    try {
                        for (ChatRecord r : batch) {
                            ps.setString(1, r.uuid().toString()); ps.setString(2, r.name());
                            ps.setString(3, r.channel().name()); ps.setString(4, r.target());
                            ps.setString(5, r.message()); ps.setLong(6, r.time());
                            ps.addBatch();
                        }
                        ps.executeBatch(); c.commit();
                    } finally { close(ps); }
                } catch (SQLException e) {
                    try { c.rollback(); } catch (SQLException ignored) {}
                    throw e;
                } finally { c.setAutoCommit(prev); }
                return null;
            }
        });
    }

    public CompletableFuture<List<LogEntry>> getChatLogs(final UUID uuid, final String name,
            final ChatChannel ch, final int limit) {
        return submit(new SqlTask<List<LogEntry>>() {
            public List<LogEntry> run(Connection c) throws SQLException {
                String sel = "SELECT uuid,name,channel,target,message,created_at FROM rs_chat_logs WHERE ";
                String ord = " ORDER BY created_at DESC, id DESC LIMIT ?";
                String sql;
                if (ch == null) sql = sel + "uuid=?" + ord;
                else if (ch == ChatChannel.PRIVATE)
                    sql = sel + "channel='PRIVATE' AND (uuid=? OR LOWER(target)=LOWER(?))" + ord;
                else sql = sel + "uuid=? AND channel=?" + ord;
                List<LogEntry> out = new ArrayList<LogEntry>();
                PreparedStatement ps = c.prepareStatement(sql);
                try {
                    int i = 1;
                    ps.setString(i++, uuid.toString());
                    if (ch == ChatChannel.PRIVATE) ps.setString(i++, name);
                    else if (ch != null) ps.setString(i++, ch.name());
                    ps.setInt(i, limit);
                    ResultSet rs = ps.executeQuery();
                    try {
                        while (rs.next()) out.add(toEntry(rs, uuid));
                    } finally { close(rs); }
                } finally { close(ps); }
                return out;
            }
        });
    }

    private LogEntry toEntry(ResultSet rs, UUID viewed) throws SQLException {
        String ch = rs.getString("channel");
        String t = rs.getString("target");
        String title;
        if ("PRIVATE".equals(ch)) {
            boolean sent = viewed.toString().equals(rs.getString("uuid"));
            title = sent ? ("DM to " + (t == null ? "(reply)" : t))
                         : ("DM from " + rs.getString("name"));
        } else if ("GUILD".equals(ch)) title = "Guild Chat";
        else title = "Public Chat";
        return new LogEntry(rs.getLong("created_at"), title, rs.getString("message"));
    }

    public CompletableFuture<Void> insertGrimAlert(final UUID u, final String n,
            final String chk, final String vrb, final long t) {
        return submit(new SqlTask<Void>() {
            public Void run(Connection c) throws SQLException {
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rs_grim_alerts (uuid,name,check_name,verbose,created_at) VALUES (?,?,?,?,?)");
                try {
                    ps.setString(1, u.toString());
                    ps.setString(2, n == null ? "unknown" : n);
                    ps.setString(3, chk.length() > 64 ? chk.substring(0, 64) : chk);
                    ps.setString(4, vrb); ps.setLong(5, t);
                    ps.executeUpdate();
                } finally { close(ps); }
                return null;
            }
        });
    }

    public CompletableFuture<List<LogEntry>> getGrimAlerts(final UUID u, final int limit) {
        return submit(new SqlTask<List<LogEntry>>() {
            public List<LogEntry> run(Connection c) throws SQLException {
                List<LogEntry> out = new ArrayList<LogEntry>();
                PreparedStatement ps = c.prepareStatement(
                    "SELECT check_name,verbose,created_at FROM rs_grim_alerts WHERE uuid=? ORDER BY created_at DESC, id DESC LIMIT ?");
                try {
                    ps.setString(1, u.toString()); ps.setInt(2, limit);
                    ResultSet rs = ps.executeQuery();
                    try {
                        while (rs.next()) out.add(new LogEntry(
                            rs.getLong("created_at"), rs.getString("check_name"),
                            rs.getString("verbose")));
                    } finally { close(rs); }
                } finally { close(ps); }
                return out;
            }
        });
    }

    public CompletableFuture<int[]> cleanup(final long rC, final long cC, final long gC) {
        return submit(new SqlTask<int[]>() {
            public int[] run(Connection c) throws SQLException {
                return new int[] {
                    del(c, "rs_reports", rC), del(c, "rs_chat_logs", cC), del(c, "rs_grim_alerts", gC)
                };
            }
        });
    }

    private int del(Connection c, String table, long cutoff) throws SQLException {
        if (cutoff <= 0) return 0;
        PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE created_at < ?");
        try { ps.setLong(1, cutoff); return ps.executeUpdate(); }
        finally { close(ps); }
    }

    private static void close(AutoCloseable c) {
        if (c != null) try { c.close(); } catch (Exception ignored) {}
    }
}

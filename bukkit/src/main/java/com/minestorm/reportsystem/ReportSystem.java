package com.minestorm.reportsystem;

import java.util.function.BiConsumer;
import com.minestorm.reportsystem.command.MyTaskCommand;
import com.minestorm.reportsystem.command.ReportCommand;
import com.minestorm.reportsystem.database.DatabaseManager;
import com.minestorm.reportsystem.gui.GuiListener;
import com.minestorm.reportsystem.listener.CommandInterceptor;
import com.minestorm.reportsystem.service.ChatLogger;
import com.minestorm.reportsystem.service.CleanupTask;
import com.minestorm.reportsystem.service.GrimHook;
import com.minestorm.reportsystem.service.PunishmentManager;
import com.minestorm.reportsystem.service.ReportService;
import com.minestorm.reportsystem.service.WebhookService;
import com.minestorm.reportsystem.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ReportSystem extends JavaPlugin {

    private Messages messages;
    private DatabaseManager database;
    private PunishmentManager punishments;
    private ReportService reports;
    private ChatLogger chatLogger;
    private WebhookService webhook;
    private GrimHook grim;

    public void onEnable() {
        autoSetup();

        messages = new Messages(this);
        punishments = new PunishmentManager(this);
        punishments.load();

        database = new DatabaseManager(this);
        boolean dbOk = true;
        try {
            database.connect();
        } catch (SQLException ex) {
            dbOk = false;
            getLogger().log(Level.SEVERE, "═══════════════════════════════════════════");
            getLogger().log(Level.SEVERE, "  DATABASE CONNECTION FAILED");
            getLogger().log(Level.SEVERE, "  Config: plugins/MineStormReportSystem/config.yml");
            getLogger().log(Level.SEVERE, "  Error: " + ex.getMessage());
            getLogger().log(Level.SEVERE, "═══════════════════════════════════════════");
            getLogger().log(Level.SEVERE, "  Fix creds then run: /my-task reload");
        }

        webhook = new WebhookService(this);
        reports = new ReportService(this);
        chatLogger = new ChatLogger(this);
        chatLogger.reload();
        grim = new GrimHook(this);
        grim.hook();

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(chatLogger, this);
        getServer().getPluginManager().registerEvents(new CommandInterceptor(this), this);

        PluginCommand rc = getCommand("report");
        if (rc != null) {
            ReportCommand exec = new ReportCommand(this);
            rc.setExecutor(exec);
            rc.setTabCompleter(exec);
        }

        PluginCommand mc = getCommand("my-task");
        if (mc != null) {
            MyTaskCommand exec = new MyTaskCommand(this);
            mc.setExecutor(exec);
            mc.setTabCompleter(exec);
        }

        if (dbOk) {
            long f = Math.max(1L, getConfig().getLong("chat-logging.flush-interval-seconds", 5L)) * 20L;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
                public void run() { chatLogger.flush(); }
            }, f, f);
            long cl = Math.max(1L, getConfig().getLong("cleanup.interval-minutes", 30L)) * 60L * 20L;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new CleanupTask(this), 600L, cl);
        } else {
            getLogger().warning("Chat logging & cleanup disabled until DB is fixed.");
        }

        getLogger().info("ReportSystem enabled (DB=" + (dbOk ? "OK" : "FAILED") + ").");
    }

    public void onDisable() {
        if (chatLogger != null) chatLogger.flushBlocking();
        if (database != null) database.shutdown();
        getLogger().info("ReportSystem disabled.");
    }

    private void autoSetup() {
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("Could not create data folder!");
            }
            copyIfMissing("config.yml");
            copyIfMissing("punishments.yml");
            if (!getConfig().contains("database.type"))
                getConfig().set("database.type", "SQLITE");
            if (!getConfig().contains("database.sqlite-file"))
                getConfig().set("database.sqlite-file", "reports.db");
            saveConfig();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "autoSetup error (non-fatal):", t);
        }
    }

    private void copyIfMissing(String name) {
        try {
            java.io.File f = new java.io.File(getDataFolder(), name);
            if (f.exists()) return;
            InputStream in = getResource(name);
            if (in == null) {
                getLogger().warning("Resource " + name + " not in JAR — skipping.");
                return;
            }
            in.close();
            saveResource(name, false);
            getLogger().info("Created " + name);
        } catch (Exception e) {
            getLogger().warning("Could not create " + name + ": " + e.getMessage());
        }
    }

    public void reloadAll() {
        reloadConfig();
        try {
            if (database != null) database.connect();
        } catch (SQLException e) {
            getLogger().warning("Reload DB: " + e.getMessage());
        }
        punishments.load();
        chatLogger.reload();
    }

    public void runSync(Runnable task) {
        if (isEnabled()) getServer().getScheduler().runTask(this, task);
    }

    public <T> void callback(final CompletableFuture<T> future,
                             final Consumer<T> onOk, final Runnable onErr) {
        future.whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
            public void accept(final T result, final Throwable error) {
                runSync(new Runnable() {
                    public void run() {
                        if (error != null) onErr.run();
                        else onOk.accept(result);
                    }
                });
            }
        });
    }

    public Messages messages() { return messages; }
    public DatabaseManager database() { return database; }
    public PunishmentManager punishments() { return punishments; }
    public ReportService reports() { return reports; }
    public ChatLogger chatLogger() { return chatLogger; }
    public WebhookService webhook() { return webhook; }
    public GrimHook grim() { return grim; }
}

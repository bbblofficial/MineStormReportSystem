package com.minestorm.reportsystem;

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
import com.minestorm.reportsystem.util.StartupVerifier;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        // ─── Step 1: auto-setup ───
        try {
            new StartupVerifier(this).verifyAll();
        } catch (Throwable ex) {
            getLogger().log(Level.WARNING, "Startup verification error (continuing):", ex);
        }

        // ─── Step 2: init components ───
        messages = new Messages(this);
        punishments = new PunishmentManager(this);
        punishments.load();

        database = new DatabaseManager(this);
        boolean dbOk = true;
        try {
            database.connect();
        } catch (SQLException ex) {
            dbOk = false;
            getLogger().log(Level.SEVERE, "╔═══════════════════════════════════════════════════╗");
            getLogger().log(Level.SEVERE, "║  Database connection FAILED.                       ║");
            getLogger().log(Level.SEVERE, "║  Check plugins/MineStormReportSystem/config.yml    ║");
            getLogger().log(Level.SEVERE, "║  and make sure MySQL credentials are correct.      ║");
            getLogger().log(Level.SEVERE, "╚═══════════════════════════════════════════════════╝");
            getLogger().log(Level.SEVERE, "Error: " + ex.getMessage());
            // در حالت SQLite که همیشه باید کار کند.
            // اگر MySQL اشتباه بود ولی SQLite بود، ادامه می‌دهیم.
        }

        // ─── Step 3: سایر سرویس‌ها ───
        webhook = new WebhookService(this);
        reports = new ReportService(this);
        chatLogger = new ChatLogger(this);
        chatLogger.reload();
        grim = new GrimHook(this);
        grim.hook();

        // ─── Step 4: listeners ───
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(chatLogger, this);
        getServer().getPluginManager().registerEvents(new CommandInterceptor(this), this);

        // ─── Step 5: commands ───
        PluginCommand reportCmd = getCommand("report");
        if (reportCmd != null) {
            ReportCommand rc = new ReportCommand(this);
            reportCmd.setExecutor(rc);
            reportCmd.setTabCompleter(rc);
        } else {
            getLogger().warning("Command 'report' not registered in plugin.yml!");
        }

        PluginCommand taskCmd = getCommand("my-task");
        if (taskCmd != null) {
            MyTaskCommand mc = new MyTaskCommand(this);
            taskCmd.setExecutor(mc);
            taskCmd.setTabCompleter(mc);
        } else {
            getLogger().warning("Command 'my-task' not registered in plugin.yml!");
        }

        // ─── Step 6: scheduled tasks ───
        if (dbOk) {
            long flushTicks = Math.max(1L,
                    getConfig().getLong("chat-logging.flush-interval-seconds", 5L)) * 20L;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
                public void run() { chatLogger.flush(); }
            }, flushTicks, flushTicks);

            long cleanupTicks = Math.max(1L,
                    getConfig().getLong("cleanup.interval-minutes", 30L)) * 60L * 20L;
            getServer().getScheduler().runTaskTimerAsynchronously(this, new CleanupTask(this),
                    600L, cleanupTicks);
        } else {
            getLogger().warning("DB failed — chat logging & cleanup disabled until /my-task reload.");
        }

        getLogger().info("ReportSystem enabled (DB=" + (dbOk ? "OK" : "FAILED")
                + ", SQLite/MySQL=" + getConfig().getString("database.type", "SQLITE") + ").");
    }

    @Override
    public void onDisable() {
        if (chatLogger != null) chatLogger.flushBlocking();
        if (database != null) database.shutdown();
        getLogger().info("ReportSystem disabled.");
    }

    /**
     * Reload کامل — دیتابیس رو دوباره وصل می‌کنه و جدول‌ها رو چک می‌کنه.
     */
    public void reloadAll() {
        reloadConfig();
        new StartupVerifier(this).verifyAll();

        if (database != null) {
            database.verify();
        }

        punishments.load();
        chatLogger.reload();

        // اگه قبلاً DB وصل نبود، الان دوباره تلاش کن
        try {
            if (database != null) {
                database.connect();
                getLogger().info("Database reconnected after reload.");
            }
        } catch (SQLException ex) {
            getLogger().warning("Reload: could not connect to database: " + ex.getMessage());
        }
    }

    public void runSync(Runnable task) {
        if (isEnabled()) getServer().getScheduler().runTask(this, task);
    }

    public <T> void callback(final CompletableFuture<T> future,
                             final Consumer<T> onSuccess, final Runnable onFailure) {
        future.whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
            public void accept(final T result, final Throwable error) {
                runSync(new Runnable() {
                    public void run() {
                        if (error != null) onFailure.run();
                        else onSuccess.accept(result);
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

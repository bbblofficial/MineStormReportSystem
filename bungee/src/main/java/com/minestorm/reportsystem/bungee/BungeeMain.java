package com.minestorm.reportsystem.bungee;

import com.minestorm.reportsystem.common.db.DatabaseManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class BungeeMain extends Plugin {
    private Configuration config;
    private DatabaseManager database;

    @Override
    public void onEnable() {
        try {
            loadConfig();
            database = new DatabaseManager(getLogger());
            database.connect(
                config.getString("database.host", "127.0.0.1"),
                config.getInt("database.port", 3306),
                config.getString("database.name", "minestorm"),
                config.getString("database.username", "root"),
                config.getString("database.password", ""),
                config.getInt("database.pool-size", 10),
                config.getBoolean("database.use-ssl", false)
            );
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Database init failed; disabling.", e);
            return;
        }

        ProxyServer.getInstance().registerChannel("minestorm:rs");
        ProxyServer.getInstance().getPluginManager().registerListener(this,
            new BungeeMessaging(this, database));

        ProxyServer.getInstance().getPluginManager().registerCommand(this,
            new BungeeCommandReport(this, database));
        ProxyServer.getInstance().getPluginManager().registerCommand(this,
            new BungeeCommandMyTask(this, database));

        long cleanupMin = config.getInt("cleanup.interval-minutes", 30);
        ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            long now = System.currentTimeMillis();
            long day = 86400000L;
            long rCut = config.getInt("cleanup.report-expiry-days", 7) * day;
            long cCut = config.getInt("cleanup.chat-log-retention-days", 14) * day;
            database.cleanup(
                rCut > 0 ? now - rCut : 0,
                cCut > 0 ? now - cCut : 0
            ).whenComplete((n, err) -> {
                if (err == null && n > 0) getLogger().info("[MSRS] Cleanup removed " + n + " rows.");
            });
        }, 1, cleanupMin, TimeUnit.MINUTES);

        getLogger().info("MineStormReportSystem (Bungee) enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.shutdown();
        getLogger().info("MineStormReportSystem (Bungee) disabled.");
    }

    private void loadConfig() throws Exception {
        File f = new File(getDataFolder(), "config.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        if (!f.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, f.toPath());
            }
        }
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(f);
    }

    public Configuration config() { return config; }
}

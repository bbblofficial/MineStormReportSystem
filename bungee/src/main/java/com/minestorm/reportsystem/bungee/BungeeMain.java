package com.minestorm.reportsystem.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public final class BungeeMain extends Plugin {

    private Configuration config;

    @Override
    public void onEnable() {
        try {
            loadConfig();
            getLogger().info("MineStormReportSystem (Bungee) enabled.");
        } catch (Throwable ex) {
            getLogger().log(Level.SEVERE, "Bungee enable failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("MineStormReportSystem (Bungee) disabled.");
    }

    private void loadConfig() {
        try {
            File folder = getDataFolder();
            if (!folder.exists() && !folder.mkdirs()) {
                getLogger().warning("Could not create data folder.");
                return;
            }
            File f = new File(folder, "config.yml");
            if (!f.exists()) {
                InputStream in = getResourceAsStream("config.yml");
                if (in == null) {
                    getLogger().warning("config.yml not in JAR — creating default.");
                    Files.write(f.toPath(), ("database:\n  host: 127.0.0.1\n  port: 3306\n"
                        + "  name: reports\n  username: root\n  password: \"\"\n").getBytes("UTF-8"));
                } else {
                    try {
                        Files.copy(in, f.toPath());
                    } finally {
                        try { in.close(); } catch (Exception ignored) {}
                    }
                }
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(f);
        } catch (Throwable ex) {
            getLogger().log(Level.WARNING, "Config load failed: " + ex.getMessage(), ex);
        }
    }

    public Configuration config() { return config; }
}

package com.minestorm.reportsystem.util;

import com.minestorm.reportsystem.ReportSystem;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.InputStream;

/**
 * چک می‌کند که همه‌ی منابع لازm موجود هستند و در صورت نبودن می‌سازد.
 *
 * این کلاس قبل از هر چیز در onEnable اجرا می‌شود.
 */
public final class StartupVerifier {

    private final ReportSystem plugin;

    public StartupVerifier(ReportSystem plugin) { this.plugin = plugin; }

    /**
     * مطمئن می‌شود:
     *  • config.yml در data folder موجود است (از JAR کپی می‌شود اگر نبود)
     *  • punishments.yml در data folder موجود است
     *  • داده‌های ضروری config مقدار دارند
     *  • database folder قابل نوشتن است
     */
    public void verifyAll() {
        ensureDataFolder();
        ensureResource("config.yml");
        ensureResource("punishments.yml");
        ensureConfigKeys();
    }

    private void ensureDataFolder() {
        File folder = plugin.getDataFolder();
        if (!folder.exists()) {
            if (folder.mkdirs()) plugin.getLogger().info("Created data folder: " + folder.getAbsolutePath());
            else plugin.getLogger().warning("Could not create data folder: " + folder.getAbsolutePath());
        }
    }

    /**
     * کپی فایل از JAR به data folder اگر نبود.
     * اگر داخل JAR هم نبود، هیچ کاری نمی‌کند و فقط لاگ می‌دهد.
     */
    private void ensureResource(String name) {
        File target = new File(plugin.getDataFolder(), name);
        if (target.exists()) return;

        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                plugin.getLogger().warning("Resource '" + name + "' not found in JAR — skipping auto-create.");
                return;
            }
            plugin.saveResource(name, false);
            plugin.getLogger().info("Auto-created missing resource: " + name);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not auto-create '" + name + "': " + ex.getMessage());
        }
    }

    /**
     * اگر کلیدهای ضروری config نبودند، مقدار پیش‌فرض می‌گذارد.
     */
    private void ensureConfigKeys() {
        FileConfiguration cfg = plugin.getConfig();
        boolean changed = false;

        changed |= setIfMissing(cfg, "database.type", "SQLITE");
        changed |= setIfMissing(cfg, "database.sqlite-file", "reports.db");
        changed |= setIfMissing(cfg, "database.mysql.host", "localhost");
        changed |= setIfMissing(cfg, "database.mysql.port", Integer.valueOf(3306));
        changed |= setIfMissing(cfg, "database.mysql.database", "reports");
        changed |= setIfMissing(cfg, "database.mysql.username", "root");
        changed |= setIfMissing(cfg, "database.mysql.password", "");
        changed |= setIfMissing(cfg, "database.mysql.use-ssl", Boolean.FALSE);

        changed |= setIfMissing(cfg, "report.cooldown-seconds", Long.valueOf(60L));
        changed |= setIfMissing(cfg, "report.allow-offline-targets", Boolean.TRUE);
        changed |= setIfMissing(cfg, "report.allow-duplicate-pending", Boolean.FALSE);

        changed |= setIfMissing(cfg, "chat-logging.enabled", Boolean.TRUE);
        changed |= setIfMissing(cfg, "chat-logging.flush-interval-seconds", Long.valueOf(5L));
        changed |= setIfMissing(cfg, "chat-logging.history-limit", Integer.valueOf(500));
        changed |= setIfMissing(cfg, "chat-logging.chat-preview-lines", Integer.valueOf(25));

        changed |= setIfMissing(cfg, "cleanup.interval-minutes", Integer.valueOf(30));
        changed |= setIfMissing(cfg, "cleanup.report-expiry-days", Integer.valueOf(7));
        changed |= setIfMissing(cfg, "cleanup.chat-log-retention-days", Integer.valueOf(14));
        changed |= setIfMissing(cfg, "cleanup.grim-alert-retention-days", Integer.valueOf(14));

        changed |= setIfMissing(cfg, "grim.enabled", Boolean.TRUE);
        changed |= setIfMissing(cfg, "grim.alert-history-limit", Integer.valueOf(100));

        changed |= setIfMissing(cfg, "discord.enabled", Boolean.FALSE);
        changed |= setIfMissing(cfg, "discord.notify-on-create", Boolean.TRUE);
        changed |= setIfMissing(cfg, "discord.notify-on-resolve", Boolean.TRUE);

        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Config has been auto-completed with missing keys.");
        }
    }

    private boolean setIfMissing(FileConfiguration cfg, String path, Object defaultValue) {
        if (cfg.contains(path)) return false;
        cfg.set(path, defaultValue);
        return true;
    }
}

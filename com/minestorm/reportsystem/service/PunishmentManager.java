package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportCategory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class PunishmentManager {

    public enum Type { BAN, MUTE }

    public static final class Punishment {
        private final Type type;
        private final String duration;
        private final String reason;

        public Punishment(Type type, String duration, String reason) {
            this.type = type; this.duration = duration; this.reason = reason;
        }
        public Type type() { return type; }
        public String duration() { return duration; }
        public String reason() { return reason; }

        public boolean isPermanent() {
            String d = duration == null ? "" : duration.trim();
            return d.isEmpty()
                    || d.equalsIgnoreCase("permanent")
                    || d.equalsIgnoreCase("perm");
        }

        public String describe() {
            return isPermanent() ? (type.name() + " (permanent)")
                                 : (type.name() + " " + duration);
        }
    }

    private final ReportSystem plugin;
    private final Map<ReportCategory, Punishment> punishments =
            new EnumMap<ReportCategory, Punishment>(ReportCategory.class);
    private FileConfiguration config;

    public PunishmentManager(ReportSystem plugin) { this.plugin = plugin; }

    public void load() {
        File file = new File(plugin.getDataFolder(), "punishments.yml");
        if (!file.exists()) {
            try { plugin.saveResource("punishments.yml", false); } catch (Throwable ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
        punishments.clear();

        for (ReportCategory category : ReportCategory.values()) {
            ConfigurationSection section = config.getConfigurationSection("categories." + category.name());
            if (section == null) {
                plugin.getLogger().warning("No punishment configured for " + category.name());
                continue;
            }
            Type type;
            try { type = Type.valueOf(section.getString("type", "BAN").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid punishment type for " + category.name() + ", using BAN.");
                type = Type.BAN;
            }
            punishments.put(category, new Punishment(type,
                    section.getString("duration", "permanent"),
                    section.getString("reason", category.getDisplayName() + " (Report #{id})")));
        }
    }

    public Punishment get(ReportCategory category) { return punishments.get(category); }

    public String execute(CommandSender admin, Report report, Punishment punishment) {
        String template = config.getString("commands." + punishment.type().name().toLowerCase(Locale.ROOT),
                punishment.type() == Type.BAN ? "ban {player} {duration} {reason}"
                                              : "mute {player} {duration} {reason}");
        String reason = punishment.reason()
                .replace("{id}", String.valueOf(report.id()))
                .replace("{player}", report.targetName())
                .replace("{admin}", admin.getName());
        String duration = punishment.isPermanent() ? "" : punishment.duration();
        String command = template
                .replace("{player}", report.targetName())
                .replace("{duration}", duration)
                .replace("{admin}", admin.getName())
                .replace("{reason}", reason)
                .trim().replaceAll("\\s{2,}", " ");
        if (command.startsWith("/")) command = command.substring(1);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        return command;
    }
}

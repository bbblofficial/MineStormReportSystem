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
        private final Type type; private final String duration; private final String reason;
        public Punishment(Type t, String d, String r) { type = t; duration = d; reason = r; }
        public Type type() { return type; }
        public String duration() { return duration; }
        public String reason() { return reason; }
        public boolean isPermanent() {
            String d = duration == null ? "" : duration.trim();
            return d.isEmpty() || d.equalsIgnoreCase("permanent") || d.equalsIgnoreCase("perm");
        }
        public String describe() {
            return isPermanent() ? (type.name() + " (perm)") : (type.name() + " " + duration);
        }
    }

    private final ReportSystem plugin;
    private final Map<ReportCategory, Punishment> map = new EnumMap<ReportCategory, Punishment>(ReportCategory.class);
    private FileConfiguration config;

    public PunishmentManager(ReportSystem p) { plugin = p; }

    public void load() {
        File f = new File(plugin.getDataFolder(), "punishments.yml");
        if (!f.exists()) {
            try { plugin.saveResource("punishments.yml", false); } catch (Throwable ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(f);
        map.clear();
        for (ReportCategory c : ReportCategory.values()) {
            ConfigurationSection s = config.getConfigurationSection("categories." + c.name());
            if (s == null) { plugin.getLogger().warning("No punishment for " + c.name()); continue; }
            Type t;
            try { t = Type.valueOf(s.getString("type", "BAN").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { t = Type.BAN; }
            map.put(c, new Punishment(t, s.getString("duration", "permanent"),
                s.getString("reason", c.getDisplayName() + " (#{id})")));
        }
    }

    public Punishment get(ReportCategory c) { return map.get(c); }

    public String execute(CommandSender admin, Report r, Punishment p) {
        String tpl = config.getString("commands." + p.type().name().toLowerCase(Locale.ROOT),
            p.type() == Type.BAN ? "ban {player} {duration} {reason}" : "mute {player} {duration} {reason}");
        String reason = p.reason().replace("{id}", String.valueOf(r.id()))
            .replace("{player}", r.targetName()).replace("{admin}", admin.getName());
        String dur = p.isPermanent() ? "" : p.duration();
        String cmd = tpl.replace("{player}", r.targetName())
            .replace("{duration}", dur).replace("{admin}", admin.getName())
            .replace("{reason}", reason).trim().replaceAll("\\s{2,}", " ");
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        return cmd;
    }
}

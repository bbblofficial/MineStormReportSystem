package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportOutcome;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class WebhookService {
    private static final int COLOR_NEW = 15844367;
    private static final int COLOR_APPROVED = 3066993;
    private static final int COLOR_REJECTED = 15158332;
    private static final int COLOR_INVALID = 15105570;
    private final ReportSystem plugin;

    public WebhookService(ReportSystem plugin) { this.plugin = plugin; }

    public void reportCreated(Report report) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("discord.notify-on-create", true)) return;
        StringBuilder fields = new StringBuilder();
        field(fields, "Report ID", "#" + report.id(), true);
        field(fields, "Category", report.category().getDisplayName(), true);
        field(fields, "Reported Player", report.targetName(), true);
        field(fields, "Reported By", report.reporterName(), true);
        send("New Report Submitted", COLOR_NEW, report, fields);
    }

    public void reportResolved(Report report, String adminName,
                               ReportOutcome outcome, String punishmentCommand) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("discord.notify-on-resolve", true)) return;
        StringBuilder fields = new StringBuilder();
        field(fields, "Report ID", "#" + report.id(), true);
        field(fields, "Category", report.category().getDisplayName(), true);
        field(fields, "Outcome", outcome.getLabel(), true);
        field(fields, "Reported Player", report.targetName(), true);
        field(fields, "Reported By", report.reporterName(), true);
        field(fields, "Handled By", adminName, true);
        if (punishmentCommand != null)
            field(fields, "Punishment", "`/" + punishmentCommand + "`", false);

        String title; int color;
        switch (outcome) {
            case APPROVED: title = "Report Approved"; color = COLOR_APPROVED; break;
            case REJECTED: title = "Report Rejected"; color = COLOR_REJECTED; break;
            default: title = "Report Closed (Invalid Category)"; color = COLOR_INVALID;
        }
        send(title, color, report, fields);
    }

    private void send(String title, int color, Report report, StringBuilder fields) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("discord.enabled", false)) return;
        final String url = cfg.getString("discord.webhook-url", "");
        if (url == null || url.trim().isEmpty()) return;

        StringBuilder json = new StringBuilder("{");
        json.append("\"username\":").append(quote(cfg.getString("discord.username", "Report System"))).append(',');
        String avatar = cfg.getString("discord.avatar-url", "");
        if (avatar != null && !avatar.trim().isEmpty())
            json.append("\"avatar_url\":").append(quote(avatar)).append(',');
        json.append("\"embeds\":[{")
                .append("\"title\":").append(quote(title)).append(',')
                .append("\"color\":").append(color).append(',')
                .append("\"thumbnail\":{\"url\":")
                .append(quote("https://minotar.net/helm/" + report.targetName() + "/100.png")).append("},")
                .append("\"fields\":[").append(fields).append("],")
                .append("\"footer\":{\"text\":\"MineStorm Reports\"},")
                .append("\"timestamp\":").append(quote(nowIso()))
                .append("}]}");

        final String payload = json.toString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() { post(url, payload); }
        });
    }

    private void post(String url, String payload) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "MineStormReportSystem/1.0");
            byte[] data = payload.getBytes(Charset.forName("UTF-8"));
            conn.setFixedLengthStreamingMode(data.length);
            OutputStream out = conn.getOutputStream();
            try { out.write(data); } finally { if (out != null) out.close(); }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300)
                plugin.getLogger().warning("Discord webhook HTTP " + code);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not send Discord webhook: " + ex.getMessage());
        } finally { if (conn != null) conn.disconnect(); }
    }

    private static void field(StringBuilder sb, String name, String value, boolean inline) {
        if (sb.length() > 0) sb.append(',');
        sb.append("{\"name\":").append(quote(name))
          .append(",\"value\":").append(quote(value))
          .append(",\"inline\":").append(inline).append('}');
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') sb.append(String.format("\\u%04x", Integer.valueOf(c)));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String nowIso() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(new Date());
    }
}

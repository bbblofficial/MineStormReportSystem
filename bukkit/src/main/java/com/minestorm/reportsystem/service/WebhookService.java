package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportOutcome;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class WebhookService {
    private static final int C_NEW = 15844367;
    private static final int C_OK = 3066993;
    private static final int C_REJ = 15158332;
    private static final int C_INV = 15105570;
    private final ReportSystem plugin;

    public WebhookService(ReportSystem p) { plugin = p; }

    public void reportCreated(Report r) {
        if (!plugin.getConfig().getBoolean("discord.notify-on-create", true)) return;
        StringBuilder f = new StringBuilder();
        field(f, "ID", "#" + r.id(), true);
        field(f, "Category", r.category().getDisplayName(), true);
        field(f, "Target", r.targetName(), true);
        field(f, "Reporter", r.reporterName(), true);
        send("New Report", C_NEW, r, f);
    }

    public void reportResolved(Report r, String admin, ReportOutcome o, String cmd) {
        if (!plugin.getConfig().getBoolean("discord.notify-on-resolve", true)) return;
        StringBuilder f = new StringBuilder();
        field(f, "ID", "#" + r.id(), true);
        field(f, "Outcome", o.getLabel(), true);
        field(f, "Target", r.targetName(), true);
        field(f, "By", admin, true);
        if (cmd != null) field(f, "Cmd", "`/" + cmd + "`", false);
        String t; int c;
        switch (o) {
            case APPROVED: t = "Approved"; c = C_OK; break;
            case REJECTED: t = "Rejected"; c = C_REJ; break;
            default: t = "Invalid"; c = C_INV;
        }
        send(t, c, r, f);
    }

    private void send(String title, int color, Report r, StringBuilder f) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("discord.enabled", false)) return;
        final String url = cfg.getString("discord.webhook-url", "");
        if (url == null || url.trim().isEmpty()) return;

        StringBuilder j = new StringBuilder("{");
        j.append("\"username\":").append(q(cfg.getString("discord.username", "Report System"))).append(',');
        String av = cfg.getString("discord.avatar-url", "");
        if (av != null && !av.trim().isEmpty())
            j.append("\"avatar_url\":").append(q(av)).append(',');
        j.append("\"embeds\":[{\"title\":").append(q(title))
         .append(",\"color\":").append(color)
         .append(",\"thumbnail\":{\"url\":")
         .append(q("https://minotar.net/helm/" + r.targetName() + "/100.png")).append("}")
         .append(",\"fields\":[").append(f).append("]")
         .append(",\"footer\":{\"text\":\"MineStorm\"}")
         .append(",\"timestamp\":").append(q(nowIso())).append("}]}");

        final String body = j.toString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() { post(url, body); }
        });
    }

    private void post(String url, String body) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(5000); c.setReadTimeout(5000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] d = body.getBytes(Charset.forName("UTF-8"));
            c.setFixedLengthStreamingMode(d.length);
            OutputStream o = c.getOutputStream();
            try { o.write(d); } finally { if (o != null) o.close(); }
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) plugin.getLogger().warning("Webhook HTTP " + code);
        } catch (Exception e) {
            plugin.getLogger().warning("Webhook error: " + e.getMessage());
        } finally { if (c != null) c.disconnect(); }
    }

    private static void field(StringBuilder sb, String n, String v, boolean inline) {
        if (sb.length() > 0) sb.append(',');
        sb.append("{\"name\":").append(q(n))
          .append(",\"value\":").append(q(v))
          .append(",\"inline\":").append(inline).append('}');
    }

    private static String q(String v) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : v.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String nowIso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}

package com.minestorm.reportsystem.gui;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.ChatChannel;
import com.minestorm.reportsystem.model.LogEntry;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.util.ItemBuilder;
import com.minestorm.reportsystem.util.TextUtil;
import com.minestorm.reportsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class EvidenceGui extends GuiHolder {

    public enum Source {
        CHAT_PUBLIC("Public Chat", ChatChannel.PUBLIC, false),
        CHAT_PRIVATE("Direct Messages", ChatChannel.PRIVATE, false),
        CHAT_GUILD("Guild Chat", ChatChannel.GUILD, false),
        CHAT_ALL("All Chat", null, false),
        GRIM("Grim Alerts", null, true);
        public final String label;
        public final ChatChannel channel;
        public final boolean grim;
        Source(String l, ChatChannel c, boolean g) { label = l; channel = c; grim = g; }
    }

    private final ReportSystem plugin;
    private final Report report;
    private final Source source;
    private final List<LogEntry> entries;
    private final int returnPage;
    private final int pages;
    private int page;

    private EvidenceGui(ReportSystem p, Report r, Source s, List<LogEntry> e, int rp) {
        plugin = p; report = r; source = s; entries = e; returnPage = rp;
        pages = Math.max(1, (e.size() + 44) / 45);
    }

    private static CompletableFuture<List<LogEntry>> fetch(ReportSystem p, Report r, Source s) {
        if (s.grim) {
            int l = p.getConfig().getInt("grim.alert-history-limit", 100);
            return p.database().getGrimAlerts(r.targetUuid(), l);
        }
        int l = p.getConfig().getInt("chat-logging.history-limit", 500);
        p.chatLogger().flush();
        return p.database().getChatLogs(r.targetUuid(), r.targetName(), s.channel, l);
    }

    public static void open(final ReportSystem plugin, final Player admin,
                            final Report report, final Source src, final int rp) {
        warnGrim(plugin, admin, src);
        plugin.callback(fetch(plugin, report, src),
            new Consumer<List<LogEntry>>() {
                public void accept(List<LogEntry> e) {
                    if (admin.isOnline()) new EvidenceGui(plugin, report, src, e, rp).show(admin);
                }
            },
            new Runnable() { public void run() { plugin.messages().send(admin, "database-error"); } });
    }

    public static void print(final ReportSystem plugin, final Player admin,
                             final Report report, final Source src) {
        warnGrim(plugin, admin, src);
        plugin.callback(fetch(plugin, report, src),
            new Consumer<List<LogEntry>>() {
                public void accept(List<LogEntry> entries) {
                    if (!admin.isOnline()) return;
                    if (entries.isEmpty()) {
                        plugin.messages().send(admin, "evidence-empty",
                            "{player}", report.targetName());
                        return;
                    }
                    int c = Math.min(entries.size(),
                        plugin.getConfig().getInt("chat-logging.chat-preview-lines", 25));
                    List<LogEntry> latest = new ArrayList<LogEntry>(entries.subList(0, c));
                    Collections.reverse(latest);
                    plugin.messages().send(admin, "evidence-header",
                        "{count}", Integer.valueOf(c),
                        "{player}", report.targetName(),
                        "{source}", src.label);
                    for (LogEntry e : latest) {
                        admin.sendMessage(plugin.messages().raw("evidence-line",
                            "{time}", TimeUtil.format(e.timestamp()),
                            "{title}", TextUtil.clean(e.title()),
                            "{text}", TextUtil.clean(e.text())));
                    }
                }
            },
            new Runnable() { public void run() { plugin.messages().send(admin, "database-error"); } });
    }

    private static void warnGrim(ReportSystem p, Player a, Source s) {
        if (s.grim && !p.grim().isActive()) p.messages().send(a, "grim-unavailable");
    }

    private void show(Player admin) {
        inventory = Bukkit.createInventory(this, 54,
            plugin.messages().title("evidence",
                "{source}", source.label,
                "{player}", report.targetName()));
        render();
        admin.openInventory(inventory);
    }

    private void render() {
        inventory.clear();
        int start = page * 45;
        for (int i = 0; i < 45 && start + i < entries.size(); i++)
            inventory.setItem(i, item(entries.get(start + i)));
        ItemStack f = ItemBuilder.filler();
        for (int j = 45; j < 54; j++) inventory.setItem(j, f);
        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("&c&lNo entries").lore("&7Nothing recorded.").build());
        }
        if (page > 0) inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&aPrev").build());
        if (page < pages - 1) inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&aNext").build());
        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&aBack")
            .lore("&7Page: &f" + (page + 1) + "/" + pages,
                  "&7Entries: &f" + entries.size()).build());
    }

    private ItemStack item(LogEntry e) {
        List<String> lore = new ArrayList<String>();
        lore.add("\u00a78" + TimeUtil.format(e.timestamp()));
        for (String line : TextUtil.wrap(TextUtil.clean(e.text()), 40, 8))
            lore.add("\u00a7f" + line);
        return new ItemBuilder(Material.PAPER)
            .name("&e" + TextUtil.clean(e.title()))
            .lore(lore).build();
    }

    public void onClick(InventoryClickEvent e, Player admin) {
        int s = e.getRawSlot();
        if (s == 45 && page > 0) { page--; render(); }
        else if (s == 53 && page < pages - 1) { page++; render(); }
        else if (s == 49) DetailsGui.open(plugin, admin, report, returnPage);
    }
}

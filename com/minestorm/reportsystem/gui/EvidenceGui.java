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

public final class EvidenceGui extends GuiHolder {

    public enum Source {
        CHAT_PUBLIC("Public Chat", ChatChannel.PUBLIC, false),
        CHAT_PRIVATE("Direct Messages", ChatChannel.PRIVATE, false),
        CHAT_GUILD("Guild Chat", ChatChannel.GUILD, false),
        CHAT_ALL("All Chat", null, false),
        GRIM("Grim Alerts", null, true);

        private final String label;
        private final ChatChannel channel;
        private final boolean grim;

        Source(String label, ChatChannel channel, boolean grim) {
            this.label = label; this.channel = channel; this.grim = grim;
        }
    }

    private final ReportSystem plugin;
    private final Report report;
    private final Source source;
    private final List<LogEntry> entries;
    private final int returnPage;
    private final int pages;
    private int page;

    private EvidenceGui(ReportSystem plugin, Report report, Source source,
                        List<LogEntry> entries, int returnPage) {
        this.plugin = plugin; this.report = report; this.source = source;
        this.entries = entries; this.returnPage = returnPage;
        this.pages = Math.max(1, (entries.size() + 45 - 1) / 45);
    }

    private static CompletableFuture<List<LogEntry>> fetch(ReportSystem plugin, Report report, Source source) {
        if (source.grim) {
            int limit = plugin.getConfig().getInt("grim.alert-history-limit", 100);
            return plugin.database().getGrimAlerts(report.targetUuid(), limit);
        }
        int limit = plugin.getConfig().getInt("chat-logging.history-limit", 500);
        plugin.chatLogger().flush();
        return plugin.database().getChatLogs(report.targetUuid(), report.targetName(),
                source.channel, limit);
    }

    public static void open(final ReportSystem plugin, final Player admin, final Report report,
                            final Source source, final int returnPage) {
        warnIfGrimMissing(plugin, admin, source);
        plugin.callback(fetch(plugin, report, source),
            new java.util.function.Consumer<List<LogEntry>>() {
                public void accept(List<LogEntry> entries) {
                    if (admin.isOnline())
                        new EvidenceGui(plugin, report, source, entries, returnPage).show(admin);
                }
            },
            new Runnable() {
                public void run() { plugin.messages().send(admin, "database-error"); }
            });
    }

    public static void print(final ReportSystem plugin, final Player admin,
                             final Report report, final Source source) {
        warnIfGrimMissing(plugin, admin, source);
        plugin.callback(fetch(plugin, report, source),
            new java.util.function.Consumer<List<LogEntry>>() {
                public void accept(List<LogEntry> entries) {
                    if (!admin.isOnline()) return;
                    if (entries.isEmpty()) {
                        plugin.messages().send(admin, "evidence-empty",
                                "{player}", report.targetName());
                        return;
                    }
                    int count = Math.min(entries.size(),
                            plugin.getConfig().getInt("chat-logging.chat-preview-lines", 25));
                    List<LogEntry> latest = new ArrayList<LogEntry>(entries.subList(0, count));
                    Collections.reverse(latest);
                    plugin.messages().send(admin, "evidence-header",
                            "{count}", Integer.valueOf(count),
                            "{player}", report.targetName(),
                            "{source}", source.label);
                    for (LogEntry entry : latest) {
                        admin.sendMessage(plugin.messages().raw("evidence-line",
                                "{time}", TimeUtil.format(entry.timestamp()),
                                "{title}", TextUtil.clean(entry.title()),
                                "{text}", TextUtil.clean(entry.text())));
                    }
                }
            },
            new Runnable() {
                public void run() { plugin.messages().send(admin, "database-error"); }
            });
    }

    private static void warnIfGrimMissing(ReportSystem plugin, Player admin, Source source) {
        if (source.grim && !plugin.grim().isActive())
            plugin.messages().send(admin, "grim-unavailable");
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
            inventory.setItem(i, createEntryItem(entries.get(start + i)));

        ItemStack filler = ItemBuilder.filler();
        for (int j = 45; j < 54; j++) inventory.setItem(j, filler);

        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("&c&lNo entries found")
                    .lore("&7Nothing was recorded for &f" + report.targetName() + "&7.")
                    .build());
        }
        if (page > 0) inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&aPrevious Page").build());
        if (page < pages - 1) inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&aNext Page").build());

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name("&aBack to Report")
                .lore("&7Page: &f" + (page + 1) + "/" + pages,
                      "&7Entries: &f" + entries.size()).build());
    }

    private ItemStack createEntryItem(LogEntry entry) {
        List<String> lore = new ArrayList<String>();
        lore.add("\u00a78" + TimeUtil.format(entry.timestamp()));
        for (String line : TextUtil.wrap(TextUtil.clean(entry.text()), 40, 8))
            lore.add("\u00a7f" + line);
        return new ItemBuilder(Material.PAPER)
                .name("&e" + TextUtil.clean(entry.title()))
                .loreRaw(lore)
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event, Player admin) {
        int slot = event.getRawSlot();
        if (slot == 45 && page > 0) { page--; render(); }
        else if (slot == 53 && page < pages - 1) { page++; render(); }
        else if (slot == 49) DetailsGui.open(plugin, admin, report, returnPage);
    }
}

package com.minestorm.reportsystem.gui;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.util.ItemBuilder;
import com.minestorm.reportsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AdminListGui extends GuiHolder {
    private final ReportSystem plugin;
    private final List<Report> reports;
    private final int pages;
    private final int page;
    private final Map<Integer, Report> slots = new HashMap<Integer, Report>();

    private AdminListGui(ReportSystem plugin, List<Report> reports, int requestedPage) {
        this.plugin = plugin;
        this.reports = reports;
        this.pages = Math.max(1, (reports.size() + 45 - 1) / 45);
        this.page = Math.min(Math.max(0, requestedPage), this.pages - 1);
    }

    public static void open(final ReportSystem plugin, final Player admin, final int page) {
        plugin.callback(plugin.database().getReports(),
            new java.util.function.Consumer<List<Report>>() {
                public void accept(List<Report> reports) {
                    if (admin.isOnline())
                        new AdminListGui(plugin, reports, page).show(admin);
                }
            },
            new Runnable() {
                public void run() { plugin.messages().send(admin, "database-error"); }
            });
    }

    private void show(Player admin) {
        inventory = Bukkit.createInventory(this, 54,
                plugin.messages().title("admin",
                        "{page}", Integer.valueOf(page + 1),
                        "{pages}", Integer.valueOf(pages)));
        int start = page * 45;
        for (int i = 0; i < 45 && start + i < reports.size(); i++) {
            Report r = reports.get(start + i);
            slots.put(Integer.valueOf(i), r);
            inventory.setItem(i, createReportItem(r));
        }
        ItemStack filler = ItemBuilder.filler();
        for (int j = 45; j < 54; j++) inventory.setItem(j, filler);
        if (page > 0) inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&aPrevious Page").build());
        if (page < pages - 1) inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&aNext Page").build());

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
                .name("&e&lActive Reports")
                .lore("&7Total: &f" + reports.size(), "&7Page: &f" + (page + 1) + "/" + pages)
                .build());

        if (reports.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.EMERALD)
                    .name("&a&lNo active reports")
                    .lore("&7Everything is under control.").build());
        }
        admin.openInventory(inventory);
    }

    private ItemStack createReportItem(Report report) {
        boolean online = Bukkit.getPlayer(report.targetUuid()) != null;
        return new ItemBuilder(report.category().getIcon())
                .name("&e&l" + report.category().getDisplayName() + " &8#" + report.id())
                .lore("&7Reported: &f" + report.targetName() + (online ? " &a(online)" : " &c(offline)"),
                      "&7Reporter: &f" + report.reporterName(),
                      "&7Submitted: &f" + TimeUtil.ago(report.createdAt()),
                      "", "&aClick to review")
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot == 45 && page > 0) open(plugin, player, page - 1);
        else if (slot == 53 && page < pages - 1) open(plugin, player, page + 1);
        else {
            Report r = slots.get(Integer.valueOf(slot));
            if (r != null) DetailsGui.open(plugin, player, r, page);
        }
    }
}

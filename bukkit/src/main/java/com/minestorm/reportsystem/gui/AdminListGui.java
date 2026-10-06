package com.minestorm.reportsystem.gui;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.util.ItemBuilder;
import com.minestorm.reportsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class AdminListGui extends GuiHolder {
    private final ReportSystem plugin;
    private final List<Report> reports;
    private final int pages;
    private final int page;
    private final Map<Integer, Report> slots = new HashMap<Integer, Report>();

    private AdminListGui(ReportSystem p, List<Report> r, int req) {
        plugin = p; reports = r;
        pages = Math.max(1, (r.size() + 44) / 45);
        page = Math.min(Math.max(0, req), pages - 1);
    }

    public static void open(final ReportSystem plugin, final Player admin, final int page) {
        plugin.callback(plugin.database().getReports(),
            new Consumer<List<Report>>() {
                public void accept(List<Report> r) {
                    if (admin.isOnline()) new AdminListGui(plugin, r, page).show(admin);
                }
            },
            new Runnable() { public void run() { plugin.messages().send(admin, "database-error"); } });
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
            inventory.setItem(i, item(r));
        }
        ItemStack f = ItemBuilder.filler();
        for (int j = 45; j < 54; j++) inventory.setItem(j, f);
        if (page > 0) inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&aPrevious").build());
        if (page < pages - 1) inventory.setItem(53, new ItemBuilder(Material.ARROW).name("&aNext").build());
        inventory.setItem(49, new ItemBuilder(Material.PAPER).name("&e&lActive Reports")
            .lore("&7Total: &f" + reports.size(), "&7Page: &f" + (page + 1) + "/" + pages).build());
        if (reports.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.EMERALD).name("&a&lNo reports")
                .lore("&7All clear.").build());
        }
        admin.openInventory(inventory);
    }

    private ItemStack item(Report r) {
        boolean online = Bukkit.getPlayer(r.targetUuid()) != null;
        return new ItemBuilder(r.category().getIcon())
            .name("&e&l" + r.category().getDisplayName() + " &8#" + r.id())
            .lore("&7Reported: &f" + r.targetName() + (online ? " &a(online)" : " &c(offline)"),
                  "&7Reporter: &f" + r.reporterName(),
                  "&7Submitted: &f" + TimeUtil.ago(r.createdAt()),
                  "", "&aClick to review").build();
    }

    public void onClick(InventoryClickEvent e, Player p) {
        int s = e.getRawSlot();
        if (s == 45 && page > 0) open(plugin, p, page - 1);
        else if (s == 53 && page < pages - 1) open(plugin, p, page + 1);
        else {
            Report r = slots.get(Integer.valueOf(s));
            if (r != null) DetailsGui.open(plugin, p, r, page);
        }
    }
}

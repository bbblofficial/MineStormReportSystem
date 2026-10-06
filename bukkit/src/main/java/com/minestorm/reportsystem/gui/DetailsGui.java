package com.minestorm.reportsystem.gui;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.Report;
import com.minestorm.reportsystem.model.ReportCategory;
import com.minestorm.reportsystem.model.ReportOutcome;
import com.minestorm.reportsystem.service.PunishmentManager;
import com.minestorm.reportsystem.util.ItemBuilder;
import com.minestorm.reportsystem.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class DetailsGui extends GuiHolder {
    private static final int[] CHAT_SLOTS = { 11, 12, 13, 14 };
    private static final EvidenceGui.Source[] CHAT_SRC = {
        EvidenceGui.Source.CHAT_PUBLIC, EvidenceGui.Source.CHAT_PRIVATE,
        EvidenceGui.Source.CHAT_GUILD, EvidenceGui.Source.CHAT_ALL };

    private final ReportSystem plugin;
    private final Report report;
    private final int returnPage;
    private boolean busy;

    private DetailsGui(ReportSystem p, Report r, int rp) { plugin = p; report = r; returnPage = rp; }

    public static void open(ReportSystem p, Player a, Report r, int rp) {
        new DetailsGui(p, r, rp).show(a);
    }

    private void show(Player admin) {
        inventory = Bukkit.createInventory(this, 54,
            plugin.messages().title("details",
                "{id}", Integer.valueOf(report.id()),
                "{player}", report.targetName()));
        ItemStack f = ItemBuilder.filler();
        for (int i = 0; i < 54; i++) inventory.setItem(i, f);

        ReportCategory cat = report.category();
        inventory.setItem(4, new ItemBuilder(cat.getIcon())
            .name("&e&l" + cat.getDisplayName() + " &8#" + report.id())
            .lore("&7Reported: &f" + report.targetName(),
                  "&7Reporter: &f" + report.reporterName(),
                  "&7Submitted: &f" + TimeUtil.ago(report.createdAt())).build());

        inventory.setItem(9, new ItemBuilder(Material.SIGN)
            .name("&6&lEvidence").lore("&7Review before deciding.").build());
        buildEvidence(cat);

        inventory.setItem(27, new ItemBuilder(Material.SIGN)
            .name("&6&lActions").lore("&7How to resolve.").build());

        PunishmentManager.Punishment p = plugin.punishments().get(cat);
        inventory.setItem(28, new ItemBuilder(Material.EMERALD_BLOCK)
            .name("&a&lApprove")
            .lore("&7Report is correct.",
                  p == null ? "&cNo punishment configured!" : ("&7Punishment: &f" + p.describe()),
                  "", "&aClick").build());
        inventory.setItem(30, new ItemBuilder(Material.REDSTONE_BLOCK)
            .name("&c&lReject").lore("&7False report.", "", "&cClick").build());
        inventory.setItem(32, new ItemBuilder(Material.GOLD_BLOCK)
            .name("&6&lInvalid Category").lore("&7Wrong rule.", "", "&6Click").build());
        inventory.setItem(34, status());
        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&aBack").build());
        admin.openInventory(inventory);
    }

    private void buildEvidence(ReportCategory cat) {
        String t = report.targetName();
        switch (cat) {
            case CHAT_ABUSE:
                inventory.setItem(CHAT_SLOTS[0], book(Material.BOOK, "Public Chat",
                    "All public chat by &f" + t));
                inventory.setItem(CHAT_SLOTS[1], book(Material.BOOK_AND_QUILL, "DMs",
                    "Private messages of &f" + t));
                inventory.setItem(CHAT_SLOTS[2], book(Material.WRITTEN_BOOK, "Guild",
                    "Guild chat of &f" + t));
                inventory.setItem(CHAT_SLOTS[3], book(Material.ENCHANTED_BOOK, "All Chat",
                    "Everything combined"));
                return;
            case CHEATING:
                inventory.setItem(13, new ItemBuilder(Material.REDSTONE)
                    .name("&c&lGrim Alerts")
                    .lore("&7Recent flags for &f" + t, "",
                          "&aLeft: &7open", "&aRight: &7print").build());
                return;
            default:
                inventory.setItem(13, new ItemBuilder(Material.PAPER)
                    .name("&e&lManual Review")
                    .lore("&7No auto evidence.",
                          "&7Verify manually.").build());
        }
    }

    private ItemStack book(Material m, String n, String d) {
        return new ItemBuilder(m).name("&e&l" + n)
            .lore("&7" + d, "", "&aLeft: &7open", "&aRight: &7print").build();
    }

    private ItemStack status() {
        Player online = Bukkit.getPlayer(report.targetUuid());
        if (online != null) {
            return new ItemBuilder(Material.INK_SACK, 10).name("&a&lONLINE")
                .lore("&f" + report.targetName() + " &7is online.", "", "&aClick teleport").build();
        }
        long last = Bukkit.getOfflinePlayer(report.targetUuid()).getLastPlayed();
        return new ItemBuilder(Material.INK_SACK, 8).name("&c&lOFFLINE")
            .lore("&f" + report.targetName() + " &7is offline.",
                  last > 0 ? "&7Last seen: &f" + TimeUtil.ago(last) : "&7Last: unknown").build();
    }

    public void onClick(InventoryClickEvent e, Player admin) {
        if (busy) return;
        int s = e.getRawSlot();
        ClickType c = e.getClick();
        boolean print = c == ClickType.RIGHT || c == ClickType.SHIFT_RIGHT;
        switch (s) {
            case 49: AdminListGui.open(plugin, admin, returnPage); return;
            case 28: resolve(admin, ReportOutcome.APPROVED); return;
            case 30: resolve(admin, ReportOutcome.REJECTED); return;
            case 32: resolve(admin, ReportOutcome.INVALID_CATEGORY); return;
            case 34: tp(admin); return;
        }
        evidence(admin, s, print);
    }

    private void evidence(Player admin, int slot, boolean print) {
        EvidenceGui.Source src = null;
        if (report.category() == ReportCategory.CHAT_ABUSE) {
            for (int i = 0; i < CHAT_SLOTS.length; i++)
                if (CHAT_SLOTS[i] == slot) src = CHAT_SRC[i];
        } else if (report.category() == ReportCategory.CHEATING && slot == 13) {
            src = EvidenceGui.Source.GRIM;
        }
        if (src == null) return;
        if (print) EvidenceGui.print(plugin, admin, report, src);
        else EvidenceGui.open(plugin, admin, report, src, returnPage);
    }

    private void resolve(final Player a, ReportOutcome o) {
        busy = true;
        plugin.reports().resolve(a, report, o, new Runnable() {
            public void run() { AdminListGui.open(plugin, a, returnPage); }
        });
    }

    private void tp(Player admin) {
        Player t = Bukkit.getPlayer(report.targetUuid());
        if (t == null) { plugin.messages().send(admin, "player-offline"); return; }
        admin.closeInventory();
        admin.teleport((Entity) t);
        plugin.messages().send(admin, "teleported", "{player}", t.getName());
    }
}

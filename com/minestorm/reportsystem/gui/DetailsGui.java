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
    private static final int[] CHAT_SLOTS = new int[] { 11, 12, 13, 14 };
    private static final EvidenceGui.Source[] CHAT_SOURCES = new EvidenceGui.Source[] {
            EvidenceGui.Source.CHAT_PUBLIC, EvidenceGui.Source.CHAT_PRIVATE,
            EvidenceGui.Source.CHAT_GUILD, EvidenceGui.Source.CHAT_ALL };

    private final ReportSystem plugin;
    private final Report report;
    private final int returnPage;
    private boolean busy;

    private DetailsGui(ReportSystem plugin, Report report, int returnPage) {
        this.plugin = plugin; this.report = report; this.returnPage = returnPage;
    }

    public static void open(ReportSystem plugin, Player admin, Report report, int returnPage) {
        new DetailsGui(plugin, report, returnPage).show(admin);
    }

    private void show(Player admin) {
        inventory = Bukkit.createInventory(this, 54,
                plugin.messages().title("details",
                        "{id}", Integer.valueOf(report.id()),
                        "{player}", report.targetName()));
        ItemStack filler = ItemBuilder.filler();
        for (int i = 0; i < 54; i++) inventory.setItem(i, filler);

        ReportCategory category = report.category();
        inventory.setItem(4, new ItemBuilder(category.getIcon())
                .name("&e&l" + category.getDisplayName() + " &8#" + report.id())
                .lore("&7Reported: &f" + report.targetName(),
                      "&7Reporter: &f" + report.reporterName(),
                      "&7Submitted: &f" + TimeUtil.ago(report.createdAt()))
                .build());

        inventory.setItem(9, new ItemBuilder(Material.SIGN)
                .name("&6&lEvidence & Context")
                .lore("&7Review the proof before deciding.").build());
        buildEvidence(category);

        inventory.setItem(27, new ItemBuilder(Material.SIGN)
                .name("&6&lManagement Actions")
                .lore("&7Choose how to resolve this report.").build());

        PunishmentManager.Punishment punishment = plugin.punishments().get(category);
        inventory.setItem(28, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("&a&lApprove Report (Valid)")
                .lore("&7The report is correct.",
                      punishment == null ? "&cNo punishment configured!"
                                         : ("&7Punishment: &f" + punishment.describe()),
                      "&7The report will be deleted.",
                      "", "&aClick to punish")
                .build());
        inventory.setItem(30, new ItemBuilder(Material.REDSTONE_BLOCK)
                .name("&c&lReject Report (Invalid/False)")
                .lore("&7The report is false.",
                      "&7The report will be deleted.",
                      "", "&cClick to reject").build());
        inventory.setItem(32, new ItemBuilder(Material.GOLD_BLOCK)
                .name("&6&lInvalid Category")
                .lore("&7The reporter chose the wrong rule.",
                      "&7The report will be deleted.",
                      "", "&6Click to close as invalid category").build());
        inventory.setItem(34, createStatusItem());
        inventory.setItem(49, new ItemBuilder(Material.ARROW).name("&aBack to Reports").build());

        admin.openInventory(inventory);
    }

    private void buildEvidence(ReportCategory category) {
        String target = report.targetName();
        switch (category) {
            case CHAT_ABUSE:
                inventory.setItem(CHAT_SLOTS[0], evidenceBook(Material.BOOK, "Public Chat",
                        "All public chat messages sent by &f" + target + "&7."));
                inventory.setItem(CHAT_SLOTS[1], evidenceBook(Material.BOOK_AND_QUILL, "Direct Messages",
                        "Private messages sent and received by &f" + target + "&7."));
                inventory.setItem(CHAT_SLOTS[2], evidenceBook(Material.WRITTEN_BOOK, "Guild Chat",
                        "All guild chat messages sent by &f" + target + "&7."));
                inventory.setItem(CHAT_SLOTS[3], evidenceBook(Material.ENCHANTED_BOOK, "Full Chat History",
                        "Every channel combined for &f" + target + "&7."));
                return;
            case CHEATING:
                inventory.setItem(13, new ItemBuilder(Material.REDSTONE)
                        .name("&c&lGrim Anticheat Alerts")
                        .lore("&7Recent GrimAC flags for &f" + target + "&7.",
                              "", "&aLeft-click: &7open alert list",
                              "&aRight-click: &7print in chat").build());
                return;
            default:
                inventory.setItem(13, new ItemBuilder(Material.PAPER)
                        .name("&e&lManual Review")
                        .lore("&7No automatic evidence for this category.",
                              "&7Verify manually (e.g. inspect the",
                              "&7player, name, skin or guild) before",
                              "&7making a decision.").build());
        }
    }

    private ItemStack evidenceBook(Material material, String name, String description) {
        return new ItemBuilder(material)
                .name("&e&l" + name)
                .lore("&7" + description, "",
                      "&aLeft-click: &7open history",
                      "&aRight-click: &7print in chat")
                .build();
    }

    private ItemStack createStatusItem() {
        Player online = Bukkit.getPlayer(report.targetUuid());
        if (online != null) {
            return new ItemBuilder(Material.INK_SACK, 10)
                    .name("&a&lPlayer Status: ONLINE")
                    .lore("&f" + report.targetName() + " &7is currently online.",
                          "", "&aClick to teleport").build();
        }
        long lastPlayed = Bukkit.getOfflinePlayer(report.targetUuid()).getLastPlayed();
        return new ItemBuilder(Material.INK_SACK, 8)
                .name("&c&lPlayer Status: OFFLINE")
                .lore("&f" + report.targetName() + " &7is currently offline.",
                      lastPlayed > 0L
                        ? "&7Last seen: &f" + TimeUtil.ago(lastPlayed)
                        : "&7Last seen: &funknown").build();
    }

    @Override
    public void onClick(InventoryClickEvent event, Player admin) {
        if (busy) return;
        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        boolean print = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;

        switch (slot) {
            case 49: AdminListGui.open(plugin, admin, returnPage); return;
            case 28: resolve(admin, ReportOutcome.APPROVED); return;
            case 30: resolve(admin, ReportOutcome.REJECTED); return;
            case 32: resolve(admin, ReportOutcome.INVALID_CATEGORY); return;
            case 34: teleport(admin); return;
        }
        handleEvidenceClick(admin, slot, print);
    }

    private void handleEvidenceClick(Player admin, int slot, boolean print) {
        EvidenceGui.Source source = null;
        if (report.category() == ReportCategory.CHAT_ABUSE) {
            for (int i = 0; i < CHAT_SLOTS.length; i++)
                if (CHAT_SLOTS[i] == slot) source = CHAT_SOURCES[i];
        } else if (report.category() == ReportCategory.CHEATING && slot == 13) {
            source = EvidenceGui.Source.GRIM;
        }
        if (source == null) return;
        if (print) EvidenceGui.print(plugin, admin, report, source);
        else EvidenceGui.open(plugin, admin, report, source, returnPage);
    }

    private void resolve(final Player admin, ReportOutcome outcome) {
        busy = true;
        plugin.reports().resolve(admin, report, outcome, new Runnable() {
            public void run() { AdminListGui.open(plugin, admin, returnPage); }
        });
    }

    private void teleport(Player admin) {
        Player target = Bukkit.getPlayer(report.targetUuid());
        if (target == null) { plugin.messages().send(admin, "player-offline"); return; }
        admin.closeInventory();
        admin.teleport((Entity) target);
        plugin.messages().send(admin, "teleported", "{player}", target.getName());
    }
}

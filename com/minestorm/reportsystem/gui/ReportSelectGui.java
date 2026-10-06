package com.minestorm.reportsystem.gui;

import com.minestorm.reportsystem.ReportSystem;
import com.minestorm.reportsystem.model.ReportCategory;
import com.minestorm.reportsystem.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportSelectGui extends GuiHolder {
    private final ReportSystem plugin;
    private final String targetName;
    private final UUID targetUuid;
    private final Map<Integer, ReportCategory> categories = new HashMap<Integer, ReportCategory>();

    public ReportSelectGui(ReportSystem plugin, String targetName, UUID targetUuid) {
        this.plugin = plugin; this.targetName = targetName; this.targetUuid = targetUuid;
    }

    public void open(Player player) {
        this.inventory = Bukkit.createInventory(this, 27,
                plugin.messages().title("select", "{player}", targetName));
        ItemStack filler = ItemBuilder.filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        inventory.setItem(4, new ItemBuilder(Material.SKULL_ITEM, 3)
                .skullOwner(targetName)
                .name("&c&l" + targetName)
                .lore("&7You are reporting this player.",
                      "&7Select the rule they broke below.")
                .build());

        int slot = 10;
        for (ReportCategory category : ReportCategory.values()) {
            List<String> lore = new ArrayList<String>();
            lore.add("&8Rule");
            for (String line : category.getDescription()) lore.add("&7" + line);
            lore.add("");
            lore.add("&aClick to report for this reason");

            inventory.setItem(slot, new ItemBuilder(category.getIcon())
                    .name("&e&l" + category.getDisplayName())
                    .lore(lore).build());
            categories.put(Integer.valueOf(slot), category);
            slot++;
        }
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event, Player player) {
        ReportCategory category = categories.get(Integer.valueOf(event.getRawSlot()));
        if (category == null) return;
        player.closeInventory();
        plugin.reports().submit(player, targetName, targetUuid, category);
    }
}

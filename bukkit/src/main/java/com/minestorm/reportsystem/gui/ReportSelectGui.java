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
    private final Map<Integer, ReportCategory> map = new HashMap<Integer, ReportCategory>();

    public ReportSelectGui(ReportSystem p, String tn, UUID tu) {
        plugin = p; targetName = tn; targetUuid = tu;
    }

    public void open(Player p) {
        inventory = Bukkit.createInventory(this, 27,
            plugin.messages().title("select", "{player}", targetName));
        ItemStack fill = ItemBuilder.filler();
        for (int i = 0; i < 27; i++) inventory.setItem(i, fill);

        inventory.setItem(4, new ItemBuilder(Material.SKULL_ITEM, 3)
            .skullOwner(targetName).name("&c&l" + targetName)
            .lore("&7You are reporting this player.",
                  "&7Select the rule they broke below.").build());

        int slot = 10;
        for (ReportCategory c : ReportCategory.values()) {
            List<String> lore = new ArrayList<String>();
            lore.add("&8Rule");
            for (String line : c.getDescription()) lore.add("&7" + line);
            lore.add(""); lore.add("&aClick to report");
            inventory.setItem(slot, new ItemBuilder(c.getIcon())
                .name("&e&l" + c.getDisplayName()).lore(lore).build());
            map.put(Integer.valueOf(slot), c);
            slot++;
        }
        p.openInventory(inventory);
    }

    public void onClick(InventoryClickEvent e, Player p) {
        ReportCategory c = map.get(Integer.valueOf(e.getRawSlot()));
        if (c == null) return;
        p.closeInventory();
        plugin.reports().submit(p, targetName, targetUuid, c);
    }
}

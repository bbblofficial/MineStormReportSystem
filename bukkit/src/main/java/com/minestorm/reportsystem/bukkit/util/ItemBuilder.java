package com.minestorm.reportsystem.bukkit.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material m) { this(m, 0); }
    public ItemBuilder(Material m, int data) {
        item = new ItemStack(m, 1, (short) data);
        meta = item.getItemMeta();
    }
    public static ItemStack filler() {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 7).name(" ").build();
    }
    public ItemBuilder name(String n) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', n));
        return this;
    }
    public ItemBuilder lore(String... lines) {
        List<String> out = new ArrayList<>();
        for (String l : Arrays.asList(lines)) out.add(ChatColor.translateAlternateColorCodes('&', l));
        meta.setLore(out);
        return this;
    }
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}

package com.minestorm.reportsystem.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material m) { this(m, 0); }
    public ItemBuilder(Material m, int data) {
        this.item = new ItemStack(m, 1, (short) data);
        this.meta = this.item.getItemMeta();
    }
    public static ItemStack filler() {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 7).name(" ").build();
    }
    public ItemBuilder name(String n) {
        if (meta != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', n));
        return this;
    }
    public ItemBuilder lore(String... lines) {
        List<String> out = new ArrayList<String>();
        for (String line : lines) out.add(ChatColor.translateAlternateColorCodes('&', line));
        if (meta != null) meta.setLore(out);
        return this;
    }
    public ItemBuilder lore(List<String> lines) {
        if (meta != null) meta.setLore(new ArrayList<String>(lines));
        return this;
    }
    public ItemBuilder skullOwner(String owner) {
        if (meta instanceof SkullMeta) ((SkullMeta) meta).setOwner(owner);
        return this;
    }
    public ItemStack build() {
        if (meta != null) {
            try { meta.addItemFlags(ItemFlag.values()); } catch (Throwable ignored) {}
            item.setItemMeta(meta);
        }
        return item;
    }
}

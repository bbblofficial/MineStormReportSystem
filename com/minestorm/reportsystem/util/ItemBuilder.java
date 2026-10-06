package com.minestorm.reportsystem.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) { this(material, 0); }

    public ItemBuilder(Material material, int data) {
        this.item = new ItemStack(material, 1, (short) data);
        this.meta = this.item.getItemMeta();
    }

    public static ItemStack filler() {
        return new ItemBuilder(Material.STAINED_GLASS_PANE, 7).name(" ").build();
    }

    public ItemBuilder name(String name) {
        if (meta != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<String> out = new ArrayList<String>();
        for (String line : Arrays.asList(lines))
            out.add(ChatColor.translateAlternateColorCodes('&', line));
        if (meta != null) meta.setLore(out);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        return lore(lines.toArray(new String[0]));
    }

    public ItemBuilder loreRaw(List<String> lines) {
        if (meta != null) meta.setLore(new ArrayList<String>(lines));
        return this;
    }

    public ItemBuilder skullOwner(String owner) {
        if (meta instanceof SkullMeta)
            ((SkullMeta) meta).setOwner(owner);
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

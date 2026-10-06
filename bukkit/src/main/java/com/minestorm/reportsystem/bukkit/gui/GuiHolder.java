package com.minestorm.reportsystem.bukkit.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class GuiHolder implements InventoryHolder {
    protected Inventory inventory;
    @Override public Inventory getInventory() { return inventory; }
    public abstract void onClick(InventoryClickEvent e, Player p);
}

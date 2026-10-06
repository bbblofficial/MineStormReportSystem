package com.minestorm.reportsystem.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryHolder h = top.getHolder();
        if (!(h instanceof GuiHolder)) return;
        e.setCancelled(true);
        HumanEntity who = e.getWhoClicked();
        if (!(who instanceof Player)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;
        ((GuiHolder) h).onClick(e, (Player) who);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof GuiHolder)
            e.setCancelled(true);
    }
}

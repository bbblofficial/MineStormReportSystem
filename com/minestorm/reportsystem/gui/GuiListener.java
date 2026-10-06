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
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof GuiHolder)) return;
        event.setCancelled(true);
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) return;
        ((GuiHolder) holder).onClick(event, (Player) who);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder)
            event.setCancelled(true);
    }
}

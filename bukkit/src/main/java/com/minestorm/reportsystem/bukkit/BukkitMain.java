package com.minestorm.reportsystem.bukkit;

import com.minestorm.reportsystem.bukkit.gui.GuiListener;
import com.minestorm.reportsystem.bukkit.listener.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitMain extends JavaPlugin {
    private static BukkitMain instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "minestorm:rs");
        getServer().getMessenger().registerIncomingPluginChannel(this, "minestorm:rs",
            new com.minestorm.reportsystem.bukkit.messaging.BukkitMessaging(this));

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        getLogger().info("MineStormReportSystem (Bukkit bridge) enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MineStormReportSystem (Bukkit bridge) disabled.");
    }

    public static BukkitMain get() { return instance; }
}

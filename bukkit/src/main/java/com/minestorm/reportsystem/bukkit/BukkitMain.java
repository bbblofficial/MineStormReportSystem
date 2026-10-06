package com.minestorm.reportsystem.bukkit;

import com.minestorm.reportsystem.bukkit.gui.GuiListener;
import com.minestorm.reportsystem.bukkit.listener.ChatListener;
import com.minestorm.reportsystem.bukkit.listener.CommandInterceptor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Backend bridge for MineStormReportSystem.
 *
 * کاری که این پلاگین انجام می‌ده:
 *   1. چت بازیکن‌ها را به پروکسی می‌فرستد
 *   2. اینترسپتور /report نصب می‌کند تا پلاگین‌های دیگر کنسل شوند
 *   3. GUI را (اگر پروکسی درخواست بدهد) باز می‌کند
 *
 * تمام منطق و دیتابیس روی پروکسی هستند.
 */
public final class BukkitMain extends JavaPlugin {

    private static BukkitMain instance;

    @Override
    public void onEnable() {
        instance = this;

        // config.yml اختیاری است — با try/catch
        try {
            saveResource("config.yml", false);
        } catch (Throwable ignored) {
            // کانفیگ اجباری نیست
        }

        // ثبت کانال پیام‌رسانی
        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "minestorm:rs");
            getServer().getMessenger().registerIncomingPluginChannel(
                    this, "minestorm:rs",
                    new com.minestorm.reportsystem.bukkit.messaging.BukkitMessaging(this));
        } catch (Throwable ex) {
            getLogger().warning("Plugin channel registration failed: " + ex.getMessage());
        }

        // ثبت لیسنرها با try/catch جداگانه تا کرش نکنه
        try {
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        } catch (Throwable ex) {
            getLogger().warning("Failed to register ChatListener: " + ex.getMessage());
        }

        try {
            getServer().getPluginManager().registerEvents(new GuiListener(), this);
        } catch (Throwable ex) {
            getLogger().warning("Failed to register GuiListener: " + ex.getMessage());
        }

        try {
            getServer().getPluginManager().registerEvents(new CommandInterceptor(this), this);
            getLogger().info("CommandInterceptor registered — /report of other plugins will be blocked.");
        } catch (Throwable ex) {
            getLogger().warning("Failed to register CommandInterceptor: " + ex.getMessage());
        }

        getLogger().info("MineStormReportSystem (Bukkit bridge) enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MineStormReportSystem (Bukkit bridge) disabled.");
    }

    public static BukkitMain get() {
        return instance;
    }
}

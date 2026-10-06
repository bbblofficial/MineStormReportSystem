package com.minestorm.reportsystem.service;

import com.minestorm.reportsystem.ReportSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GrimHook implements Listener {
    private final ReportSystem plugin;
    private boolean active;

    public GrimHook(ReportSystem plugin) { this.plugin = plugin; }
    public boolean isActive() { return active; }

    public void hook() {
        if (!plugin.getConfig().getBoolean("grim.enabled", true)) return;
        Plugin grim = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grim == null || !grim.isEnabled()) {
            plugin.getLogger().info("GrimAC not found - Grim alert logging disabled.");
            return;
        }
        for (String className : plugin.getConfig().getStringList("grim.event-classes")) {
            try {
                Class<?> raw = Class.forName(className, true, grim.getClass().getClassLoader());
                if (!Event.class.isAssignableFrom(raw)) continue;
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) raw;
                Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR,
                    new org.bukkit.plugin.EventExecutor() {
                        public void execute(Listener listener, Event event) {
                            handle(event);
                        }
                    }, plugin, true);
                active = true;
                plugin.getLogger().info("Hooked into GrimAC (" + className + ").");
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Could not register Grim event " + className + ": " + ex.getMessage());
            }
        }
        plugin.getLogger().warning("GrimAC installed but no compatible flag event found.");
    }

    private void handle(Event event) {
        try {
            Object playerObject = call(event, "getPlayer");
            UUID uuid = null;
            String name = null;
            if (playerObject instanceof Player) {
                Player p = (Player) playerObject;
                uuid = p.getUniqueId();
                name = p.getName();
            } else if (playerObject != null) {
                Object rawUuid = call(playerObject, "getUniqueId");
                Object rawName = call(playerObject, "getName");
                if (rawUuid instanceof UUID) uuid = (UUID) rawUuid;
                if (rawName != null) name = rawName.toString();
            }
            if (uuid == null) return;

            String checkName = "Unknown";
            Object check = call(event, "getCheck");
            if (check != null) {
                Object v = call(check, "getCheckName", "getName", "getDisplayName");
                checkName = v != null ? v.toString() : check.toString();
            } else {
                Object v = call(event, "getCheckName");
                if (v != null) checkName = v.toString();
            }

            Object verboseObj = call(event, "getVerbose", "getVerboseInfo");
            String verbose = verboseObj == null ? "" : verboseObj.toString();
            Object vl = call(event, "getViolations", "getVl");
            if (vl != null) verbose = "VL " + vl + (verbose.isEmpty() ? "" : (" | " + verbose));

            plugin.database().insertGrimAlert(uuid, name, checkName, verbose,
                    System.currentTimeMillis())
                .exceptionally(new java.util.function.Function<Throwable, Void>() {
                    public Void apply(Throwable error) {
                        plugin.getLogger().warning("Could not store Grim alert: " + error.getMessage());
                        return null;
                    }
                });
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Error handling Grim flag: " + ex.getMessage());
        }
    }

    private static Object call(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

package com.minestorm.reportsystem.service;

import java.util.function.Function;
import com.minestorm.reportsystem.ReportSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GrimHook implements Listener {
    private final ReportSystem plugin;
    private boolean active;
    public GrimHook(ReportSystem p) { plugin = p; }
    public boolean isActive() { return active; }

    public void hook() {
        if (!plugin.getConfig().getBoolean("grim.enabled", true)) return;
        Plugin grim = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grim == null || !grim.isEnabled()) {
            plugin.getLogger().info("GrimAC not found — Grim logging disabled.");
            return;
        }
        for (String cls : plugin.getConfig().getStringList("grim.event-classes")) {
            try {
                Class<?> raw = Class.forName(cls, true, grim.getClass().getClassLoader());
                if (!Event.class.isAssignableFrom(raw)) continue;
                @SuppressWarnings("unchecked")
                Class<? extends Event> ec = (Class<? extends Event>) raw;
                Bukkit.getPluginManager().registerEvent(ec, this, EventPriority.MONITOR,
                    new EventExecutor() { public void execute(Listener l, Event e) { handle(e); } },
                    plugin, true);
                active = true;
                plugin.getLogger().info("Hooked GrimAC (" + cls + ").");
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Grim register " + cls + ": " + e.getMessage());
            }
        }
        plugin.getLogger().warning("GrimAC installed but no compatible flag event.");
    }

    private void handle(Event e) {
        try {
            Object po = call(e, "getPlayer");
            UUID uuid = null; String name = null;
            if (po instanceof Player) { Player p = (Player) po; uuid = p.getUniqueId(); name = p.getName(); }
            else if (po != null) {
                Object u = call(po, "getUniqueId");
                Object n = call(po, "getName");
                if (u instanceof UUID) uuid = (UUID) u;
                if (n != null) name = n.toString();
            }
            if (uuid == null) return;
            String check = "Unknown";
            Object c = call(e, "getCheck");
            if (c != null) {
                Object v = call(c, "getCheckName", "getName", "getDisplayName");
                check = v != null ? v.toString() : c.toString();
            } else {
                Object v = call(e, "getCheckName");
                if (v != null) check = v.toString();
            }
            Object vo = call(e, "getVerbose", "getVerboseInfo");
            String v = vo == null ? "" : vo.toString();
            Object vl = call(e, "getViolations", "getVl");
            if (vl != null) v = "VL " + vl + (v.isEmpty() ? "" : " | " + v);
            plugin.database().insertGrimAlert(uuid, name, check, v, System.currentTimeMillis())
                .exceptionally(new java.util.function.Function<Throwable, Void>() {
                    public Void apply(Throwable t) {
                        plugin.getLogger().warning("Grim store: " + t.getMessage());
                        return null;
                    }
                });
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Grim handle: " + ex.getMessage());
        }
    }

    private static Object call(Object t, String... names) {
        for (String n : names) {
            try {
                Method m = t.getClass().getMethod(n);
                m.setAccessible(true);
                return m.invoke(t);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

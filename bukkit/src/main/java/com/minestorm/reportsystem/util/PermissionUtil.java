package com.minestorm.reportsystem.util;

import org.bukkit.command.CommandSender;

public final class PermissionUtil {
    public static final String USE = "reports.use";
    public static final String ADMIN = "reports.admin";
    public static final String BYPASS = "reports.bypass";

    public static boolean has(CommandSender s, String n) {
        if (s.isOp()) return true;
        if (s.isPermissionSet(n)) return s.hasPermission(n);
        return s.hasPermission("reports.*");
    }
}

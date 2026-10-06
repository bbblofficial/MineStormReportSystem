package com.minestorm.reportsystem.util;

import org.bukkit.command.CommandSender;

public final class PermissionUtil {
    public static final String USE = "reports.use";
    public static final String ADMIN = "reports.admin";
    public static final String BYPASS = "reports.bypass";
    public static final String WILDCARD = "reports.*";

    public static boolean has(CommandSender sender, String node) {
        if (sender.isOp()) return true;
        if (sender.isPermissionSet(node)) return sender.hasPermission(node);
        return sender.hasPermission(WILDCARD);
    }
}

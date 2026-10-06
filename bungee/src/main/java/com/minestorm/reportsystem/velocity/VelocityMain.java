package com.minestorm.reportsystem.velocity;

import com.google.inject.Inject;
import com.minestorm.reportsystem.common.db.DatabaseManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(id = "minestormreportsystem", name = "MineStormReportSystem",
        version = "1.0.0", authors = {"MineStorm"},
        description = "Proxy-side report system brain")
public final class VelocityMain {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDir;
    private DatabaseManager db;

    @Inject
    public VelocityMain(ProxyServer server, Logger logger, @DataDirectory Path dataDir) {
        this.server = server; this.logger = logger; this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent e) {
        try {
            if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
            File cfgFile = dataDir.resolve("config.yml").toFile();
            if (!cfgFile.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) Files.copy(in, cfgFile.toPath());
                }
            }
            db = new DatabaseManager(java.util.logging.Logger.getLogger("MSRS"));
            db.connect("127.0.0.1", 3306, "minestorm", "root", "", 10, false);
        } catch (Exception ex) {
            logger.error("Database init failed", ex);
            return;
        }
        server.getChannelRegistry().register(com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
            .create("minestorm", "rs"));
        server.getChannelRegistry().register(com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
            .create("minestorm", "rs"), new VelocityMessaging(this, db));

        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("report").build(),
            new VelocityCommandReport(this, db));
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("my-task").aliases("mytask").build(),
            new VelocityCommandMyTask(this, db));

        server.getScheduler().buildTask(this, () -> {
            long now = System.currentTimeMillis();
            long day = 86400000L;
            db.cleanup(now - 7 * day, now - 14 * day)
              .whenComplete((n, err) -> { if (err == null && n > 0) logger.info("Cleanup removed {} rows", n); });
        }).repeat(30, TimeUnit.MINUTES).schedule();

        logger.info("MineStormReportSystem (Velocity) enabled.");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent e) {
        if (db != null) db.shutdown();
        logger.info("MineStormReportSystem (Velocity) disabled.");
    }

    public ProxyServer server() { return server; }
    public Logger logger() { return logger; }
}

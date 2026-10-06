# MineStormReportSystem

Cross-platform report system for Minecraft networks.

## Modules

| Folder   | Runs on                             | Purpose                          |
|----------|-------------------------------------|----------------------------------|
| `bungee/`| BungeeCord **or** Velocity          | Brain: commands, DB, webhooks    |
| `bukkit/`| Spigot / Paper / Bukkit 1.8 – 1.21+ | Bridge: chat capture, GUI, punish|

Both JARs must be installed. The proxy owns the database and the logic; the backend is a
thin bridge that captures chat and opens GUIs when the proxy asks.

## Build

    mvn clean package

Outputs:

    bungee/target/MineStormReportSystem-Bungee-1.0.0.jar
    bukkit/target/MineStormReportSystem-Bukkit-1.0.0.jar

## CI

GitHub Actions builds both JARs on JDK 8, 17, 21, 25 automatically.

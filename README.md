# MineStormReportSystem

Advanced GUI-based report system for Minecraft servers.

## Features

- 🎯 **Full GUI** — `/report`, `/my-task` with item-based menus
- 💾 **Database** — SQLite (default) or MySQL, tables auto-created
- 📝 **Chat Logger** — public, private, guild channels
- 🛡 **GrimAC Hook** — logs anticheat alerts automatically
- ⚖️ **Punishments** — configurable per category (BAN/MUTE)
- 🌐 **Discord Webhook** — reports pushed to Discord
- 🚫 **Command Interceptor** — blocks rival `/report` plugins

## Install

1. Build: `mvn clean package -DskipTests`
2. Drop `bukkit/target/MineStormReportSystem-Bukkit-1.0.0.jar` in `plugins/`
3. Restart server. `config.yml` and `punishments.yml` auto-create.
4. (Optional) Edit `config.yml` for MySQL.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/report <player>` | `reports.use` | Report a player |
| `/my-task` | `reports.admin` | Open admin panel |
| `/my-task reload` | `reports.admin` | Reload config & reconnect DB |

## Permissions

| Node | Default | Description |
|------|---------|-------------|
| `reports.*` | op | All |
| `reports.use` | true | Submit reports |
| `reports.admin` | op | Review reports |
| `reports.bypass` | op | Skip cooldown |

## Auto-Setup

On first enable the plugin will:
- Copy missing `config.yml` and `punishments.yml` from JAR
- Fill missing config keys with defaults
- Create all database tables (reports, chat_logs, grim_alerts)
- Reconnect automatically if MySQL drops

## Database

Default: SQLite (zero-config). To use MySQL:

```yaml
database:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: reports
    username: root
    password: "yourpassword"
    use-ssl: false
```

Tables are auto-created.

## Troubleshooting

- **Plugin disabled on enable** → Check console for `Database connection FAILED`. Fix MySQL creds or switch to SQLITE.
- **/report does nothing** → Another plugin is overriding it. Check `/pl` and remove the rival or use `/my-task reload`.
- **Chat not logged** → Verify `chat-logging.enabled: true` in config and that the server has writing permission on `plugins/MineStormReportSystem/`.

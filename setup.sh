#!/usr/bin/env bash
# MineStormReportSystem — one-shot setup
set -e

echo "═══════════════════════════════════════════════════════"
echo "  MineStormReportSystem — Setup"
echo "═══════════════════════════════════════════════════════"

cd "$(dirname "$0")"

echo ""
echo "▶ Building with Maven..."
mvn -B clean package -DskipTests

JAR="bukkit/target/MineStormReportSystem-Bukkit-1.0.0.jar"
if [ ! -f "$JAR" ]; then
    echo "❌ Build failed: $JAR not found."
    exit 1
fi

echo ""
echo "✅ Build OK: $JAR"
echo ""
echo "▶ Contents:"
unzip -l "$JAR" | grep -E "config.yml|plugin.yml|punishments.yml" || true

echo ""
echo "▶ Verify: tables auto-create on first server start."
echo ""
echo "Install:"
echo "  cp $JAR /path/to/server/plugins/"
echo "  restart the server"
echo ""
echo "For MySQL: edit plugins/MineStormReportSystem/config.yml"

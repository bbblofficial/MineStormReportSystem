#!/usr/bin/env bash
# چک کردن JAR بعد از build
set -e
cd "$(dirname "$0")"

echo "▶ Building..."
mvn -B clean package -DskipTests

JAR="bukkit/target/MineStormReportSystem-Bukkit-1.0.0.jar"
if [ ! -f "$JAR" ]; then echo "❌ JAR not found: $JAR"; exit 1; fi

echo ""
echo "▶ Checking JAR contents..."
echo ""

MAIN_CLASS=$(unzip -p "$JAR" plugin.yml | grep "^main:" | awk '{print $2}')
echo "  plugin.yml main: $MAIN_CLASS"

CLASS_PATH="${MAIN_CLASS//.//}.class"
if unzip -l "$JAR" | grep -q "$CLASS_PATH"; then
    echo "  ✅ Main class found in JAR: $CLASS_PATH"
else
    echo "  ❌ Main class MISSING: $CLASS_PATH"
    echo "  Contents of com/minestorm:"
    unzip -l "$JAR" | grep "com/minestorm" | head -20
    exit 1
fi

for RES in config.yml plugin.yml punishments.yml; do
    if unzip -l "$JAR" | grep -q " $RES"; then
        echo "  ✅ Resource in JAR: $RES"
    else
        echo "  ❌ Resource MISSING from JAR: $RES"
        exit 1
    fi
done

echo ""
echo "✅ JAR is valid: $JAR"
echo ""
echo "Install: cp $JAR /path/to/server/plugins/"

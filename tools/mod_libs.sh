#!/usr/bin/env bash
# Puts the jars the mod compiles against into mod/libs (not committed): Create, plus the Flywheel, Ponder and
# Registrate jars bundled inside it. The mod's generator and smelters are real Create kinetic blocks.
#
#   tools/mod_libs.sh [path/to/create-1.20.1-6.0.8.jar]   (default: download it from CurseForge)
set -euo pipefail
cd "$(dirname "$0")/../mod"
mkdir -p libs
jar=libs/create-1.20.1-6.0.8.jar
if [ -n "${1:-}" ]; then cp "$1" "$jar"; fi
if [ ! -f "$jar" ]; then curl -fsSL -o "$jar" https://mediafilez.forgecdn.net/files/7178/761/create-1.20.1-6.0.8.jar; fi
# Sophisticated Backpacks + Core (compile only: the backpack tiers build on them)
[ -f libs/sophisticatedbackpacks-1.20.1-3.26.12.2206.jar ] || curl -fsSL -o libs/sophisticatedbackpacks-1.20.1-3.26.12.2206.jar https://mediafilez.forgecdn.net/files/9154/766/sophisticatedbackpacks-1.20.1-3.26.12.2206.jar
[ -f libs/sophisticatedcore-1.20.1-1.5.10.2402.jar ] || curl -fsSL -o libs/sophisticatedcore-1.20.1-1.5.10.2402.jar https://mediafilez.forgecdn.net/files/9166/87/sophisticatedcore-1.20.1-1.5.10.2402.jar
tmp="$(mktemp -d)"
unzip -q -o "$jar" 'META-INF/jarjar/*.jar' -d "$tmp"
cp "$tmp"/META-INF/jarjar/{flywheel-forge-1.20.1-1.0.5,Ponder-Forge-1.20.1-1.0.91,Registrate-MC1.20-1.3.3}.jar libs/
rm -rf "$tmp"
ls libs

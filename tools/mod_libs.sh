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
tmp="$(mktemp -d)"
unzip -q -o "$jar" 'META-INF/jarjar/*.jar' -d "$tmp"
cp "$tmp"/META-INF/jarjar/{flywheel-forge-1.20.1-1.0.5,Ponder-Forge-1.20.1-1.0.91,Registrate-MC1.20-1.3.3}.jar libs/
rm -rf "$tmp"
ls libs

#!/usr/bin/env bash
# Builds the website in site/ from the real pack files, so it can never drift from the scripts:
#   - the live config + tome recipes (the page runs them to show every enchant at any level)
#   - every item texture
#   - the UI Vault library (from a local clone of github.com/thenameiscobmarley/ui-vault)
#
#   tools/build_site.sh [path/to/ui-vault]     (default: ../ui-vault)
set -euo pipefail
cd "$(dirname "$0")/.."
uivault="${1:-../ui-vault}"
if [ ! -f "$uivault/lib/uivault.css" ]; then
  echo "error: UI Vault not found at $uivault (pass its path as the first argument)" >&2
  exit 1
fi

rm -rf site/fa site/vendor
mkdir -p site/fa/items site/fa/blocks site/vendor/uivault
cp kubejs/startup_scripts/00_fa_config.js kubejs/startup_scripts/10_fa_items.js site/fa/
cp kubejs/server_scripts/20_fa_core_recipes.js kubejs/server_scripts/25_fa_tome_recipes.js kubejs/server_scripts/30_fa_material_recipes.js \
   kubejs/server_scripts/35_fa_starter_recipes.js kubejs/server_scripts/37_fa_food_recipes.js site/fa/
cp kubejs/assets/kubejs/textures/item/*.png site/fa/items/
cp kubejs/assets/kubejs/textures/block/*.png site/fa/items/   # blocks (altar, condenser, condensed essence) use their block texture as the icon
cp mod/src/main/resources/assets/factoryascension/textures/block/*.png site/fa/blocks/   # the mod's machines (Full download)
# Release version (download links) and test counts for the page
version="${FA_VERSION:-$(git describe --tags --abbrev=0 2>/dev/null || true)}"; version="${version#v}"   # FA_VERSION=1.2.0 before tagging
# RHINO=path/to/rhino.jar counts the harness checks for real; otherwise the last known count
checks=457
if [ -n "${RHINO:-}" ]; then checks="$(java -jar "$RHINO" -version 200 tools/test/harness.js 2>&1 | grep -c '^  ok')"; fi
echo "window.FA_PACK = { version: \"${version:-1.0.0}\", checks: $checks, selftest: ${FA_SELFTEST:-82} };" > site/fa/pack.js
python3 tools/site_art.py
python3 tools/mob_art.py site site/fa/mobs.js
# Cache-busting: every page asks for its scripts and styles with ?v=<hash of their contents>, so a browser never mixes old and new files
stamp="$(cat site/*.css site/*.js site/fa/*.js | sha1sum | cut -c1-10)"
sed -i -E 's/(\.(css|js))\?v=[0-9a-z]*"/\1?v='"$stamp"'"/g; s/(href|src)="((site|pages|builder|recipes|calc)\.(css|js))"/\1="\2?v='"$stamp"'"/g' site/*.html
cp "$uivault/lib/uivault.css" "$uivault/lib/uivault.js" "$uivault/lib/css/fonts.css" site/vendor/uivault/
cp "$uivault/LICENSE" site/vendor/uivault/LICENSE
echo "UI Vault $(git -C "$uivault" rev-parse --short HEAD 2>/dev/null || echo '?')" > site/vendor/uivault/VERSION
echo "site built: $(ls site/fa/items | wc -l) textures, config + recipes, $(cat site/vendor/uivault/VERSION)"

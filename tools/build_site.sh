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
mkdir -p site/fa/items site/vendor/uivault
cp kubejs/startup_scripts/00_fa_config.js kubejs/startup_scripts/10_fa_items.js site/fa/
cp kubejs/server_scripts/20_fa_core_recipes.js kubejs/server_scripts/25_fa_tome_recipes.js kubejs/server_scripts/30_fa_material_recipes.js site/fa/
cp kubejs/assets/kubejs/textures/item/*.png site/fa/items/
cp kubejs/assets/kubejs/textures/block/ascension_altar.png site/fa/items/   # the altar shows its block texture as its icon
cp "$uivault/lib/uivault.css" "$uivault/lib/uivault.js" "$uivault/lib/css/fonts.css" site/vendor/uivault/
cp "$uivault/LICENSE" site/vendor/uivault/LICENSE
echo "UI Vault $(git -C "$uivault" rev-parse --short HEAD 2>/dev/null || echo '?')" > site/vendor/uivault/VERSION
echo "site built: $(ls site/fa/items | wc -l) textures, config + recipes, $(cat site/vendor/uivault/VERSION)"

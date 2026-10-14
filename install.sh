#!/usr/bin/env bash
# Copies the Factory Ascension KubeJS files into a Minecraft instance.
# Only this pack's files are touched; anything they would overwrite is backed up first.
#
#   ./install.sh "/path/to/instance/minecraft"
set -euo pipefail

target="${1:-}"
if [ -z "$target" ]; then
  echo "usage: $0 /path/to/instance/minecraft   (the folder that has 'mods' in it)" >&2
  exit 1
fi
if [ ! -d "$target/mods" ]; then
  echo "error: '$target' has no mods folder, so it doesn't look like a Minecraft instance" >&2
  exit 1
fi

src="$(cd "$(dirname "$0")" && pwd)/kubejs"
backup="$target/kubejs/.factory-ascension-backup-$(date +%Y%m%d-%H%M%S)"
copied=0
backed_up=0

while IFS= read -r -d '' file; do
  rel="${file#"$src"/}"
  dest="$target/kubejs/$rel"
  if [ -f "$dest" ] && ! cmp -s "$file" "$dest"; then
    mkdir -p "$(dirname "$backup/$rel")"
    cp -p "$dest" "$backup/$rel"
    backed_up=$((backed_up + 1))
  fi
  mkdir -p "$(dirname "$dest")"
  cp "$file" "$dest"
  copied=$((copied + 1))
done < <(find "$src" -type f -print0)

echo "Installed $copied files into $target/kubejs"
if [ "$backed_up" -gt 0 ]; then
  echo "Backed up $backed_up changed files to $backup"
fi

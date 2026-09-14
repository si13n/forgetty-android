#!/usr/bin/env bash
set -euo pipefail

bundle_path="${1:-app/build/outputs/bundle/release/app-release.aab}"
if [[ ! -f "$bundle_path" ]]; then
  echo "Release bundle not found: $bundle_path" >&2
  exit 1
fi

check_dir="$(mktemp -d)"
trap 'rm -rf "$check_dir"' EXIT

native_count=0
while IFS= read -r entry; do
  [[ -n "$entry" ]] || continue
  native_count=$((native_count + 1))
  lib_path="$check_dir/library.so"
  unzip -p "$bundle_path" "$entry" > "$lib_path"

  while IFS= read -r alignment; do
    alignment_value=$((alignment))
    if (( alignment_value < 16384 )); then
      echo "$entry has LOAD alignment $alignment; 0x4000 or greater is required." >&2
      exit 1
    fi
  done < <(readelf -lW "$lib_path" | awk '$1 == "LOAD" { print $NF }')
done < <(unzip -Z1 "$bundle_path" | awk '/\.so$/')

echo "Verified $native_count native libraries for 16 KB ELF LOAD alignment."

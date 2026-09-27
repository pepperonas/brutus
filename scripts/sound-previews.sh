#!/usr/bin/env bash
# Rebuilds the product page's sound previews (website/assets/sounds/<slug>.m4a) from the app's own
# generator — the same buffers the phone plays, trimmed and 6 dB quieter (see SoundExportTest).
# Needs macOS (afconvert). Afterwards: rebuild the page with the kit and deploy.
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
BRUTUS_SOUND_PREVIEWS="$tmp" ./gradlew -q :app:testDebugUnitTest --tests '*SoundExportTest' --rerun
out=website/assets/sounds
rm -rf "$out"; mkdir -p "$out"
for wav in "$tmp"/*.wav; do
  afconvert -f m4af -d aac -b 96000 "$wav" "$out/$(basename "${wav%.wav}").m4a"
done
ls "$out" | wc -l | xargs echo "previews:"
du -sh "$out"

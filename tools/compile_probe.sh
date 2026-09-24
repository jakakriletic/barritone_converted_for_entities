#!/usr/bin/env bash
# Prevede Baritone (upstream ali naš mod) proti mapiranemu Forge jarju in izpiše profil napak.
# Uporaba:
#   FORGE_JAR=tools/cache/forgeSrc-1.12.2-14.23.5.2847.jar LIBS=tools/cache/libs \
#   bash tools/compile_probe.sh references/baritone-1.12.2/src /tmp/probe
# LIBS = mapa z jarji Minecraft odvisnosti (guava, gson, log4j, netty, lwjgl, authlib, fastutil, ...).
set -euo pipefail
SRC="${1:?izvorna mapa (src)}"; OUT="${2:-/tmp/npcb-probe}"
: "${FORGE_JAR:?nastavi FORGE_JAR}"; : "${LIBS:?nastavi LIBS}"
mkdir -p "$OUT/classes"
CP="$FORGE_JAR:$(ls "$LIBS"/*.jar | tr '\n' ':')"
find "$SRC" -name '*.java' -not -path '*/launch/*' -not -path '*/test/*' > "$OUT/sources.txt"
set +e
javac --release 8 -nowarn -encoding UTF-8 -proc:none -Xmaxerrs 5000 -cp "$CP" -d "$OUT/classes" @"$OUT/sources.txt" 2> "$OUT/errors.txt"
set -e
echo "napak skupaj: $(grep -c 'error:' "$OUT/errors.txt" || true)"
echo "od tega elytra: $(grep 'error:' "$OUT/errors.txt" | grep -c elytra || true)"
grep -A3 'cannot find symbol' "$OUT/errors.txt" | grep 'symbol:' | sort | uniq -c | sort -rn | head -30

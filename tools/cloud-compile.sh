#!/usr/bin/env bash
# M0.6 — prevod in JUnit brez Gradla (oblak, Linux VM), proti istemu mapiranemu Forge jarju
# kot Windows build (MCP snapshot_20171003). Ni nadomestilo za '.\dev.ps1 build' (D-007):
# ne preveri reobf, shadow ne FG nalog.
#
# Predpogoj (enkrat, na Windowsu po setupDecompWorkspace):
#   tools/cache/forgeSrc-1.12.2-14.23.5.2847.jar
#       iz %USERPROFILE%\.gradle\caches\minecraft\net\minecraftforge\forge\
#          1.12.2-14.23.5.2847\snapshot\20171003\
#   tools/cache/libs/*.jar   knjižnice iz versionJsons/1.12.2.json + junit 4.13.2,
#                            hamcrest-core 1.3, jsr305 3.0.1 (glej 04-STANJE, M0)
#
# Uporaba (iz korena repozitorija):
#   bash tools/cloud-compile.sh            # prevod main + test, zagon vseh testov
#   bash tools/cloud-compile.sh --no-test  # samo prevod
#   bash tools/cloud-compile.sh HarnessTest NpcbConfigTest   # samo izbrani testi
#
# Izhod: build/cloud/{main,test} razredi, build/cloud/reports/npcb poročila sond.
# Exit code != 0 ob napaki prevoda ali padlem testu.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/tools/cache"
FORGE_JAR="${FORGE_JAR:-$CACHE/forgeSrc-1.12.2-14.23.5.2847.jar}"
LIBS="${LIBS:-$CACHE/libs}"
OUT="$ROOT/mod/build/cloud"

[ -f "$FORGE_JAR" ] || { echo "manjka $FORGE_JAR (glej glavo skripte)"; exit 2; }
ls "$LIBS"/*.jar >/dev/null 2>&1 || { echo "manjka $LIBS/*.jar"; exit 2; }
command -v javac >/dev/null || { echo "javac ni na voljo"; exit 2; }

CP="$FORGE_JAR:$(ls "$LIBS"/*.jar | tr '\n' ':')"
JAVAC=(javac --release 8 -encoding UTF-8 -proc:none -Xlint:-options -nowarn)

rm -rf "$OUT/main" "$OUT/test"
mkdir -p "$OUT/main" "$OUT/test" "$OUT/reports/npcb"

find "$ROOT/mod/src/main/java" -name '*.java' > "$OUT/main-sources.txt"
echo "prevod main: $(wc -l < "$OUT/main-sources.txt") datotek"
"${JAVAC[@]}" -cp "$CP" -d "$OUT/main" @"$OUT/main-sources.txt"
# FG replace '@VERSION@' se v oblaku ne zgodi; to je pričakovano.
cp -r "$ROOT/mod/src/main/resources/." "$OUT/main/" 2>/dev/null || true

if [ -d "$ROOT/mod/src/test/java" ]; then
  find "$ROOT/mod/src/test/java" -name '*.java' > "$OUT/test-sources.txt"
  echo "prevod test: $(wc -l < "$OUT/test-sources.txt") datotek"
  "${JAVAC[@]}" -cp "$OUT/main:$CP" -d "$OUT/test" @"$OUT/test-sources.txt"
fi

[ "${1:-}" = "--no-test" ] && { echo "prevod OK"; exit 0; }

if [ $# -gt 0 ]; then
  CLASSES=()
  for n in "$@"; do
    f=$(cd "$OUT/test" && find . -name "$n.class" | head -1)
    [ -n "$f" ] || { echo "ni testa $n"; exit 2; }
    f=${f#./}; CLASSES+=("${f%.class}"); done
else
  mapfile -t CLASSES < <(cd "$OUT/test" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||' | sort)
fi
CLASSES=("${CLASSES[@]//\//.}")
echo "testi: ${CLASSES[*]}"
java -Xmx1G ${NPCB_JAVA_OPTS:-} -Dnpcb.reportDir="$OUT/reports/npcb" -cp "$OUT/test:$OUT/main:$CP" org.junit.runner.JUnitCore "${CLASSES[@]}"

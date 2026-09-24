#!/usr/bin/env bash
# M0 merilo A5 / D-026: uvozni commit je bitno enak cabaletta/baritone v1.2.19.
# Primerja drevo mod/src/upstream v commitu uvoza z src/{api,main} v referenci.
# Uporaba: bash tools/check-upstream-import.sh [commit]   (privzeto: commit z "upstream import")
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; cd "$ROOT"
C="${1:-$(git log --format=%H --grep='^upstream import' | tail -1)}"
REF=references/baritone-1.12.2
[ "$(git -C $REF rev-parse HEAD)" = d9cb2d91a06501c5bcba2181509d0df80361f413 ] || { echo "referenca ni na d9cb2d9"; exit 1; }
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
git archive "$C" mod/src/upstream | tar -x -C "$T"
mkdir -p "$T/ref/java" "$T/ref/resources"
cp -r $REF/src/api/java/. $REF/src/main/java/. "$T/ref/java/"
cp -r $REF/src/main/resources/. "$T/ref/resources/"
if diff -r "$T/ref" "$T/mod/src/upstream" >/dev/null; then
  echo "OK: $C mod/src/upstream = baritone d9cb2d9 src/{api,main} ($(find "$T/ref" -type f | wc -l) datotek)"
else
  diff -rq "$T/ref" "$T/mod/src/upstream" | head -20; exit 1
fi

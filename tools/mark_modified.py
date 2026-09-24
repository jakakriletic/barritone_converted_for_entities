#!/usr/bin/env python3
"""D-004: spremenjene Baritonove datoteke dobijo v LGPL glavi vrstico
' * Modified for NPC Baritone.' (enkrat). Poženi pred commitom.

Uporaba (iz korena): python3 tools/mark_modified.py [REF]
Označi vse datoteke pod core/, ki se razlikujejo od REF (privzeto HEAD) ali so v
indeksu/delovni kopiji spremenjene. Nove datoteke (brez Baritonove glave) se preskočijo.
"""
import subprocess, sys, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE = 'mod/src/main/java/si/ladja/npcbaritone/core/'
ref = sys.argv[1] if len(sys.argv) > 1 else 'HEAD'
names = subprocess.run(['git', 'diff', '--name-only', '--diff-filter=M', ref, '--', CORE],
                       cwd=ROOT, capture_output=True, text=True, check=True).stdout.split()
MARK = ' * Modified for NPC Baritone.'
ANCHOR = ' * This file is part of Baritone.'
n = 0
for rel in names:
    p = os.path.join(ROOT, rel)
    s = open(p, encoding='utf-8').read()
    if MARK in s or ANCHOR not in s:
        continue
    s = s.replace(ANCHOR, ANCHOR + '\n' + MARK, 1)
    open(p, 'w', encoding='utf-8', newline='').write(s)
    n += 1
print('označenih:', n)

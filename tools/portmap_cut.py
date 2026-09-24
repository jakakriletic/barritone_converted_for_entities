#!/usr/bin/env python3
"""M1.2 izrez: izbriše iz mod/src/main/java/si/ladja/npcbaritone/core vse datoteke,
ki jih docs/porting/PORT-MAP.md označi z akcijami iz ACTIONS.

Uporaba (iz korena):  python3 tools/portmap_cut.py [--dry-run] [AKCIJA ...]
Privzete akcije: DROP CLIENT LATER. Datoteke ostanejo v zgodovini (uvoz 3a4ea58).
"""
import os, re, sys, subprocess
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE = 'mod/src/main/java/si/ladja/npcbaritone/core/'
args = [a for a in sys.argv[1:] if not a.startswith('--')]
ACTIONS = set(args or ['DROP', 'CLIENT', 'LATER'])
dry = '--dry-run' in sys.argv
row = re.compile(r'^\| `(?P<src>[^`]+)` \| \d+ \|[^|]*\| (?P<act>[A-Z]+) \|')
todo = []
for line in open(os.path.join(ROOT, 'docs/porting/PORT-MAP.md'), encoding='utf-8'):
    m = row.match(line)
    if not m or m['act'] not in ACTIONS:
        continue
    src = m['src']
    for pre in ('api/java/baritone/', 'main/java/baritone/'):
        if src.startswith(pre):
            rel = src[len(pre):]
            todo.append((m['act'], CORE + rel))
            break
missing = [p for _, p in todo if not os.path.exists(os.path.join(ROOT, p))]
if missing:
    sys.exit('manjkajo: %s' % missing[:5])
from collections import Counter
print(Counter(a for a, _ in todo))
if not dry and todo:
    subprocess.check_call(['git', 'rm', '-q', '--'] + [p for _, p in todo], cwd=ROOT)

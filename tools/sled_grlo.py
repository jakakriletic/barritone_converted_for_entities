#!/usr/bin/env python3
"""M7.9 - analiza sledi /npcb trace iz serije CNPC nav-run -Sled (nedeterminizem grla).

Vhod: ena sled (CSV, stolpci PathTrace.HEADER z M7.9 stolpci) na zagon. Ob sledi
<ime>-sled.csv skripta poisce se zapis zagona <ime>.json in iz njega vzame B.G.prispelo.

Kaj izpise (Markdown):
  1 po zagonih: prispeli, iskanja, razlogi preklicev in premorov, zamik uporabe
    rezultata iskanja (exec_lat), ticki v gneci/trku in zastoja - za izbrano fazo in progo
  2 med zagoni: prvi tick (od zacetka faze), kjer se polozaj istega NPC-ja razlikuje od
    referencnega zagona, in kaj se je tam zgodilo (dogodki, exec_lat)
  3 sklep: ali je exec_lat med zagoni razlicen (casovni vzrok) in ali slabi zagoni
    izstopajo po gneci/preklicih (vzrok gneca)

Faza: sled tece od faze A do konca faze B; skripta NAV_Control ob koncu faze NPC-je
teleportira na start (z pade za > 3). Odsek 0 = faza A, odsek 1 = faza B.
Proga: po x cilja (G = -10, O = 15; glej nav-run.ps1).

Uporaba:
    python3 tools/sled_grlo.py audit/m27-nav-*-p*-sled.csv
    python3 tools/sled_grlo.py --faza A --proga O --out porocilo.md sled1.csv sled2.csv
Brez odvisnosti razen standardne knjiznice (Python 3.6+).
"""
import argparse
import csv
import glob
import json
import math
import os
import re
import sys
from collections import Counter, defaultdict

GOAL = {'G': (-10.0, 78.0), 'O': (15.0, 78.0)}   # kot distToGoal v nav-control.js (brez y)
GOAL_X_TO_LANE = {-10: 'G', 15: 'O'}
TELEPORT_DZ = 3.0          # skok nazaj na start med fazama
ARRIVE_DIST = 2.0          # kot PRISPEL v nav-control.js
STALL_WINDOW = 20          # tickov
STALL_MOVE = 0.3           # blokov v oknu -> zastoj
DIVERGE_DIST = 0.25        # blokov razlike polozaja med zagonoma
M79_COLS = ('cancel_reason', 'pause', 'submit_tick', 'exec_no', 'exec_lat', 'crowd', 'collided')


def lane_of(goal):
    m = re.search(r'x=(-?\d+)', goal or '')
    return GOAL_X_TO_LANE.get(int(m.group(1))) if m else None


def fnum(v, default=0.0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


class Npc:
    def __init__(self, eid):
        self.eid = eid
        self.rows = []      # (tick, x, z, row)
        self.lane = None


def load(path):
    with open(path, newline='', encoding='utf-8') as f:
        rd = csv.DictReader(f)
        missing = [c for c in M79_COLS if c not in (rd.fieldnames or [])]
        if missing:
            raise SystemExit('%s: sled nima stolpcev M7.9 %s (knjiznica pred M7.9?)' % (path, missing))
        npcs = {}
        for r in rd:
            eid = r['entity_id']
            n = npcs.get(eid)
            if n is None:
                n = npcs[eid] = Npc(eid)
            n.rows.append((int(r['world_tick']), fnum(r['x']), fnum(r['z']), r))
            if n.lane is None:
                n.lane = lane_of(r.get('goal'))
    for n in npcs.values():
        n.rows.sort(key=lambda t: t[0])
    return npcs


def segments(npc):
    """Odseki med teleporti nazaj na start. Vrne seznam seznamov vrstic."""
    segs = [[]]
    prev = None
    for row in npc.rows:
        if prev is not None and prev[2] - row[2] > TELEPORT_DZ:
            segs.append([])
        segs[-1].append(row)
        prev = row
    return segs


def phase_rows(npc, faza):
    segs = segments(npc)
    idx = 0 if faza == 'A' else 1
    return segs[idx] if idx < len(segs) else []


def json_for(path):
    base = path[:-len('-sled.csv')] if path.endswith('-sled.csv') else None
    if base and os.path.exists(base + '.json'):
        try:
            with open(base + '.json', encoding='utf-8-sig') as f:
                return json.load(f)
        except (OSError, ValueError):
            return None
    return None


def velicina(zapis, kljuc):
    if not zapis:
        return None
    for k in ('velicine', 'Velicine'):
        if k in zapis and kljuc in zapis[k]:
            return zapis[k][kljuc]
    return None


def analyse_run(path, faza, proga):
    npcs = load(path)
    lane_npcs = [n for n in npcs.values() if lane_of_any(n) == proga]
    per = []
    starts = []
    for n in lane_npcs:
        rows = phase_rows(n, faza)
        if rows:
            starts.append(rows[0][0])
    t0 = min(starts) if starts else 0
    for n in lane_npcs:
        rows = phase_rows(n, faza)
        if not rows:
            continue
        gx, gz = GOAL[proga]
        dists = [math.hypot(x - gx, z - gz) for _, x, z, _ in rows]
        arrived_at = next((rows[i][0] - t0 for i, d in enumerate(dists) if d <= ARRIVE_DIST), None)
        ev = Counter()
        cancels = Counter()
        pauses = Counter()
        lats = []
        crowd_ticks = coll_ticks = stall_ticks = 0
        for i, (t, x, z, r) in enumerate(rows):
            for e in filter(None, (r.get('events') or '').split('|')):
                ev[e] += 1
            if r['cancel_reason']:
                cancels[r['cancel_reason']] += 1
            if r['pause']:
                pauses[r['pause']] += 1
            if r['exec_lat'] != '':
                lats.append(int(r['exec_lat']))
            moving = r.get('state') == 'MOVING'
            if moving and int(r['crowd'] or 0) > 0:
                crowd_ticks += 1
            if moving and r['collided'] == '1':
                coll_ticks += 1
            if i >= STALL_WINDOW and dists[i] > ARRIVE_DIST:
                _, px, pz, _ = rows[i - STALL_WINDOW]
                if math.hypot(x - px, z - pz) < STALL_MOVE:
                    stall_ticks += 1
        per.append({
            'eid': n.eid, 'startx': round(rows[0][1], 1), 'rows': rows, 't0': t0,
            'arrived': arrived_at, 'final': round(dists[-1], 2), 'events': ev, 'cancels': cancels,
            'pauses': pauses, 'lats': lats, 'crowd': crowd_ticks, 'coll': coll_ticks, 'stall': stall_ticks,
        })
    per.sort(key=lambda p: p['startx'])
    return {'path': path, 'npcs': per, 't0': t0, 'zapis': json_for(path)}


def lane_of_any(n):
    if n.lane:
        return n.lane
    for _, _, _, r in n.rows:
        l = lane_of(r.get('goal'))
        if l:
            n.lane = l
            return l
    return None


def fmt_counter(c):
    return ', '.join('%s %d' % (k, v) for k, v in sorted(c.items(), key=lambda kv: (-kv[1], kv[0]))) or '-'


def divergence(ref, run):
    """Prvi tick od zacetka faze, kjer se polozaj para NPC-jev (po startnem x) razlikuje."""
    by_x = {p['startx']: p for p in ref['npcs']}
    best = None
    for p in run['npcs']:
        q = by_x.get(p['startx'])
        if q is None:
            continue
        qa = {t - q['t0']: (x, z, r) for t, x, z, r in q['rows']}
        for t, x, z, r in p['rows']:
            rel = t - p['t0']
            o = qa.get(rel)
            if o is None:
                continue
            if math.hypot(x - o[0], z - o[1]) > DIVERGE_DIST:
                if best is None or rel < best[0]:
                    best = (rel, p['startx'], r, o[2])
                break
    return best


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('sledi', nargs='+')
    ap.add_argument('--faza', default='B', choices=['A', 'B'])
    ap.add_argument('--proga', default='G', choices=['G', 'O'])
    ap.add_argument('--out', default='')
    a = ap.parse_args(argv)

    # Windows lupina vzorcev ne razsiri - razsiri jih skripta (sortirano, ponovitve p1, p2, ...)
    poti = []
    for arg in a.sledi:
        najdeno = sorted(glob.glob(arg)) if any(c in arg for c in '*?[') else [arg]
        if not najdeno:
            raise SystemExit('ni datotek za %s' % arg)
        poti.extend(najdeno)
    runs = [analyse_run(p, a.faza, a.proga) for p in poti]
    out = []
    w = out.append
    w('# M7.9 - sled faze %s, proga %s (%d zagonov)' % (a.faza, a.proga, len(runs)))
    w('')
    w('Prispel = vodoravno <= %.1f bloka od cilja; zastoj = premik < %.1f bloka v %d tickih pred prihodom; '
      'gneca/trk = ticki v stanju MOVING s crowd > 0 oziroma collided.' % (ARRIVE_DIST, STALL_MOVE, STALL_WINDOW))
    w('')
    w('| zagon | prispelih (sled) | B.G.prispelo (zapis) | CALC_STARTED | preklici | premori | exec_lat (ticki: stevilo) | gneca | trk | zastoj |')
    w('|---|---:|---:|---:|---|---|---|---:|---:|---:|')
    lat_sets = []
    for r in runs:
        n = r['npcs']
        arr = sum(1 for p in n if p['arrived'] is not None)
        calc = sum(p['events']['CALC_STARTED'] + p['events']['NEXT_SEGMENT_CALC_STARTED'] for p in n)
        canc = sum((p['cancels'] for p in n), Counter())
        paus = sum((p['pauses'] for p in n), Counter())
        lats = Counter(l for p in n for l in p['lats'])
        lat_sets.append(tuple(sorted(lats.items())))
        zap = velicina(r['zapis'], '%s.%s.prispelo' % (a.faza, a.proga))
        w('| %s | %d/%d | %s | %d | %s | %s | %s | %d | %d | %d |' % (
            os.path.basename(r['path']), arr, len(n), '-' if zap is None else zap, calc, fmt_counter(canc),
            fmt_counter(paus), fmt_counter(lats), sum(p['crowd'] for p in n), sum(p['coll'] for p in n),
            sum(p['stall'] for p in n)))
    w('')
    w('## Po NPC-jih (startni x)')
    for r in runs:
        w('')
        w('**%s**' % os.path.basename(r['path']))
        w('')
        w('| x | prispel (tick) | konec (bl.) | iskanj | preklici | exec_lat | gneca | trk | zastoj |')
        w('|---:|---:|---:|---:|---|---|---:|---:|---:|')
        for p in r['npcs']:
            w('| %s | %s | %s | %d | %s | %s | %d | %d | %d |' % (
                p['startx'], '-' if p['arrived'] is None else p['arrived'], p['final'],
                p['events']['CALC_STARTED'] + p['events']['NEXT_SEGMENT_CALC_STARTED'], fmt_counter(p['cancels']),
                ','.join(map(str, p['lats'])) or '-', p['crowd'], p['coll'], p['stall']))
    if len(runs) > 1:
        w('')
        w('## Razhajanje od referencnega zagona (%s)' % os.path.basename(runs[0]['path']))
        w('')
        w('| zagon | prvi tick razlike | NPC (x) | dogodki tam | exec_lat tam | ref. dogodki |')
        w('|---|---:|---:|---|---|---|')
        for r in runs[1:]:
            d = divergence(runs[0], r)
            if d is None:
                w('| %s | brez razlike | | | | |' % os.path.basename(r['path']))
            else:
                rel, sx, row, refrow = d
                w('| %s | %d | %s | %s | %s | %s |' % (os.path.basename(r['path']), rel, sx,
                  row.get('events') or '-', row.get('exec_lat') or '-', refrow.get('events') or '-'))
        w('')
        w('## Sklep (samodejni)')
        w('')
        distinct = len(set(lat_sets))
        w('- Razlicnih porazdelitev exec_lat v %d zagonih: %d - %s.' % (
            len(runs), distinct, 'CASOVNI vzrok je verjeten (rezultat iskanja pride v razlicnem ticku)'
            if distinct > 1 else 'enaka v vseh, cas uporabe rezultata ni vir razlike'))
        arrs = [sum(1 for p in r['npcs'] if p['arrived'] is not None) for r in runs]
        worst = min(arrs)
        if worst < max(arrs):
            bad = [r for r, x in zip(runs, arrs) if x == worst]
            good = [r for r, x in zip(runs, arrs) if x == max(arrs)]
            def avg(rs, key):
                return sum(sum(p[key] for p in r['npcs']) for r in rs) / float(len(rs))
            def avgc(rs):
                return sum(sum(sum(p['cancels'].values()) for p in r['npcs']) for r in rs) / float(len(rs))
            w('- Slabi zagoni (%d/%d prispelih, %d) proti dobrim (%d): gneca %.0f/%.0f, trk %.0f/%.0f, '
              'zastoj %.0f/%.0f, preklici %.1f/%.1f tickov oz. dogodkov na zagon.' % (
                  worst, len(runs[0]['npcs']), len(bad), len(good), avg(bad, 'crowd'), avg(good, 'crowd'),
                  avg(bad, 'coll'), avg(good, 'coll'), avg(bad, 'stall'), avg(good, 'stall'), avgc(bad), avgc(good)))
        else:
            w('- Vsi zagoni imajo enako prispelih (%d); slabega zagona v seriji ni.' % worst)
    text = '\n'.join(out) + '\n'
    if a.out:
        with open(a.out, 'w', encoding='utf-8') as f:
            f.write(text)
    sys.stdout.write(text)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))

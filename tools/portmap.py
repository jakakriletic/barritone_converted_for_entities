#!/usr/bin/env python3
"""Generira docs/porting/PORT-MAP.md iz references/baritone-1.12.2.

Uporaba (iz korena projekta):  python tools/portmap.py
Pravila razvrstitve so v funkciji classify(); razlogi se sklicujejo na docs/02-ODLOCITVE.md.
"""
import os, re, io, sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
root = os.path.join(ROOT, 'references', 'baritone-1.12.2', 'src')
OUT = os.path.join(ROOT, 'docs', 'porting', 'PORT-MAP.md')
if len(sys.argv) > 1:
    root = sys.argv[1]
if len(sys.argv) > 2:
    OUT = sys.argv[2]
rows = []
CLIENT=re.compile(r'net\.minecraft\.client|Minecraft\.getMinecraft|EntityPlayerSP|WorldClient|PlayerControllerMP')
def classify(p):
    s=p.replace('\\','/')
    def r(a,why): return a,why
    if s.startswith('launch/') : return r('DROP','klientski mixini in launcher; brez coremoda (D-008)')
    if s.startswith('schematica_api/'): return r('DROP','integracija s Schematico')
    b=s.split('/java/baritone/',1)[1]
    if 'elytra' in b.lower(): return r('DROP','Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002)')
    if b.startswith('api/command') or b.startswith('command/'): return r('DROP','klientski chat ukazi; nadomesti /npcb (M3)')
    if b.startswith('utils/schematic/litematica/') or b.startswith('utils/schematic/schematica/'):
        return r('DROP','integracija s klientskim modom Schematica/Litematica (D-037)')
    if b == 'utils/schematic/SelectionSchematic.java': return r('DROP','izbira /sel; območja poda porabnik (D-037)')
    if b.startswith('api/schematic') or b.startswith('utils/schematic'): return r('LATER','M14: sheme in bralniki datotek (D-037)')
    if b.startswith('selection') or b.startswith('api/selection'): return r('DROP','izbira /sel; območja poda porabnik (D-037)')
    if b.startswith('utils/accessor') or b.startswith('api/utils/accessor'):
        if 'IChunkProviderServer' in b: return r('LATER','ni potreben: id2ChunkMap je v 1.12 javen')
        return r('DROP','mixin accessorji; v 1.12 strežniku javna polja / AT')
    if b.startswith('api/utils/gui') or b in ('utils/GuiClick.java','utils/IRenderer.java','utils/PathRenderer.java'):
        return r('CLIENT','ponovno napisano kot neobvezen debug prikaz (M3)')
    if b.startswith('process/'):
        n=os.path.basename(b)
        if n=='CustomGoalProcess.java': return r('ADAPT','edini proces v jedru: "pojdi do cilja"')
        later={'MineProcess.java':'M12: rudarjenje (D-035)','GetToBlockProcess.java':'M12: pot do bloka',
               'BackfillProcess.java':'M12: zasipanje lukenj','FarmProcess.java':'M13: farmanje',
               'BuilderProcess.java':'M14: gradnja (D-037)','FollowProcess.java':'M15: sledenje kot proces'}
        if n in later: return r('LATER',later[n])
        if n=='ExploreProcess.java': return r('DROP','raziskovanje nenaloženega sveta (D-014, D-037)')
        if n=='InventoryPauserProcess.java': return r('DROP','premik v inventarju je takojšen, ni pavze (D-034)')
        return r('DROP','proces je izpuščen')
    if b.startswith('api/process/'):
        n=os.path.basename(b)
        if n in ('IBaritoneProcess.java','ICustomGoalProcess.java','PathingCommand.java','PathingCommandType.java'): return r('KEEP','pogodba procesov')
        later={'IMineProcess.java':'M12','IGetToBlockProcess.java':'M12','IFarmProcess.java':'M13',
               'IBuilderProcess.java':'M14','IFollowProcess.java':'M15'}
        if n in later: return r('LATER',later[n])
        return r('DROP','proces je izpuščen')
    if b.startswith('cache/') or b.startswith('api/cache/'):
        n=os.path.basename(b)
        if n in ('IWorldData.java','WorldData.java','IWorldProvider.java','WorldProvider.java'): return r('REWRITE','minimalen per-dimension WorldData brez datotek na disku')
        if n=='IBlockTypeAccess.java': return r('KEEP','')
        if n in ('WorldScanner.java','FasterWorldScanner.java','IWorldScanner.java'):
            return r('LATER','M12: vzorec za strežniški ServerBlockScanner (D-035)')
        return r('DROP','klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014)')
    if b.startswith('pathing/') or b.startswith('api/pathing/'):
        return r('ADAPT' ,'jedro; preimenovanja MCP + kontekst entitete') if CLIENTF else r('KEEP','jedro iskanja poti')
    if b.startswith('behavior/') or b.startswith('api/behavior/'):
        n=os.path.basename(b)
        if n in ('PathingBehavior.java','IPathingBehavior.java','Behavior.java','IBehavior.java'): return r('ADAPT','izvajanje poti; bounded executor (D-017)')
        if 'Look' in n or b.endswith('look') or '/look/' in b: return r('ADAPT','obrat entitete namesto kamere (D-011)')
        if n=='InventoryBehavior.java': return r('LATER','M11: inventar workerja nad IItemHandler (D-034)')
        return r('DROP','waypointi igralca')
    if b.startswith('event/') or b.startswith('api/event/'):
        n=os.path.basename(b)
        if n in ('ChatEvent.java','PacketEvent.java','RenderEvent.java','TabCompleteEvent.java','WorldEvent.java','BlockInteractEvent.java','SprintStateEvent.java','RotationMoveEvent.java'): return r('DROP','klientski dogodek')
        return r('ADAPT','dogodki se prožijo iz navigatorja (D-009)')
    n=os.path.basename(b)
    special={
     'IPlayerContext.java':('REWRITE','IEntityContext nad EntityLiving + referenčni okvir (D-006, D-021)'),
     'IPlayerController.java':('REWRITE','interakcije entitete: vrata (D-015); M11 roke workerja (D-032)'),
     'InputOverrideHandler.java':('REWRITE','Input → moveForward/moveStrafing/setJumping/sprint (D-010)'),
     'PlayerMovementInput.java':('DROP','klientski MovementInput'),
     'IInputOverrideHandler.java':('KEEP',''),
     'BlockStateInterface.java':('ADAPT','strežniški id2ChunkMap, omejena kopija (D-013)'),
     'BlockStateInterfaceAccessWrapper.java':('ADAPT','Forge isSideSolid manjka (napaka prevoda)'),
     'BlockBreakHelper.java':('LATER','M11: roke workerja (D-031, D-032)'),
     'BlockPlaceHelper.java':('LATER','M11: roke workerja (D-031, D-032)'),
     'ToolSet.java':('ADAPT','orodje v roki entitete ali stub (cena rušenja); M11 iz inventarja workerja (D-034)'),
     'PathingControlManager.java':('ADAPT',''),
     'PathingCommandContext.java':('KEEP',''),
     'BaritoneProcessHelper.java':('ADAPT',''),
     'BaritoneMath.java':('KEEP',''),
     'Baritone.java':('REWRITE','vitka instanca na entiteto (NavigatorCore)'),
     'BaritoneProvider.java':('REWRITE','register instanc po entiteti, šibke reference'),
     'BaritoneAPI.java':('REWRITE','globalni vhod v knjižnico'),
     'IBaritone.java':('ADAPT',''),'IBaritoneProvider.java':('ADAPT',''),
     'Settings.java':('ADAPT','profili nastavitev na instanco; NPC privzete vrednosti (D-016)'),
     'SettingsUtil.java':('ADAPT','branje configa strežnika namesto .minecraft/baritone'),
     'Helper.java':('REWRITE','log4j namesto klepeta'),
     'NotificationHelper.java':('DROP','namizna obvestila'),
     'RayTraceUtils.java':('ADAPT',''),'RotationUtils.java':('ADAPT',''),
     'BlockUtils.java':('ADAPT','MCP preimenovanja'),
     'KeepName.java':('DROP','proguard'),
    }
    if n in special: return special[n]
    if b.startswith('utils/player/'): return ('REWRITE','EntityContext + nadzornik interakcij')
    if b.startswith('utils/pathing/'):
        if n=='Avoidance.java': return ('ADAPT','izogibanje mobom na strežniku')
        return ('KEEP','')
    if b.startswith('api/utils/'): return ('ADAPT' if CLIENTF else 'KEEP','')
    if b.startswith('utils/type'): return ('KEEP','')
    return ('REVIEW','')

def main():
    global CLIENTF
    for dp, dn, fn in os.walk(root):
        for f in fn:
            if not f.endswith('.java'):
                continue
            full = os.path.join(dp, f)
            rel = os.path.relpath(full, root).replace('\\', '/')
            if rel.startswith('test/'):
                continue
            txt = io.open(full, encoding='utf-8').read()
            CLIENTF = bool(CLIENT.search(txt))
            a, why = classify(rel)
            if a == 'KEEP' and CLIENTF:
                a = 'ADAPT'
                why = (why + '; ' if why else '') + 'klientski import'
            rows.append((rel, txt.count('\n'), 'da' if CLIENTF else '', a, why))
    rows.sort()
    cnt = Counter(); lines = Counter()
    for r in rows:
        cnt[r[3]] += 1; lines[r[3]] += r[1]
    out = io.StringIO()
    out.write('# Port map: Baritone v1.2.19 → NPC Baritone\n\n')
    out.write('Generirano s `tools/portmap.py` iz `references/baritone-1.12.2/src` (brez `src/test`). ')
    out.write('Ne urejaj ročno — spremeni pravila v skripti in jo poženi znova.\n\n')
    out.write('Akcije: **KEEP** nespremenjeno (razen relokacije paketa); **ADAPT** delne spremembe; ')
    out.write('**REWRITE** napisano znova po vzoru; **CLIENT** samo klientski debug prikaz (M3); ')
    out.write('**LATER** odloženo v naveden milestone (M11–M15); **DROP** se ne prenese. Stolpec *klient* = datoteka uvaža klientske razrede.\n\n')
    out.write('| akcija | datotek | vrstic |\n|---|---:|---:|\n')
    for k in ['KEEP', 'ADAPT', 'REWRITE', 'CLIENT', 'LATER', 'DROP', 'REVIEW']:
        if cnt[k]:
            out.write('| %s | %d | %d |\n' % (k, cnt[k], lines[k]))
    out.write('| **skupaj** | %d | %d |\n\n' % (len(rows), sum(lines.values())))
    out.write('| datoteka (`src/…`) | vrstic | klient | akcija | razlog |\n|---|---:|:-:|---|---|\n')
    for r in rows:
        out.write('| `%s` | %d | %s | %s | %s |\n' % r)
    io.open(OUT, 'w', encoding='utf-8', newline='\n').write(out.getvalue())
    print('zapisano', OUT, dict(cnt))

if __name__ == '__main__':
    main()

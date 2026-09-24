#!/usr/bin/env python3
"""Generira docs/porting/AUTOMATONE-ROADMAP.md iz references/automatone.

Uporaba (iz korena projekta):  python tools/automatone_roadmap.py
Razvrstitev commitov po milestonih je v slovarju M; kar ni v njem in ustreza SKIPKW, je SKIP.
"""
import io, os, subprocess, sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
REPO = os.path.join(ROOT, 'references', 'automatone')
OUT = os.path.join(ROOT, 'docs', 'porting', 'AUTOMATONE-ROADMAP.md')
if len(sys.argv) > 1:
    REPO = sys.argv[1]
if len(sys.argv) > 2:
    OUT = sys.argv[2]
REV = '843b8397a92efdf0c934a9e892ae34d286face03'
log = subprocess.check_output(['git', '-C', REPO, 'log', '--reverse', '--author=Pyrofab',
                               '--format=%h|%ad|%s', '--date=short', '--abbrev=8', REV]).decode('utf-8')
M={
# M1 jedro
'd386d440':('M1','izhodišče: izrezano vse, kar ni jedro (−2030 vrstic)'),
'808f2b99':('M1','odstranitev klientskega igralca iz jedra'),
'4dae7a91':('M1','računanje poti na strežniku'),
'e757b3e5':('M1','poti za navadne entitete'),
'8a97376b':('M1','IPlayerContext → IEntityContext'),
'00c370f8':('M1','iskanje poti z drugimi entitetami'),
'fb073c81':('M1','padec v praznino'),
'2ba848ca':('M1','višina oči iz entitete'),
'c32eb014':('M1','nether preverba iz sveta'),
'd3828043':('M4','zaporedno rušenje (LATER)'),
'89b1174a':('M1','nastavitve na instanco (priprava)'),
'7bd582c2':('M1','kaskadne nastavitve'),
'a1bb2422':('M1','lokalne nastavitve'),
'08d79439':('M1','globalne nastavitve preimenovane'),
'a67d6901':('M5','executor v glavnem razredu moda'),
'e3d476d5':('M5','ugašanje executorja'),
'548f9f25':('M2','sesutje ob preklicu AI moba'),
'd65e13c6':('M1','varnost strani (klient/strežnik)'),
'fa380f68':('M1','sesutje na dedicated strežniku'),
'41371c6f':('M1','NPE v isBlockNormalCube'),
'e8b74bbe':('M1','prepakiranje paketov (relokacija)'),
'843b8397':('M6','tovarna instanc v API'),
'e4d493a9':('M6','dostop iz API-ja'),
'7dd013f0':('M6','drugi modi podajo seznam izogibanja'),
'076c44ff':('M1','hitrejši feetPos'),
'38c477f1':('M1','hitro branje chunkov na strežniku'),
'1afcc8fe':('M1','NPE ob odstranjevanju entitet'),
'c212980e':('M3','ukazi v strežniški niti'),
'a893582a':('M3','ukazi samo za OP'),
# M2 noge
'8adf38cf':('M2','mobi sledijo poti (MixinMobEntity; pri nas navigator, D-008/D-009)'),
'064c4857':('M2','isPathing'),
'f1a2d467':('M2','sprint'),
'930085b5':('M2','sprint na koncu poti'),
'6ddd4b14':('M2','BitSet v InputOverrideHandler'),
'a8d793f9':('M2','gibanje neodvisno od smeri pogleda'),
'46b388ca':('M2','parkour: sprint na cilju'),
'333cc295':('M2','brez rubber bandinga na strežniku'),
'31374839':('M2','hitrost na soul sandu'),
'446120d8':('M2','privzeta višina koraka'),
'72695f8c':('LATER','sprostitev uporabljenih predmetov'),
'ebc87346':('M2','odstranjen PlayerMovementInput'),
'33b707cf':('M2','sneak se ne ponastavi vsak tick'),
'fadc7082':('M2','yaw glave ob spawnu'),
# M3 debug
'2ebfbf0f':('M3','sinhronizacija poti za izris'),
'635f8aaf':('M3','zapis paketa poti'),
'2bce8f85':('M3','izris za druge entitete privzeto izklopljen'),
'32bf8032':('M3','syncWithOps'),
'55152759':('SKIP','izbira (gradnja)'),
# M4 interakcije
'b1899f30':('M4','ali se vrata dajo odpreti'),
'3216de48':('M4','interakcije z vrati'),
'456aa6bf':('M4','Traverse v vodi pod trto'),
'8c12848f':('M4','Ascend ne koplje v vodi'),
'd5ff55ce':('M4','Pillar ne leze po tekoči vodi'),
'77aadb8a':('M4','cene gibanja v vodi'),
'64c680a9':('M4','Traverse v vodi'),
'bd80cf1d':('M4','ocene cen z rezultatnim argumentom'),
'46a3b4d1':('M4','cena kisika'),
'9b018aaa':('M4','nastavitve za pot pod vodo'),
'51694ee9':('M4','utopitve'),
'7f6f8034':('M4','sprint plavanje'),
'1fb7ad15':('M4','padec v vodo'),
'885027f1':('M4','skok do lebdeče vode'),
'5d0a1d69':('SKIP','fake player potapljanje'),
# M8 velikosti
'324bd259':('M8','dimenzije v MovementTraverse'),
'943ae1be':('M8','pozicija rušenja v Traverse'),
'3a08d43e':('M8','pametnejši MovementPillar'),
'f3b5b24a':('M8','velikosti v MovementDiagonal'),
'7b3a8a43':('M8','zaznava majhnih blokov'),
'5d9ddabe':('M8','velikosti v MovementParkour'),
'0a6399f9':('M8','dimenzije v MovementDownward'),
'39da8286':('M8','velikosti v MovementAscend'),
'a9c929f0':('M8','Traverse z velikimi entitetami'),
'2fec595b':('LATER','razširjeno postavljanje v Ascend'),
'4a94645c':('LATER','air bridging'),
'b3d431f8':('LATER','gradnja z lestvami'),
'06176954':('LATER','MLG nether preverba'),
}
SKIPKW=['changelog','readme','gradle','buildscript','proguard','fabric','icon','cca','brigadier','fake player','fake players','tab complete','command','tag','schematic','selection','mixin refmap','version','prefix','recommending','trolling','merge','compilation','scaffolding','invoker','life buoy','sysout','/click','/come','/build','/version','logger','publishing','entrypoint','argument','chat logging','protection','io operations','stack lookups','drop lookups','un-hardcode world height','sections','wrapper','it\'s automatone','actually it\'s baritone','client baritone instance','todo','renderManager','irender','world data to nbt','un-hardcode number','chunkloading cancellation','requiem','component','performance notes','settings from command','document','remove some now useless mixins','sink','ride','summoned clientside','unloading','unloadEntities','exceptions','exception spaghetti','worldscanner','chunk scanning','dependencies']

def main():
    out = io.StringIO()
    c = Counter()
    rows = []
    for line in log.strip().split('\n'):
        h, d, s = line.split('|', 2)
        if h in M:
            m, note = M[h]
        else:
            low = s.lower()
            m, note = (('SKIP', 'Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost')
                       if any(k in low for k in SKIPKW) else ('SKIP', 'ni relevantno za 1.12 NPC noge'))
        c[m] += 1
        rows.append('| `%s` | %s | %s | **%s** | %s |\n' % (h, d, s.replace('|', '/'), m, note))
    out.write('# Automatone roadmap: 167 commitov predelave, razvrščenih po naših milestonih\n\n')
    out.write('Generirano s `tools/automatone_roadmap.py` iz `references/automatone` @ `843b8397` '
              '(avtor Pyrofab, 2021-03-05 → 2021-05-26). Commit je **vzorec**, ne vir za kopiranje '
              '(D-001): pred delom na isti datoteki ga preberi z `git -C references/automatone show <hash>`.\n\n')
    out.write('SKIP pomeni: Fabric/Yarn, Cardinal Components, Brigadier ukazi, fake player, gradnja, '
              'posebnosti 1.14+ (scaffolding, tagi, višina sveta), build in changelog.\n\n')
    out.write('| za nas | commitov |\n|---|---:|\n')
    for k in ['M1', 'M2', 'M3', 'M4', 'M5', 'M6', 'M8', 'LATER', 'SKIP']:
        if c[k]:
            out.write('| %s | %d |\n' % (k, c[k]))
    out.write('\n| commit | datum | sporočilo | za nas | opomba |\n|---|---|---|---|---|\n')
    out.write(''.join(rows))
    io.open(OUT, 'w', encoding='utf-8', newline='\n').write(out.getvalue())
    print('zapisano', OUT, dict(c))

if __name__ == '__main__':
    main()

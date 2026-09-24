# 01 — Arhitektura

Kako je knjižnica zgrajena in zakaj. Odločitve so v `02-ODLOCITVE.md` (D-xxx); ta
dokument pove, kako se sestavijo v delujoč sistem.

---

## 1. V enem stavku

AI taski entitete ("možgani") še naprej odločajo, **kam**; Baritone ("noge") odloča,
**kako** — išče pot v ozadju nad posnetkom naloženih chunkov in vsak tick prevede
naslednji premik v vhode, ki jih vanilla `travel()` razume.

---

## 2. Paketi

```
si.ladja.npcbaritone
├── api/                    STABILNO od M6; edino, kar vidi CNPC (compileOnly)
│   ├── NpcBaritone         vhod: attach(EntityLiving, Profile), get(entity), detach
│   ├── INpcNavigator       goTo(BlockPos | Goal), follow(Entity), stop(), state()
│   ├── NavState            IDLE, SEARCHING, MOVING, ARRIVED, FAILED(reason)
│   ├── Profile             ime + prepis nastavitev (D-016)
│   ├── IMovementFrame      identiteta / ladijski okvir (D-021)
│   └── goals/              Baritonovi cilji (GoalBlock, GoalNear, GoalXZ, …)
├── core/                   PORT BARITONA (LGPL, D-004), relociran (D-003)
│   ├── pathing/calc        A*, odprt seznam, PathNode       ← KEEP
│   ├── pathing/movement    premiki + CalculationContext      ← KEEP/ADAPT
│   ├── pathing/path        PathExecutor                      ← ADAPT
│   ├── behavior            PathingBehavior, LookBehavior     ← ADAPT
│   ├── process             CustomGoalProcess                 ← ADAPT
│   ├── context             IEntityContext, EntityContext     ← REWRITE (D-006)
│   ├── world               BlockStateInterface, ChunkSnapshot← ADAPT (D-012, D-013)
│   ├── input               InputState (namesto InputOverrideHandler) ← REWRITE (D-010)
│   └── settings            Settings, profili                 ← ADAPT (D-016)
├── forge/                  povezava z Minecraftom (ni iz Baritona)
│   ├── BaritonePathNavigate    extends PathNavigateGround (D-018)
│   ├── BaritoneMoveHelper      extends EntityMoveHelper (D-010)
│   ├── BaritoneJumpHelper      extends EntityJumpHelper
│   ├── Attach                  refleksija za tuje entitete (D-008)
│   ├── SearchExecutor          omejen bazen, vrsta, deljenje (D-017)
│   ├── Telemetry               števci, CSV (M3/M5)
│   ├── NpcbCommand             /npcb (M3)
│   └── NpcBaritoneMod          @Mod, config, NetworkCheckHandler (D-024)
└── client/                 samo klient, neobvezno (M3): izris poti iz paketa
```

Stanje po M1: Baritonovi razredi so ohranili svoje podpakete (`core/utils/BlockStateInterface`,
`core/utils/InputOverrideHandler`, `core/api/...`); nov je `core/world/ChunkSnapshot`. BSI nosi
profil nastavitev instance (`bsi.settings`, D-016), `IEntityContext.baritone()` vodi do instance.

Pravilo meje: `core` ne uvaža `forge` in nikoli `net.minecraft.client`. `api` ne uvaža
`core` razen ciljev. Test v M1 preveri uvoze (ArchUnit-lite: skeniranje izvornih
datotek).

---

## 3. Tok enega ticka

```
EntityLiving.onUpdate()
 └─ onLivingUpdate()
     ├─ updateEntityActionState()                         [final, vanilla]
     │   ├─ goalSelector.onUpdateTasks()                   AI task pokliče
     │   │     navigator.tryMoveToXYZ(x,y,z,speed)  ─────► BaritonePathNavigate
     │   │                                                   ├─ isti cilj? → nič
     │   │                                                   └─ nov cilj → CustomGoalProcess.setGoal
     │   ├─ navigator.onUpdateNavigation()  ─────────────► instance.tick()           (D-009)
     │   │                                                   ├─ PathingBehavior:
     │   │                                                   │   ├─ rezultat iskanja pripravljen? → prevzemi
     │   │                                                   │   ├─ rabimo iskanje? → SearchExecutor.submit(
     │   │                                                   │   │      ChunkSnapshot.copy(start ∪ goal + rob))  (D-013)
     │   │                                                   │   └─ PathExecutor.onTick() → premik.update()
     │   │                                                   │          → InputState {FORWARD, JUMP, SPRINT, …}
     │   │                                                   └─ LookBehavior → želen yaw
     │   ├─ updateAITasks()
     │   ├─ moveHelper.onUpdateMoveHelper()  ────────────► BaritoneMoveHelper
     │   │                                                   ├─ Baritone vodi? ne → super (vanilla)
     │   │                                                   └─ da → yaw (≤30°/tick), setAIMoveSpeed(attr),
     │   │                                                          setMoveForward(±1), setMoveStrafing(±1),
     │   │                                                          setSprinting(...)                 (D-010, D-011)
     │   ├─ lookHelper.onUpdateLook()                        vanilla: glava gleda, kamor hoče AI
     │   └─ jumpHelper.doJump()  ────────────────────────► BaritoneJumpHelper: setJumping(JUMP)
     └─ travel(strafe, vertical, forward)                 vanilla fizika, enaka kot pri igralcu
```

Iskalne niti (`SearchExecutor`, privzeto 2):

```
submit(request)
 ├─ ista pot že teče (isti cilj, začetek v istem chunku)? → pripni se na njen rezultat
 └─ v vrsto po prednosti → nit: AStarPathFinder(CalculationContext(snapshot, profile)).calculate(timeouts)
                            → rezultat se vrne na glavno nit (prebere ga naslednji tick)
```

Glavna nit ne čaka nikoli. Iskalna nit nikoli ne piše v svet in ne bere mimo posnetka.

---

## 4. Odgovornosti

| Komponenta | Dela | Ne dela |
|---|---|---|
| AI taski (vanilla/CNPC) | izberejo cilj, kličejo navigator | ne vedo, ali je pod njimi Baritone |
| `BaritonePathNavigate` | preslika vanilla pogodbo na Baritone, debounce, hibrid za sinhrone klice | ne išče poti sam |
| Baritone instanca (`core`) | išče, izvaja, ponovno preverja, segmentira | ne premika entitete neposredno |
| `BaritoneMoveHelper`/`JumpHelper` | edina točka, ki piše vhode entitete | ne odloča o poti |
| `SearchExecutor` | niti, vrsta, deljenje, časovne omejitve | ne pozna entitet |
| `ChunkSnapshot` | omejena kopija `id2ChunkMap` na glavni niti | nikoli ne naloži chunka |
| `LookBehavior` | yaw telesa proti naslednjemu premiku | ne dotakne se glave |

---

## 5. Stanja navigatorja (za AI taske in telemetrijo)

```
IDLE ──tryMoveTo──► SEARCHING ──pot najdena──► MOVING ──na cilju──► ARRIVED
  ▲                    │   ▲                      │  │
  │                    │   └──ponovno iskanje─────┘  └──premik ne uspe / blok spremenjen
  │                    └──ni poti / časovna omejitev──► FAILED(reason)
  └──────────── clearPath / stop ────────────── (iz kateregakoli)
```

`noPath()` = `IDLE | ARRIVED | FAILED`. Razlogi za `FAILED` so kode, kot pri floti v
`ladja_mod`: `unloaded_goal`, `no_path`, `timeout`, `entity_too_large`, `queue_full`.

---

## 6. Konfiguracija (`config/npcbaritone.cfg`)

| ključ | privzeto | meje | pomen |
|---|---|---|---|
| `search.threads` | 2 | 1–8 | iskalne niti (D-017) |
| `search.queueLimit` | 64 | 8–1024 | največ čakajočih iskanj |
| `search.snapshotMarginChunks` | 8 | 2–32 | rob posnetka (D-013) |
| `search.shareRadiusChunks` | 1 | 0–4 | kdaj se iskanje deli |
| `profile.default.*` | NPC vrednosti iz §8 raziskave | — | profil (D-016) |
| `movement.speedMode` | `player` | `player`/`own` | D-010 |
| `movement.maxTurnDegrees` | 30 | 5–180 | D-011 |
| `debug.syncPathsToOps` | false | — | M3 |

Nesmiselne vrednosti se obrežejo (vzorec `FleetCommandLimits` iz `ladja_mod`).

---

## 7. Kako se to vklopi pri porabniku

**Vanilla mob (razvoj, D-025):** `/npcb attach <entity> [puppet]` → `Attach` z refleksijo
zamenja `navigator`, `moveHelper`, `jumpHelper`.

**CustomNPC rework (M7):** v `EntityNPCInterface.updateTasks()` nova veja pred
`movementType`:

```java
if (RwNav.backend(this) == Backend.BARITONE && NpcBaritoneBridge.available()) {
    this.moveHelper = NpcBaritoneBridge.moveHelper(this);
    this.navigator  = NpcBaritoneBridge.navigator(this, this.world);
} else if (this.ais.movementType == 1) { … vanilla …
```

`NpcBaritoneBridge` je v CNPC in kliče samo `api`; če mod ni naložen, je `available()`
false in vse ostane vanilla (D-005).

**Piratska posadka (M9):** `ShipNavigator` v `ladja_mod` se lahko zamenja z
`INpcNavigator` s ladijskim `IMovementFrame`.

---

## 8. Kaj se namerno NE podpira

- igralci (fake player ali pravi) — to je Baritone sam
- leteči in plavajoči NPC-ji (`movementType` 1, 2) — ostanejo vanilla/CNPC
- entitete večje od 1×2 do M8 (D-019)
- rušenje, postavljanje, inventar do M10 (D-015)
- hoja po nenaloženem svetu (D-014)

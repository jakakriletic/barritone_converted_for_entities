# Navodila za agente in seje

## Začetek vsake seje

Preberi `README.md`, nato `docs/04-STANJE.md`, nato README trenutnega milestona in
`docs/05-SEJA-PROTOKOL.md`. Odločitve v `docs/02-ODLOCITVE.md` so zavezujoče.

## Pravila projekta

- Jezik dokumentacije je slovenščina; imena v kodi so angleška.
- `references/` se nikoli ne spreminja in se iz nje ne gradi.
- Koda iz Baritona ohrani LGPL glavo; spremenjene datoteke dobijo vrstico
  `Modified for NPC Baritone` (D-004).
- Mehanske spremembe (relokacija, izrez, preimenovanja) so vedno ločen commit (D-026).
- `core/` ne uvaža `net.minecraft.client` in ne bere sveta mimo `BlockStateInterface` (D-012, D-024).
- Nobena sprememba obnašanja pri porabniku (CustomNPC, ladja_mod) brez stikala, privzeto staro obnašanje.
- "Narejeno" pomeni: prevod + JUnit + build na Windowsu + zagon v igri (protokol §3).

## Git

- Repozitorij: ta mapa, veja `main`. Oddaljeni repozitorij še ni nastavljen (predlog: `jakakriletic/npc-baritone`, javen zaradi LGPL).
- Po opravljenem in preverjenem delu commitaj samo spremembe te naloge; tujih ali že
  obstoječih sprememb ne vključuj brez izrecnega navodila.
- Nikoli force-push ali prepis skupne zgodovine brez izrecne zahteve.
- Submodula v `references/` sta pripeta; `git submodule update --init` po kloniranju.
- Na mapi, ki jo seja doseže prek oddaljenega računalnika, git potrebuje dovoljenje za
  brisanje (lock datoteke); brez njega pusti napol narejen `.git`.

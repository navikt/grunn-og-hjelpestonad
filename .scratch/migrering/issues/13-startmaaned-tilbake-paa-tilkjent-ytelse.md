# 13: Startmåned tilbake på tilkjent ytelse

**What to build:** Tilkjent ytelse får tilbake startmåned, som `startdato` i EF (ADR-0013). Startmåned er den tidligste av første andel og forrige behandlings startmåned. Reverserer ticket 01.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

🔴 Rød sone: utledningen av startmåned. Utvikleren skriver funksjonen og testene. Flyway-migreringen og koblingen i `TilkjentYtelseService` er grønn sone.

Spec: [spec.md](../spec.md)

- [ ] En ny Flyway-migrering legger til `startmaned DATE` på `tilkjent_ytelse`. V44 og V46 blir liggende.
- [ ] Eksisterende rader får første dag i måneden til første andel, og tom verdi hvis de ikke har andeler.
- [ ] En ren funksjon utleder startmåned fra andelene, forrige startmåned og eventuell opphørsmåned.
- [ ] Ved opphør uten andeler er startmåned den tidligste av opphørsmåneden og forrige startmåned. Funksjonen kaster ikke feil, slik den gamle `utledStartmåned` fra før commit `1a4d1b3` gjorde.
- [ ] `TilkjentYtelseService` henter forrige startmåned fra forrige behandling og lagrer resultatet.
- [ ] Rene tester dekker: første behandling, revurdering med andel før forrige startmåned, revurdering med andel etter, opphør uten andeler og opphør før forrige startmåned.
- [ ] En repository-test sjekker at startmåned lagres og leses som riktig måned.

# 13: Startmåned tilbake på tilkjent ytelse

**What to build:** Tilkjent ytelse får tilbake startmåned, som `startdato` i EF (ADR-0013). Startmåned er den tidligste av første andel og forrige behandlings startmåned. Reverserer ticket 01.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

🔴 Rød sone: utledningen av startmåned. Utvikleren skriver funksjonen og testene. Flyway-migreringen og koblingen i `TilkjentYtelseService` er grønn sone.

Spec: [spec.md](../spec.md)

- [x] En ny Flyway-migrering legger til `startmaned DATE` på `tilkjent_ytelse`. V44 og V46 blir liggende.
- [x] ~~Eksisterende rader får første dag i måneden til første andel, og tom verdi hvis de ikke har andeler.~~ V48 sletter all tilkjent ytelse og legger til kolonnen som `NOT NULL`. `apps/sak` er ikke i prod. Se kommentar.
- [x] En ren funksjon utleder startmåned fra andelene, forrige startmåned og eventuell opphørsmåned.
- [x] Ved opphør uten andeler er startmåned den tidligste av opphørsmåneden og forrige startmåned. Funksjonen kaster ikke feil, slik den gamle `utledStartmåned` fra før commit `1a4d1b3` gjorde.
- [x] ~~`TilkjentYtelseService` henter forrige startmåned fra forrige behandling og lagrer resultatet.~~ `BeregnTilkjentYtelseService` henter forrige startmåned fra forrige behandling, og `TilkjentYtelseService` lagrer resultatet. Se kommentar.
- [x] Rene tester dekker: første behandling, revurdering med andel før forrige startmåned, revurdering med andel etter, opphør uten andeler og opphør før forrige startmåned.
- [x] En repository-test sjekker at startmåned lagres og leses som riktig måned.

## Comments

**2026-10-30:** Ferdig.

- `utledStartmåned` er skrevet. Ved opphør kaster den ikke feil. Den kaster `UgyldigInput` bare når det verken finnes andeler, forrige startmåned eller opphørsmåned, altså en innvilgelse uten andeler og uten tidligere tilkjent ytelse.
- `StartmånedTest` dekker de fem tilfellene over, og i tillegg revurdering uten andeler og tilfellet der ingen av de tre gir en måned.
- `TilkjentYtelseService` er bare avhengig av `TilkjentYtelseRepository` (`hentTilkjentYtelse`, `lagre`, `slettTilkjentYtelseHvisFinnes`). Den nye `BeregnTilkjentYtelseService` setter sammen tilkjent ytelse fra vedtaket, og `VedtakService` kaller den. Migreringen i ticket 16 kan bruke den samme servicen.
- Beregningen av andeler er flyttet til den rene funksjonen `beregnAndeler` i `BeregnAndeler.kt`. Den er arvet fra barnetilsyn i forken og er feil for grunnstønad. Andelene skal beregnes fra sats og trygdetid fra vilkårsvurderingen, se TODO i koden.
- `startmåned` på `TilkjentYtelse` er `YearMonth`, og kolonnen er `NOT NULL`. `apps/sak` er ikke i prod, så V48 sletter all tilkjent ytelse i stedet for å fylle inn startmåned. Behandlinger i dev som allerede har vedtak, mangler tilkjent ytelse til vedtaket lagres på nytt.

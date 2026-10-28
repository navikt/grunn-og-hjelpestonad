# 01: Fjern startmåned fra tilkjent ytelse

**What to build:** Tilkjent ytelse har ikke lenger startmåned. Startmåned var EF-regelen der en vanlig revurdering kunne flytte starten bakover, og den erstattes av migreringsdato på migreringsbehandlinger (ADR-0011). Revurderinger og opphør gir samme tilkjent ytelse som før, bare uten startmåned.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [x] ~~Migreringen som la til startmåned (V44), er slettet.~~ V44 er beholdt, og V46 dropper kolonnen. Migreringen med ekstern behandlings-ID (V43) er beholdt. Se kommentar.
- [x] Feltet på tilkjent ytelse og funksjonen som utleder startmåned, er fjernet.
- [x] Testene for startmåned er fjernet. Testen for opphør av alle andeler sjekker fortsatt at andelslisten blir tom.
- [x] Ingen kode eller dokumentasjon i `apps/sak` viser til startmåned, bortsett fra migreringene V44 og V46.
- [x] Hele testsuiten i `apps/sak` er grønn.
- [x] ~~Commit-meldingen sier at lokale databaser der V44 er kjørt, må settes opp på nytt.~~ Gjelder ikke lenger: V46 dropper kolonnen også lokalt.

## Comments

**2026-10-26:** V44 ble deployet til dev-gcp med «Oppdrag (#95)» før ticketen ble tatt. Å slette V44 ville stoppet Flyway-valideringen i dev, så V44 er beholdt og V46 kjører `DROP COLUMN startmaned`. Spec-linjen «Den slettes i stedet for å følges av en ny migrering» stemmer derfor ikke lenger.

**2026-10-28:** Reverseres av ticket 13. Migreringen gjøres nå som i EF, med startmåned på tilkjent ytelse (ADR-0013).

# 01: Fjern startmåned fra tilkjent ytelse

**What to build:** Tilkjent ytelse har ikke lenger startmåned. Startmåned var EF-regelen der en vanlig revurdering kunne flytte starten bakover, og den erstattes av migreringsdato på migreringsbehandlinger (ADR-0011). Revurderinger og opphør gir samme tilkjent ytelse som før, bare uten startmåned.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Migreringen som la til startmåned (V44), er slettet. Den er ikke deployet. Migreringen med ekstern behandlings-ID (V43) er beholdt.
- [ ] Feltet på tilkjent ytelse og funksjonen som utleder startmåned, er fjernet.
- [ ] Testene for startmåned er fjernet. Testen for opphør av alle andeler sjekker fortsatt at andelslisten blir tom.
- [ ] Ingen kode eller dokumentasjon i `apps/sak` viser til startmåned.
- [ ] Hele testsuiten i `apps/sak` er grønn.
- [ ] Commit-meldingen sier at lokale databaser der V44 er kjørt, må settes opp på nytt.

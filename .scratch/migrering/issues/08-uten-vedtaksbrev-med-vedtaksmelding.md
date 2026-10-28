# 08: Uten vedtaksbrev, med vedtaksmelding til Infotrygd

**What to build:** En migreringsbehandling går gjennom stegene uten vedtaksbrev, fordi ingenting endres for stønadsmottaker. Beslutter må likevel godkjenne den. Når den iverksettes, får Infotrygd beskjed om å slutte å utbetale fra migreringsdatoen.

**Blocked by:** 02

**Status:** wontfix

Spec: [spec.md](../spec.md)

- [ ] Migreringsbehandlinger hopper over vedtaksbrevsteget i backend og frontend.
- [ ] Ved iverksetting lages og distribueres ikke vedtaksbrev.
- [ ] Totrinnskontrollen er som for andre behandlinger, og beslutter kan underkjenne.
- [ ] Ved iverksetting av en migreringsbehandling med årsak `MIGRERING` sendes `vedtaksmelding` med `datoStartNyGrunnstønad` lik migreringsdatoen, gjennom den eksisterende feed-klienten.
- [ ] Behandlinger med årsak `ENDRE_MIGRERINGSDATO` sender ingen `vedtaksmelding`.
- [ ] Andre behandlinger får vedtaksbrev og sender `vedtaksmelding` som før.

## Comments

**2026-10-28:** Lukket. Migreringen gjøres nå som i EF, se ADR-0013 og ny spec. Erstattet av ticket 13–25.

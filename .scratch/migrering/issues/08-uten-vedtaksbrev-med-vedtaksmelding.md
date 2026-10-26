# 08: Uten vedtaksbrev, med vedtaksmelding til Infotrygd

**What to build:** En migreringsbehandling går gjennom stegene uten vedtaksbrev, fordi ingenting endres for stønadsmottaker. Beslutter må likevel godkjenne den. Når den iverksettes, får Infotrygd beskjed om å slutte å utbetale fra migreringsdatoen.

**Blocked by:** 02

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Migreringsbehandlinger hopper over vedtaksbrevsteget i backend og frontend.
- [ ] Ved iverksetting lages og distribueres ikke vedtaksbrev.
- [ ] Totrinnskontrollen er som for andre behandlinger, og beslutter kan underkjenne.
- [ ] Ved iverksetting av en migreringsbehandling med årsak `MIGRERING` sendes `vedtaksmelding` med `datoStartNyGrunnstønad` lik migreringsdatoen, gjennom den eksisterende feed-klienten.
- [ ] Behandlinger med årsak `ENDRE_MIGRERINGSDATO` sender ingen `vedtaksmelding`.
- [ ] Andre behandlinger får vedtaksbrev og sender `vedtaksmelding` som før.

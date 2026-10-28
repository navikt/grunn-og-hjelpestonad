# 24: Unik indeks mot to åpne behandlinger på samme fagsak

**What to build:** Databasen hindrer to åpne behandlinger på samme fagsak. I dag sjekker bare `OpprettBehandlingService` det, uten lås, så to samtidige kall kan begge opprette en behandling.

**Blocked by:** None (can start immediately)

**Status:** needs-triage

Spec: [spec.md](../spec.md)

Fant sikkerhetsgjennomgangen av migreringen. Gjelder alle behandlinger, ikke bare migrering, og er derfor utenfor spec-en.

- [ ] Sjekk at ingen fagsak i dev har mer enn én åpen behandling før indeksen legges til.
- [ ] En partiell unik indeks på `behandling (fagsak_id)` for behandlinger som ikke er ferdigstilt.
- [ ] Brudd på indeksen gir samme funksjonelle feil som sjekken i `OpprettBehandlingService`.

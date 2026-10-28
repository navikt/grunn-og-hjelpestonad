# 23: Test av hele migreringen

**What to build:** Én test går gjennom en migrering fra forhåndsvisning til iverksetting, med Postgres og WireMock for `apps/infotrygd`. Testen sjekker at servicene henter, kaller og lagrer riktig. Reglene er dekket av de rene testene i de andre ticketene.

**Blocked by:** 16, 17, 18

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Forhåndsvisningen og migreringen gir samme periode og beløp.
- [ ] Behandlingen har årsak `MIGRERING`, vilkår som er maskinelt oppfylt, startmåned og faktagrunnlag fra Infotrygd.
- [ ] Et avvik i kontroll 1 gir ingen behandling.
- [ ] To samtidige kall gir én behandling.
- [ ] Iverksettingen oppretter task for `vedtaksmelding` og task for sjekk av opphør i Infotrygd.
- [ ] En saksbehandler uten tilgang til personen får 403 på begge endepunktene.

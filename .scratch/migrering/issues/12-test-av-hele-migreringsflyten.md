# 12: Test av hele migreringsflyten

**What to build:** Én test går gjennom en migrering fra opprettelse til iverksetting, med Postgres og WireMock for `apps/infotrygd`. Testen sjekker at servicene henter, kaller og lagrer riktig. Reglene er dekket av de rene testene i de andre ticketene.

**Blocked by:** 05, 06, 07, 08, og ekstern blokkering: oppdrag-delen av ADR-0011 (integrasjon med `familie-oppdrag`)

**Status:** wontfix

Spec: [spec.md](../spec.md)

- [ ] Testen oppretter en migreringsbehandling, henter perioder og vilkår fra Infotrygd og beregner.
- [ ] Testen sjekker at migreringsdatoen er lagret, og at et avvik stopper sending til beslutter.
- [ ] Testen sjekker at en henlagt migreringsbehandling ikke påvirker gjeldende migreringsdato.
- [ ] Testen sjekker at `vedtaksmelding` sendes ved iverksetting av en migrering, og ikke ved «Endre migreringsdato».
- [ ] Testen bruker oppsettet i `SpringContextTest` og WireMock som de andre klienttestene.

## Comments

**2026-10-28:** Lukket. Migreringen gjøres nå som i EF, se ADR-0013 og ny spec. Erstattet av ticket 13–25.

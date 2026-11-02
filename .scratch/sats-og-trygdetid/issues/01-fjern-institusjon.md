# 01: Fjern institusjon

**What to build:** Saksbehandler vurderer bare medlemskap og diagnose i steg 1. Vilkåret institusjon er borte fra løsningen: database, API, regler, kopiering og skjermbilde. Begrunnelsen står i ADR-0006 under «Institusjon tas ut av løsningen inntil videre».

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En ny Flyway-migrering dropper tabellen for institusjonsvilkåret. Eldre migreringer røres ikke.
- [ ] Verdien for institusjon er fjernet fra vilkårstypen, sammen med entitet, enumene for oppholdstype og unntakshjemmel, repository, service, kontroller, request og response.
- [ ] Valideringen av vilkårssteget krever bare vurderte perioder for medlemskap og diagnose.
- [ ] Utledningen av perioder bruker bare medlemskap og diagnose.
- [ ] Kopieringen til ny behandling kopierer ikke institusjon.
- [ ] Komponentene for institusjon er fjernet fra frontend, og API-klienten er generert på nytt.
- [ ] Testene for institusjon er slettet. Testene for utledning, validering og kopiering er oppdatert og grønne.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert.
- [ ] `npx tsc` passerer i frontend.

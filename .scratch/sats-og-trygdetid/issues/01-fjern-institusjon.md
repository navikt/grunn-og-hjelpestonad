# 01: Fjern institusjon

**What to build:** Saksbehandler vurderer bare medlemskap og diagnose i steg 1. Vilkåret institusjon er borte fra løsningen: database, API, regler, kopiering og skjermbilde. Begrunnelsen står i ADR-0006 under «Institusjon tas ut av løsningen inntil videre».

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [x] En ny Flyway-migrering dropper tabellen for institusjonsvilkåret. Eldre migreringer røres ikke.
- [x] Verdien for institusjon er fjernet fra vilkårstypen, sammen med entitet, enumene for oppholdstype og unntakshjemmel, repository, service, kontroller, request og response.
- [x] Valideringen av vilkårssteget krever bare vurderte perioder for medlemskap og diagnose.
- [x] Utledningen av perioder bruker bare medlemskap og diagnose.
- [x] Kopieringen til ny behandling kopierer ikke institusjon.
- [x] Komponentene for institusjon er fjernet fra frontend, og API-klienten er generert på nytt.
- [x] Testene for institusjon er slettet. Testene for utledning, validering og kopiering er oppdatert og grønne.
- [x] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert.
- [x] `npx tsc` passerer i frontend.

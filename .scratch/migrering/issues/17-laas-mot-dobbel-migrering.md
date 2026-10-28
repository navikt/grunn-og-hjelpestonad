# 17: Lås mot dobbel migrering

**What to build:** To samtidige migreringer av samme person gir én behandling. I dag sjekker `OpprettBehandlingService.opprettBehandling` først om det finnes en åpen behandling og oppretter deretter en ny, uten lås. To transaksjoner kan begge passere sjekken.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

🔴 Rød sone: låsen. En feil her gir dobbel utbetaling.

Spec: [spec.md](../spec.md)

- [ ] Migreringen tar `pg_advisory_xact_lock` på fagsakpersonen i samme transaksjon som den oppretter behandlingen.
- [ ] Låsen slippes når transaksjonen avsluttes, også ved feil.
- [ ] En test med Postgres starter to migreringer av samme person samtidig og sjekker at det finnes én behandling etterpå.

# 02: Opprett migreringsbehandling med migreringsdato

**What to build:** Saksbehandler kan opprette en migreringsbehandling for en person med løpende grunnstønad i Infotrygd og velge migreringsdatoen selv. Systemet foreslår ingen dato. Datoen lagres på behandlingen og vises i behandlingen. Gjeldende migreringsdato for saken kan hentes og brukes av senere tickets.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Årsak for behandling har verdiene `MIGRERING` og `ENDRE_MIGRERINGSDATO`. En migreringsbehandling er en behandling med en av dem.
- [ ] Migreringsdatoen lagres sammen med årsaken til behandlingen, som `DATE` med første dag i måneden. Den er tom for andre behandlinger.
- [ ] Saksbehandler kan velge årsak `MIGRERING` og migreringsdato i skjermbildet for årsak. Feltet er tomt til saksbehandler fyller det ut.
- [ ] Migreringsdatoen kan bare være neste måned eller senere. Datoer tilbake i tid kommer i ticket 10.
- [ ] En ren funksjon utleder gjeldende migreringsdato for saken: den tidligste blant iverksatte migreringsbehandlinger. Henlagte og åpne behandlinger teller ikke. For en migreringsbehandling under arbeid brukes behandlingens egen dato.
- [ ] Rene tester for funksjonen dekker: ingen migreringsbehandlinger, én iverksatt, flere iverksatte, henlagt og åpen.
- [ ] En repository-test sjekker at migreringsdatoen lagres og leses som riktig måned, og er tom for andre behandlinger.
- [ ] Migrering ligger bak en feature toggle i Unleash. Uten toggle kan ikke årsaken `MIGRERING` velges.
- [ ] Frontend-typene er generert på nytt fra OpenAPI.

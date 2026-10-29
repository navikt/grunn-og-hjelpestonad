# 19: Sperre mot førstegangsbehandling med aktive perioder i Infotrygd

**What to build:** Saksbehandler kan ikke opprette en ny førstegangsbehandling for en person som har aktive perioder for grunnstønad i Infotrygd. Feilmeldingen ber saksbehandler migrere først, som `validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad` i EF.

**Blocked by:** 14

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Sperren gjelder bare fagsaker uten behandlinger.
- [ ] En ren funksjon avgjør om personen har perioder med beløp som er nyere enn grensen i EF.
- [ ] Feilen er funksjonell og sier at personen må migreres først.
- [ ] Sperren ligger bak feature toggle for migrering.
- [ ] Rene tester dekker: ingen perioder, gamle perioder, aktive perioder og perioder uten beløp.

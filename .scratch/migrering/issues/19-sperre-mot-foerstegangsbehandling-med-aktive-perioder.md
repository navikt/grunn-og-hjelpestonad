# 19: Sperre mot førstegangsbehandling med aktive perioder i Infotrygd

**What to build:** Saksbehandler kan ikke opprette en ny førstegangsbehandling for en person som har aktive perioder for grunnstønad i Infotrygd. Feilmeldingen ber saksbehandler migrere først, som `validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad` i EF.

**Blocked by:** 14

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Sperren gjelder bare fagsaker uten behandlinger.
- [ ] En ren funksjon avgjør om personen har en gjeldende periode med beløp som er løpende eller slutter på eller etter datogrensen 2026-01-01, samme grense som migreringen. Sperren gjelder også når migreringen ville stoppet av en annen grunn, for saken må rettes i Infotrygd og migreres. EF bruker en egen grense på 4 år.
- [ ] Feilen er funksjonell og sier at personen må migreres først.
- [ ] Sperren ligger bak feature toggle for migrering.
- [ ] Rene tester dekker: ingen perioder, gamle perioder, aktive perioder og perioder uten beløp.

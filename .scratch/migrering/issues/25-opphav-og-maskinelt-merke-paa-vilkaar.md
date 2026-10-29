# 25: Opphav og maskinelt merke på vilkår

**What to build:** Hver vilkårsperiode viser hvilken behandling den først ble vurdert i, som `opphavsvilkår` i EF, og om systemet opprettet den i en migrering. Saksbehandler ser da i en revurdering hvilke vilkår ingen har vurdert.

**Blocked by:** 21, nummeret på neste Flyway-migrering (V47 brukes av branchen `bruk-ny-prosessering`)

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En Flyway-migrering legger til `opphav_behandling_id` og `maskinelt_opprettet` på tabellene for medlemskap, diagnose og institusjon.
- [ ] Nye perioder får egen behandling som opphav. Kopierte perioder beholder opphavet fra perioden de er kopiert fra.
- [ ] En periode som endres i revurderingen, får revurderingen som opphav og mister merket «maskinelt opprettet».
- [ ] Svaret til frontend har begge feltene.

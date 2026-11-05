# 14: Behandlingen perioden sist ble endret i

**What to build:** Hver periode husker hvilken behandling den sist ble endret i, som `sistEndretIBehandlingId` på `VilkårResultat` i familie-ba-sak. I dag får en kopi ny `behandlingId` og ny sporbarhet når en revurdering arver periodene, og koblingen til behandlingen der perioden ble vurdert, forsvinner. Saksbehandler og beslutter kan da ikke se om en periode er vurdert i denne behandlingen eller arvet uendret fra en tidligere.

Feltet hører til fellesdelen fra ticket 02, så det gjelder medlemskap, diagnose, ekstrautgifter (ticket 07) og trygdetid (ticket 08).

**Blocked by:** 06

**Status:** needs-triage

Spec: [spec.md](../spec.md)

- [ ] `PeriodeMedBegrunnelse` får feltet `sistEndretIBehandlingId`.
- [ ] Feltet settes etter samme regler som i familie-ba-sak:

  | Hendelse | `sistEndretIBehandlingId` |
  |---|---|
  | Saksbehandler legger inn en ny periode | gjeldende behandling |
  | Saksbehandler endrer en periode, også en arvet | gjeldende behandling |
  | Perioden blir forkortet eller delt fordi en annen periode overlapper | gjeldende behandling |
  | Perioden blir ikke berørt når en annen periode lagres | uendret |
  | Perioden blir kopiert til en ny behandling | uendret, peker fortsatt til behandlingen den sist ble endret i |

- [ ] Reglene ligger i fellesdelen (`PeriodeMedBegrunnelseService` og `medNyPeriode`), ikke i hver periodetype.
- [ ] En ny Flyway-migrering legger kolonnen `sist_endret_i_behandling_id` til i `vilkar_medlemskap` og `vilkar_diagnose`, med `NOT NULL` og fremmednøkkel til `behandling`. Eksisterende rader får verdien fra `behandling_id`.
- [ ] Tabellene i ticket 07 og 08 får kolonnen fra starten, hvis de blir laget etter denne ticketen.
- [ ] Responsen for hver periode tar med feltet med samme navn, og API-klienten er generert på nytt.
- [ ] Testene dekker hver rad i tabellen over, og kopi av en kopi: en periode som er arvet uendret gjennom to revurderinger, peker til behandlingen den sist ble endret i.

## Forbilde i familie-ba-sak

Sjekket mot `main` på commit `74816f7` (5.11.2026):

- `VilkårResultat.sistEndretIBehandlingId` (kolonnen `sist_endret_i_behandling_id`, `NOT NULL`).
- `kopierMedParent` kopierer med `copy(id = 0, …)` og beholder feltet.
- `oppdater` og `oppdaterPekerTilBehandling()` setter gjeldende behandling når saksbehandler endrer vilkåret.
- `tilpassVilkårForEndretVilkår` i `VilkårsvurderingUtils` setter gjeldende behandling på perioder som blir forkortet (`kanFlytteFom`, `kanFlytteTom`) eller delt (`kopierMedNyPeriode`).
- ba-sak har i tillegg `opprinneligKopiertIBehandlingId` og `erOpprinneligPreutfyltIBehandling`, men bare for vilkår som blir laget automatisk. Vi trenger dem ikke nå.

## Åpne spørsmål

1. **Eksisterende kopier.** Migreringen setter `behandling_id`, så rader som allerede er kopiert, ser ut som om de ble endret i revurderingen. Det er godt nok hvis ingen revurderinger med kopierte vilkår finnes i prod. Ellers må migreringen følge `forrige_behandling_id` bakover, og det gir ikke et sikkert svar når perioden er endret eller delt.
2. **Visning i skjermbildet**, for eksempel en tag med «Vurdert i tidligere behandling». Forslaget er å holde dette utenfor ticketen og lage en egen frontend-ticket.

# 16: Opprett og iverksett migrering

**What to build:** Saksbehandler migrerer en person med ett klikk. Systemet oppretter fagsak og behandling med årsak `MIGRERING`, setter vilkårene til oppfylt, beregner, kontrollerer og iverksetter uten vedtaksbrev og uten beslutter, som `MigreringService.opprettMigrering` i EF.

**Blocked by:** 13, 14, 15, 17, 25, oppdrag-delen av ADR-0011

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Årsak for behandling har verdien `MIGRERING`.
- [ ] `POST /api/migrering/{fagsakPersonId}` sjekker tilgang med `TilgangService.validerTilgangTilFagsakPerson` før noe annet, og auditlogges som opprettelse.
- [ ] Endepunktet tar låsen fra ticket 17, leser Infotrygd på nytt og kjører valideringen fra ticket 14 inne i låsen. Det bruker ikke data fra forhåndsvisningen.
- [ ] Finnes migreringen allerede, returneres den eksisterende behandlingen.
- [ ] Alle vilkår opprettes som oppfylt og markeres som maskinelt opprettet, som `VurderingService.opprettVilkårForMigrering` i EF. Sats og trygdetidsbrøk kommer fra Infotrygd etter svaret i ticket 03.
- [ ] Faktagrunnlaget fra Infotrygd lagres på behandlingen etter mønsteret i ADR-0007, med kilde `INFOTRYGD`.
- [ ] Vedtak og tilkjent ytelse lages fra perioden. Startmåned er første måned fra valideringen.
- [ ] Kontroll 1 og sjekken av simuleringen fra ticket 15 kjøres. Ved avvik rulles alt tilbake, og saksbehandler får en funksjonell feil med måned og beløp.
- [ ] Behandlingen iverksettes uten vedtaksbrev og uten beslutter. Saksbehandleren som trykket, står som saksbehandler, og systemet står som beslutter.
- [ ] Migreringen sender `SendStartBehandlingTilInfotrygdFeedTask`, som andre førstegangsbehandlinger. Det skjer av seg selv hvis behandlingen opprettes med `OpprettBehandlingService`.
- [ ] Metrikken `migrering_total` telles med `resultat` og `aarsak`. `aarsak` er enum-verdien fra valideringen.
- [ ] Endepunktet ligger bak feature toggle for migrering.

## Startbehandling i EF

EF sendte startbehandling for alle førstegangsbehandlinger, også migreringer. `FørstegangsbehandlingService`, `JournalføringService` og `MigreringService` opprettet alle `StartBehandlingTask`. Tasken sendte bare når fagsaken ikke hadde en iverksatt behandling. Det er samme regel som `forrigeBehandlingId == null` i `OpprettBehandlingService`.

Det er `vedtaksmelding` som stopper utbetalingen i Infotrygd (ticket 18), ikke startbehandling.

EF sluttet å sende startbehandling i april 2025 (familie-ef-sak `d16fcc0a`), fordi rutinen i Infotrygd var skrudd av. Spør Infotrygd om de bruker `startbehandlingsmelding` for grunnstønad.

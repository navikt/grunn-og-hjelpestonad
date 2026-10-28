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
- [ ] Sjekk om `SendStartBehandlingTilInfotrygdFeedTask` skal sendes for en migrering, og gjør som EF.
- [ ] Metrikken `migrering_total` telles med `resultat` og `aarsak`. `aarsak` er enum-verdien fra valideringen.
- [ ] Endepunktet ligger bak feature toggle for migrering.

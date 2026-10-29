# Migrering fra Infotrygd

Status: ready-for-agent

Se [ADR-0013](../../docs/adr/ADR-0013-migrering-fra-infotrygd-som-i-ef.md),
avrundingen i [ADR-0011](../../docs/adr/ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md)
og begrepene under «Behandling» i [CONTEXT.md](../../CONTEXT.md).

Spec-en erstatter versjonen fra 2026-10-27, der saksbehandler valgte
migreringsdato og beslutter godkjente migreringen. Ticket 02 og 04–12 hørte til
den versjonen og er slettet. Begrunnelsen står i ADR-0013. Ticket 01 reverseres
av ticket 13.

## Problem

Løpende saker om grunnstønad ligger i Infotrygd. Ny løsning skal overta
utbetalingen, men kan i dag ikke ta imot en sak fra Infotrygd.

## Løsning

Vi migrerer som familie-ef-sak. Saksbehandler trykker «Migrer» på en person med
løpende grunnstønad i Infotrygd. Systemet validerer perioden i Infotrygd,
oppretter fagsak og behandling med årsak `MIGRERING`, setter vilkårene til
oppfylt, beregner, kontrollerer og iverksetter uten vedtaksbrev og uten
beslutter. Ny løsning utbetaler fra inneværende måned.

Tilkjent ytelse får tilbake startmåned. En vanlig revurdering kan flytte den
bakover og ta over måneder Infotrygd har utbetalt for, som `startdato` i EF.

Maskinell migrering i batch kommer senere og bruker samme kode.

## Brukerhistorier

### Migrering

1. Som saksbehandler vil jeg se om en person kan migreres, og hvorfor ikke, slik
   at jeg vet om saken må rettes i Infotrygd først.
2. Som saksbehandler vil jeg se perioden og beløpene ny løsning vil utbetale, før
   jeg migrerer.
3. Som saksbehandler vil jeg migrere med ett klikk, slik at jeg slipper å fylle ut
   en behandling.
4. Som saksbehandler vil jeg få en feilmelding med måned og begge beløpene hvis
   beregningen avviker fra det Infotrygd utbetalte.
5. Som saksbehandler vil jeg få en feilmelding med beløpene hvis simuleringen
   viser etterbetaling eller feilutbetaling.
6. Som saksbehandler vil jeg ikke kunne opprette en ny førstegangsbehandling for
   en person med aktive perioder i Infotrygd, slik at personen blir migrert
   først.

### Stønadsmottaker og økonomi

7. Som stønadsmottaker vil jeg få det samme utbetalt etter migreringen som før.
8. Som Nav vil jeg at Infotrygd slutter å utbetale fra startmåned, slik at bare
   én løsning utbetaler for hver måned.
9. Som Nav vil jeg få varsel hvis Infotrygd fortsatt utbetaler etter
   migreringen.

### Revurdering i en migrert sak

10. Som saksbehandler vil jeg at den første revurderingen kopierer vilkårene fra
    migreringen.
11. Som saksbehandler vil jeg kunne gi andeler før startmåned i en revurdering,
    slik at ny løsning kan ta over flere måneder fra Infotrygd.

### Teamet

12. Som utvikler vil jeg at reglene er rene funksjoner, slik at de kan testes uten
    Spring og database (ADR-0008).
13. Som utvikler vil jeg se hvem som migrerte, når og ut fra hvilke data fra
    Infotrygd.

## Beslutninger om implementasjon

### Startmåned

- `startmaned` legges til igjen på `tilkjent_ytelse` i en ny Flyway-migrering.
  V44 og V46 blir liggende. Eksisterende rader får måneden til første andel, og
  tom verdi hvis de ikke har andeler.
- En ren funksjon utleder startmåned: den tidligste av første andel og forrige
  startmåned. Ved opphør uten andeler brukes opphørsmåneden i stedet for første
  andel. Den gamle `utledStartmåned` fra før commit `1a4d1b3` kastet feil i dette
  tilfellet.

### Validering av Infotrygd

- Rene funksjoner validerer periodene fra `/stonader` i `apps/infotrygd`, med
  reglene fra `InfotrygdPeriodeValideringService` i EF. Se ADR-0013 for listen.
- Resultatet er enten perioden som skal migreres, eller en lukket enum med
  årsaken til at saken ikke kan migreres, og en tekst til saksbehandler.
- Første måned er den seneste av inneværende måned og periodens fom. Har
  personen ingen periode fremover i tid, migreres siste måned av siste periode
  med beløp.
- Hvordan periode, sats, trygdetidsbrøk og utbetalt beløp leses fra Infotrygd,
  avgjøres i ticket 03. Utgangspunktet er `GjeldendeInfotrygdPerioder.utled`
  fra ADR-0012 (arv og gjeldende perioder).

### Opprett og iverksett

- `GET /api/migrering/{fagsakPersonId}` gir forhåndsvisningen: om personen kan
  migreres, årsaken hvis ikke, perioden og beløpene.
- `POST /api/migrering/{fagsakPersonId}` migrerer. Endepunktet leser Infotrygd på
  nytt og bruker ikke data fra forhåndsvisningen.
- Begge sjekker tilgang med `TilgangService.validerTilgangTilFagsakPerson` før
  noe annet, og auditlogges (`GET` som lesing, `POST` som opprettelse).
- Årsak for behandling får verdien `MIGRERING`.
- Alle vilkår opprettes som oppfylt og markeres som maskinelt opprettet, som
  `VurderingService.opprettVilkårForMigrering` i EF. Sats og trygdetidsbrøk
  kommer fra Infotrygd.
- Faktagrunnlaget fra Infotrygd lagres på behandlingen etter mønsteret i
  ADR-0007, med kilde `INFOTRYGD`.
- Behandlingen iverksettes uten vedtaksbrev og uten beslutter. Systemet står som
  beslutter, og saksbehandleren som trykket, står som saksbehandler.
- En lås per fagsakperson (`pg_advisory_xact_lock`) hindrer to samtidige
  migreringer. Valideringen kjøres på nytt inne i låsen. Finnes migreringen
  allerede, returneres den.
- Metrikken `migrering_total` har labelene `resultat` og `aarsak`. `aarsak` er
  enum-verdien fra valideringen, aldri fritekst.
- Migrering ligger bak en feature toggle i Unleash.

### Kontrollene

- Kontroll 1 er en ren funksjon. Den får utbetalingsbeløp per måned fra
  beregningen og utbetalt beløp per måned fra Infotrygd, og returnerer avvikene.
  Den sammenligner beløp etter avrundingsfunksjonen i ADR-0011.
- Sjekken av simuleringen er en ren funksjon. Den får summene for feilutbetaling
  og etterbetaling og stopper migreringen hvis en av dem ikke er 0.
- Ingen av dem kan overstyres. Overstyring av etterbetaling kommer senere.

### Infotrygd

- Iverksettingen sender `vedtaksmelding` med `datoStartNyGrunnstønad` lik
  startmåned gjennom den eksisterende feed-klienten.
- En task sjekker etter iverksettingen at Infotrygd har sluttet å utbetale, som
  `SjekkMigrertStatusIInfotrygdTask` i EF. Den går til manuell oppfølging og
  varsler hvis ikke.
- Opprettelse av en ny førstegangsbehandling stoppes hvis personen har aktive
  perioder i Infotrygd, som `validerKanOppretteBehandlingUtenÅMigrere…` i EF.

### Revurdering

- En revurdering kopierer vilkårene fra forrige behandling, som i EF. I dag
  kopierer ikke revurderinger vilkår, så endringen gjelder alle revurderinger.
- En ren funksjon utleder `opphørAlleKjederFra`: ny startmåned når en revurdering
  i en migrert sak flytter startmåned bakover, ellers `null`.
- En revurdering sender `vedtaksmelding` som i dag, med startmåned som
  `datoStartNyGrunnstønad`.

## Beslutninger om testing

- Gode tester sjekker oppførsel gjennom funksjonens inn- og utdata. Testnavnene
  beskriver regelen på norsk, som i resten av repoet.
- **Hovedgrensen er rene funksjoner uten Spring.** Den dekker:
  - hver valideringsregel for Infotrygd, med tilfellene fra
    `InfotrygdPeriodeValideringServiceTest` i EF
  - utledning av startmåned, også ved opphør uten andeler
  - kontroll 1 og sjekken av simuleringen
  - utledning av `opphørAlleKjederFra`
- **En repository-test** sjekker at startmåned lagres og leses riktig.
- **En integrasjonstest** går gjennom hele migreringen med Postgres og WireMock
  for `apps/infotrygd`, og sjekker at to samtidige kall gir én behandling.
- Tidligere eksempler: `BeregningUtilsTest` for rene funksjoner og
  `TilkjentYtelseRepositoryTest` for repository med Postgres i Testcontainers.

## Utenfor omfang

- Maskinell migrering i batch med `migreringspersoner`.
- Overstyring av etterbetaling.
- Hjelpestønad. Spec-en gjelder grunnstønad.
- En partiell unik indeks mot to åpne behandlinger på samme fagsak. Den gjelder
  alle behandlinger og er en egen ticket (24).

## Andre merknader

- **Blokkert av oppdrag.** Migreringen kan ikke iverksettes før oppdrag-delen av
  ADR-0011 sender til OS.
- **Blokkert av Økonomi.** Vi vet ikke om OS motregner mellom Infotrygd og ny
  løsning. Det må være avklart før migrering i prod og før revurdering bakover i
  en migrert sak slås på.
- **Data fra Infotrygd må avklares av et menneske** i ticket 03.
- **Godtatt risiko.** Ingen beslutter kontrollerer migreringen, og inneværende
  måned kan bli utbetalt to ganger hvis Infotrygd kjører utbetalingen etter at vi
  har simulert. Se ADR-0013.

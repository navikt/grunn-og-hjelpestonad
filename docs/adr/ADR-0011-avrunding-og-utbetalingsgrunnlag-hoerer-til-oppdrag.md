# ADR-0011: Avrunding og utbetalingsgrunnlag hører til oppdrag

**Dato:** 2026-10-26
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`apps/sak` skal sende utbetalinger til Oppdrag gjennom `familie-oppdrag`, med
`familie-utbetalingsgenerator`, slik BA og EF gjør. Helved erstattes fordi den ikke
støtter grensesnittavstemming og konsistensavstemming.

Referanseappene deler ansvaret ulikt:

- **familie-ba-sak** avrunder i beregningen og legger kjedefeltene (`periodeOffset`,
  `forrigePeriodeOffset`, `kildeBehandlingId`) rett på sakens andeler.
- **familie-ef-sak** avrunder i beregningen. **familie-ef-iverksett** er en egen app
  med sin egen kopi av tilkjent ytelse, der andelene har `beløp: Int` og kjedefeltene.

Generatoren krever `beløp: Int`. Tilkjent ytelse for grunnstønad er satsen redusert
med trygdetidsbrøken, og brøken gir desimaler. Fagsiden har ikke bestemt
avrundingsregelen.

Løpende saker skal migreres fra Infotrygd. Referanseappene håndterer også dette ulikt:

- **BA** lagrer migreringsdatoen bare på migreringsbehandlinger
  (`BehandlingMigreringsinfo`). Datoen flyttes i egne behandlinger med årsak
  `ENDRE_MIGRERINGSDATO`, og de stoppes hvis utbetalingen endres.
- **EF** lagrer `startdato` på all tilkjent ytelse, og en vanlig revurdering kan
  flytte den bakover.

## Beslutning

### Saken eier tilkjent ytelse, oppdrag eier utbetalingsbeløpet

- Tilkjent ytelse beholder alle desimaler. Saken vet ikke at beløpet blir avrundet.
- Oppdrag avrunder i én ren funksjon. Til fagsiden har bestemt regelen, runder vi
  til hele kroner med `HALF_UP`.
- Oppdrag har sine egne tabeller i samme database:
  - `oppdrag_resultat`, med nøkkel `behandling_id`: kvitteringsstatus,
    kvitteringsmelding og `utbetalingsoppdrag` (JSONB) slik det ble sendt.
  - `utbetalingsandel`: periode, utbetalingsbeløp, klassekode, `periode_id`,
    `forrige_periode_id` og `kilde_behandling_id`.
- Generatoren får forrige behandlings utbetalingsandeler fra `utbetalingsandel`.
  Vi regner dem ikke ut på nytt. Hvis avrundingsregelen endres, gjelder den nye
  regelen bare nye behandlinger, og generatoren sender bare forskjellen.
- Siste andel i hver kjede hentes med en spørring: høyeste `periode_id` per fagsak
  og klassekode. Den virker også når siste behandling opphørte alle andelene.
- All kode for oppdrag ligger i pakken `no.nav.grunn.og.hjelpestonad.oppdrag`.

### Migreringsdatoen lagres på migreringsbehandlingen, som i BA

- Migreringsdatoen er den første måneden ny løsning eier utbetalingen, og ikke
  Infotrygd. I BA ligger datoen i måneden før, se
  [alternativene](#migreringsdato-i-måneden-før-som-i-ba).
- Bare migreringsbehandlinger har migreringsdato. Den lagres som `DATE` (første dag
  i måneden) på behandlingen, som andre måneder i databasen etter V45.
- Migreringsdatoen for saken er den tidligste datoen blant iverksatte
  migreringsbehandlinger. Henlagte og åpne behandlinger teller ikke.
- Saker som ikke har vært i Infotrygd, har ingen migreringsdato. `startmåned` på
  `tilkjent_ytelse` og `utledStartmåned` fjernes.
- Migreringsdatoen kan bare flyttes bakover, og bare i en behandling med årsak
  `ENDRE_MIGRERINGSDATO` («Endre migreringsdato»-behandling).
- Saksbehandler velger migreringsdatoen, også i den første migreringen. Systemet
  foreslår ingen dato. Datoen kan være en måned Infotrygd allerede har utbetalt for.

### Migreringsbehandlinger flytter saken uten å endre utbetalingen

En migrering endrer ingenting for stønadsmottaker, og er derfor ikke en behandling
i juridisk forstand. Endringer i saken gjøres i en revurdering etter migreringen.

- Vilkår og perioder for månedene vi tar over, hentes fra Infotrygd. Saksbehandler
  fyller bare inn det som mangler eller er motstridende, og må begrunne det.
- I en «Endre migreringsdato»-behandling kopieres vilkår og perioder fra den gamle
  migreringsdatoen og fremover uendret fra forrige behandling.
- Skjermbildet låser feltene saksbehandler ikke skal endre. Backend validerer ikke
  vilkårene.
- Backend stopper behandlingen hvis en av disse kontrollene feiler. Saksbehandler
  kan ikke overstyre dem:
  1. For månedene vi tar over, er utbetalingsbeløpet per måned likt det Infotrygd
     utbetalte.
  2. Fra den gamle migreringsdatoen og fremover er utbetalingsbeløpet per måned
     likt forrige behandling.
  3. Simuleringen fra økonomi viser 0 kr i etterbetaling og feilutbetaling.
- Ved avvik henlegges behandlingen. Saken rettes i Infotrygd, eller migreres med en
  annen dato.
- Migreringsbehandlinger har ikke vedtaksbrev, men beslutter må godkjenne dem.
- «Endre migreringsdato»-behandlingen sender ingen ny `vedtaksmelding` til
  Infotrygd. Infotrygd har allerede sluttet å utbetale fra den gamle datoen.

Vi sammenligner utbetalingsbeløpet, ikke tilkjent ytelse. Infotrygd og OS ser bare
hele kroner, så tilkjent ytelse med desimaler blir aldri helt lik.

### Revurderinger kan ikke gå før migreringsdatoen

- En revurdering i en migrert sak stoppes hvis en andel starter før
  migreringsdatoen. Sjekken er en ren funksjon, se
  [ADR-0008](ADR-0008-deterministisk-logikk-utenfor-spring.md).
- Vilkårssteget stopper en vilkårsperiode som starter før migreringsdatoen, slik at
  saksbehandler får beskjed tidlig.

### Oppdrag opphører bare når migreringsdatoen flyttes

- Oppdrag setter `opphørAlleKjederFra` lik ny migreringsdato bare i en «Endre
  migreringsdato»-behandling.
- I alle andre behandlinger er `opphørAlleKjederFra` `null`. Når en revurdering i en
  sak som ikke er migrert gir en andel før den første, sender generatoren bare den
  nye andelen.

## Alternativer vurdert

### Avrunde i beregningen, som BA og EF

Koden ville ligne mer på referanseappene. Forkastet fordi regelen er uavklart, og
fordi saken da må lagre et beløp den ikke eier. Med avrundingen i oppdrag blir en
ny regel én endring i én funksjon.

### Utbetalingsgrunnlag som JSONB, som EF-iverksett

Én tabell og kode som ligner på EF-iverksett. Forkastet fordi konsistensavstemmingen
skal lese løpende utbetalingsandeler per fagsak, og fordi vi vil ha constraints på
periodene. EF-iverksett må i tillegg matche andelene mot det ef-sak sender, og den
koblingen trenger vi ikke.

### Kjedefeltene på sakens andeler, som BA

Færre tabeller. Forkastet fordi andelene da må ha både beløpet med desimaler og
utbetalingsbeløpet, og saken blir avhengig av oppdragsbegreper.

### Egen app for iverksetting, som EF

Forkastet. En egen app gir en kontrakt, en deploy og en database til, og vi har
ingen behov for å skalere eller rulle ut iverksettingen for seg.

### Startmåned på all tilkjent ytelse, som `startdato` i EF

Dette var det første forslaget i denne ADR-en. Forkastet fordi en vanlig revurdering
kunne flytte startmåneden bakover. Da kunne ny løsning ta over måneder Infotrygd
har utbetalt for, uten at noen kontrollerte beløpene. Saker som aldri har vært i
Infotrygd, fikk dessuten en startmåned uten mening.

### Migreringsdato i måneden før, som i BA

BA lagrer en dato og utbetaler fra måneden etter, så opphøret starter en måned
etter migreringsdatoen. Forkastet fordi vi vil at migreringsdatoen,
`opphørAlleKjederFra` og `datoStartNyGrunnstønad` i Infotrygd-feeden skal være
samme måned. Vi bruker likevel ordet migreringsdato, fordi saksbehandlere kjenner
det fra BA.

### Opphør utledet fra historikken, som i BA

BA utleder opphøret fra migreringsdatoen og forrige behandlings andeler, ikke fra
årsaken, og trenger ekstra regler for å unngå å sende samme opphør to ganger.
Forkastet fordi en «Endre migreringsdato»-behandling hos oss ikke kan iverksettes
uten at oppdraget er sendt. Da holder det å sette opphøret i selve behandlingen.

### Forlenge den tidligste perioden bakover, som i BA

BA kopierer vilkårene fra forrige behandling og forlenger det tidligste
vilkårsresultatet bakover til ny migreringsdato. Forkastet fordi kontrollen mot
Infotrygd da bare viser at Infotrygd hadde samme sats i de månedene. Den viser ikke
at vi har flyttet saken riktig.

### Overstyring av avvik

Saksbehandler kunne fått godkjenne et avvik i beløp eller simulering. Forkastet
fordi migreringen da blir en endring for stønadsmottaker, og en endring krever en
juridisk begrunnelse.

## Konsekvenser

### Positive

- Avrundingsregelen kan endres uten at tilkjent ytelse eller beregningen endres.
- Konsistensavstemmingen leser det OS faktisk har fått, med avrundede beløp.
- Saken har ingen avhengighet til generatoren eller kontraktene fra
  `familie-oppdrag`.
- Ny løsning tar aldri over måneder Infotrygd har utbetalt for, uten at
  utbetalingsbeløpene og simuleringen er kontrollert.

### Negative

- Vi avviker fra både BA og EF. Kode fra referanseappene må tilpasses før den
  kan brukes.
- Tilkjent ytelse og utbetalingsbeløp kan avvike med inntil 50 øre per måned.
  Skjermbilder som viser beløp, må si hvilket av dem de viser.
- Ved avvik må saksbehandler henlegge migreringsbehandlingen og starte på nytt.
- Backend stopper ikke en endret vilkårsvurdering i en migreringsbehandling så
  lenge utbetalingen er den samme. Bare beslutter ser den.
- `startmåned` (V44), `utledStartmåned` og `StartmånedTest` må fjernes eller
  skrives om.

### Risiko

- Hvis migreringsdatoen egentlig skal tolkes som i BA, blir opphøret én måned
  feil. Før prod er det en endring i én funksjon med tester. Etter første
  migrering i prod må lagrede datoer og kjeder i OS rettes for hver sak.
- Vi bruker andre fagområder og klassekoder enn Infotrygd. Vi vet ikke om OS da
  motregner det Infotrygd har utbetalt mot det vi sender for samme måned. Hvis OS
  ikke motregner, viser simuleringen etterbetaling for hver måned vi tar over, og
  kontrollen på 0 kr stopper alle migreringer med dato tilbake i tid. Ingenting blir
  utbetalt to ganger, men «Endre migreringsdato» kan da ikke brukes.

## Aksjonspunkter

- [ ] Fagsiden bekrefter at migreringsdatoen er den første måneden ny løsning
      utbetaler (EF), ikke måneden før (BA). Må være avklart før første migrering
      i prod.
- [ ] Fagsiden bestemmer avrundingsregelen før første utbetaling i prod.
- [ ] Økonomi gir fagområde og klassekode for prod. Dev bruker `IT28` og
      `GHGSORMG`.
- [ ] Økonomi bekrefter om OS motregner mellom Infotrygd og ny løsning når de
      bruker ulike fagområder og klassekoder, og hva simuleringen viser for måneder
      Infotrygd har utbetalt for. Må være avklart før vi bygger «Endre
      migreringsdato» og migrering med dato tilbake i tid.
- [ ] Infotrygd bekrefter at `vedtaksmelding` med `datoStartNyGrunnstønad` stopper
      utbetalingen i Infotrygd.
- [ ] Infotrygd bekrefter at det ikke trengs en ny `vedtaksmelding` når
      migreringsdatoen flyttes bakover.

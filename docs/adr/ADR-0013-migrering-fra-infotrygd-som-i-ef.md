# ADR-0013: Migrering fra Infotrygd som i EF

**Dato:** 2026-10-28
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

Erstatter delene om migrering i
[ADR-0011](ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md):
migreringsdato, migreringsbehandlinger, revurderingssperren og opphør når
migreringsdatoen flyttes. Avrundingen og tabellene for oppdrag i ADR-0011 gjelder
fortsatt.

## Kontekst

Løpende saker om grunnstønad skal flyttes fra Infotrygd til ny løsning.
ADR-0011 la opp til en migreringsbehandling der saksbehandler valgte
migreringsdato, fylte inn vilkår med kilde og begrunnelse og sendte til beslutter.
Flere måneder fra Infotrygd skulle tas over i en egen «Endre
migreringsdato»-behandling, og vanlige revurderinger kunne ikke gå før
migreringsdatoen.

Teamet vil heller gjøre det som familie-ef-sak:

- `MigreringService` migrerer med ett klikk. Systemet validerer periodene i
  Infotrygd, oppretter behandlingen, setter vilkårene til oppfylt, beregner,
  sjekker simuleringen og iverksetter uten vedtaksbrev og uten beslutter.
  Systemet står som både saksbehandler og beslutter
  (`IverksettingDtoMapper.tilDtoMaskineltBehandlet`).
- Tilkjent ytelse har `startdato`. En vanlig revurdering kan flytte den bakover
  (`BeregnYtelseSteg`).
- En revurdering kopierer vilkårene fra forrige behandling
  (`RevurderingService`).

Bare ticket 01 i den gamle spec-en er gjort: startmåned ble fjernet fra tilkjent
ytelse, og V46 dropper kolonnen.

## Beslutning

### Første migrering gjøres med ett klikk, uten beslutter

- Alle saksbehandlere kan migrere en person med løpende grunnstønad i Infotrygd.
  Saksbehandler ser en forhåndsvisning av perioden og beløpene, og trykker
  «Migrer».
- Backend gjør resten i én transaksjon:
  1. Leser Infotrygd på nytt og validerer periodene, som
     `InfotrygdPeriodeValideringService` i EF.
  2. Oppretter fagsak og første behandling med årsak `MIGRERING`.
  3. Oppretter alle vilkår som oppfylt og markerer dem som maskinelt opprettet.
     Sats og trygdetidsbrøk kommer fra Infotrygd.
  4. Lager vedtak og tilkjent ytelse fra perioden i Infotrygd.
  5. Kjører kontroll 1 og sjekker simuleringen.
  6. Iverksetter uten vedtaksbrev og uten beslutter.
- Systemet velger første måned. Den er den seneste av inneværende måned og
  periodens fom, som `maxOf(kjøremåned, fom)` i EF. Saksbehandler velger ingen
  dato.
- Har personen ingen periode fremover i tid, migreres siste måned av siste
  periode med beløp, som i EF.
- Migreringen stoppes hvis:
  - personen har en åpen sak i Infotrygd, utenom klage og anke
  - det er mer enn én aktiv periode fremover i tid, etter at like perioder som
    henger sammen, er slått sammen
  - fom ikke er første dag eller tom ikke er siste dag i måneden
  - beløpet er 0
  - siste utbetaling i Infotrygd er eldre enn datogrensen
  - fagsaken har en behandling som ikke er henlagt eller avslått
- Kontroll 1: utbetalingsbeløpet per måned er likt det Infotrygd utbetalte.
  Kontrollen sammenligner beløpet etter avrundingen i oppdrag (ADR-0011).
- Simuleringen må vise 0 kr i feilutbetaling og 0 kr i etterbetaling.
- Ingen av sperrene kan overstyres.

### Startmåned på tilkjent ytelse, som `startdato` i EF

- Tilkjent ytelse får tilbake `startmåned`: den tidligste av forrige behandlings
  startmåned og første andel.
- Ved opphør uten andeler er startmåned den tidligste av opphørsmåneden og
  forrige startmåned, som `beregnNyttStartdatoForRevurdering` i EF.
- I en migrert sak er startmåned den første måneden ny løsning eier
  utbetalingen.

### En vanlig revurdering kan ta over flere måneder fra Infotrygd

- En revurdering kan gi andeler før startmåned. Da flyttes startmåned bakover.
  Det finnes ingen egen behandling for å flytte den.
- Revurderingen har beslutter og vedtaksbrev som andre revurderinger.
- Det finnes ingen egen kontroll av månedene Infotrygd har utbetalt for.
  Saksbehandler og beslutter ser simuleringen.
- Oppdrag setter `opphørAlleKjederFra` lik ny startmåned når en revurdering i en
  migrert sak flytter startmåned bakover. Ellers er feltet `null`, også når en
  revurdering i en sak som ikke er migrert gir en andel før den første.
- Revurderingen kopierer vilkårene fra forrige behandling, også de som ble
  opprettet maskinelt i migreringen. Hvert kopierte vilkår viser hvilken
  behandling det først ble vurdert i, som `opphavsvilkår` i EF.

### Infotrygd får beskjed ved hver iverksetting

- Når migreringen iverksettes, sendes `vedtaksmelding` med
  `datoStartNyGrunnstønad` lik startmåned, gjennom den eksisterende
  feed-klienten.
- Revurderinger sender også `vedtaksmelding`, som i dag og som i EF. Flytter
  revurderingen startmåned bakover, får Infotrygd den nye startmåneden.
- En task sjekker etterpå at Infotrygd har sluttet å utbetale, som
  `SjekkMigrertStatusIInfotrygdTask` i EF. Den går til manuell oppfølging og gir
  varsel hvis Infotrygd fortsatt utbetaler.

### Sporbarhet og tilgang

- Begge endepunktene sjekker tilgang til personen med
  `TilgangService.validerTilgangTilFagsakPerson` før de leser Infotrygd
  (ADR-0009). Forhåndsvisningen auditlogges som lesing, migreringen som
  opprettelse.
- Behandlingen lagrer hvem som migrerte, når, og faktagrunnlaget fra Infotrygd
  som migreringen bygger på (ADR-0007).
- En lås per fagsakperson (`pg_advisory_xact_lock`) hindrer to migreringer av
  samme person samtidig. Validering skjer på nytt inne i låsen, og et nytt kall
  returnerer den eksisterende behandlingen.
- Migrering ligger bak en feature toggle i Unleash.

## Alternativer vurdert

### Migreringsbehandling med beslutter, som i ADR-0011

Saksbehandler fylte inn det som manglet i Infotrygd, og beslutter godkjente.
Forkastet fordi teamet vil gjøre det likt som EF, og fordi maskinell migrering i
batch senere må virke uten beslutter. Med beslutter kunne måneden også være
passert før godkjenningen, og behandlingen måtte beregnes på nytt.

### «Endre migreringsdato»-behandling og revurderingssperre, som i BA

Forkastet fordi teamet vil at en vanlig revurdering skal kunne ta over flere
måneder, som i EF. Det gir én behandling i stedet for to når saken både skal
flyttes bakover og endres.

### Sperre migrering rundt utbetalingskjøringen i Infotrygd

Hvis Infotrygd kjører utbetalingen for inneværende måned etter at vi har simulert,
men før `vedtaksmelding` er behandlet, utbetaler begge. Forkastet fordi EF ikke
har en slik sperre, og teamet godtar risikoen.

### Egen rolle for å migrere

Forkastet. Alle saksbehandlere kan migrere. Kontroll 1, simuleringen og
sporbarheten erstatter fireøyneprinsippet.

## Konsekvenser

### Positive

- Migrering tar ett klikk, og samme kode kan brukes i maskinell migrering.
- Spec-en blir mindre: ingen migreringsdato på behandlingen, ingen kilde,
  låsing eller begrunnelse per vilkår, ingen kontroll 2 og ingen
  revurderingssperre.
- Koden ligner på EF, så kode og erfaring derfra kan brukes direkte.

### Negative

- Bare saker der Infotrygd har én entydig periode, kan migreres. Andre saker må
  rettes i Infotrygd først.
- Vilkårene i en migrert sak er ikke vurdert av noen. Den første revurderingen
  kopierer dem som oppfylt.
- Ticket 01 må reverseres. V44 og V46 blir liggende, og en ny migrering legger
  kolonnen til igjen.
- En migrering kan ikke rulles tilbake etter at Infotrygd har sluttet å
  utbetale. Feil må rettes med en revurdering.

### Godtatt risiko

- Ingen beslutter kontrollerer migreringen. Én saksbehandler kan starte løpende
  utbetaling. Kontroll 1 og simuleringen binder beløpet til det Infotrygd
  utbetalte, og ingen kan overstyre dem.
- Inneværende måned kan bli utbetalt to ganger hvis Infotrygd kjører
  utbetalingen etter at vi har simulert.
- En revurdering som tar over måneder Infotrygd har utbetalt for, kan gi dobbel
  utbetaling hvis OS ikke motregner. Bare simuleringen og beslutter fanger det.
  Revurdering bakover i en migrert sak ligger bak en egen toggle til Økonomi
  har svart.

## Aksjonspunkter

- [ ] Økonomi bekrefter om OS motregner mellom Infotrygd og ny løsning når de
      bruker ulike fagområder og klassekoder. Må være avklart før migrering i
      prod og før revurdering bakover i en migrert sak slås på.
- [ ] Infotrygd bekrefter at `vedtaksmelding` med `datoStartNyGrunnstønad`
      stopper utbetalingen fra den måneden, også når det er inneværende måned.
- [ ] Sjekk hvordan ef-iverksett bruker `startdato` mot generatoren, før
      revurdering bakover bygges.
- [ ] Ta stilling til beslutter når overstyring av etterbetaling i migreringen
      kommer.
- [ ] Vurder en hard kontroll på måneder Infotrygd har utbetalt for, når Økonomi
      har svart.

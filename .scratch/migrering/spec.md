# Migrering fra Infotrygd

Status: ready-for-agent

Se [ADR-0011](../../docs/adr/ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md)
og begrepene under «Behandling» i [CONTEXT.md](../../CONTEXT.md).

## Problem

Løpende saker om grunnstønad ligger i Infotrygd. Ny løsning skal overta
utbetalingen, men kan i dag ikke ta imot en sak fra Infotrygd.

Tilkjent ytelse har i dag en startmåned på alle behandlinger, og en vanlig
revurdering kan flytte den bakover. Da kan ny løsning ta over måneder Infotrygd
har utbetalt for, uten at noen har kontrollert at beløpene stemmer.

En migrering er ikke en behandling i juridisk forstand, fordi ingenting endres
for stønadsmottaker. Hvis en migrering likevel endrer utbetalingen, har Nav
endret saken uten begrunnelse.

## Løsning

Saksbehandler oppretter en migreringsbehandling og velger migreringsdatoen: den
første måneden ny løsning eier utbetalingen. Vilkår og perioder flyttes over fra
Infotrygd. Saksbehandler fyller bare inn det som mangler eller er motstridende i
Infotrygd.

Behandlingen kan ikke sendes til beslutter før tre kontroller er oppfylt:

1. For månedene vi tar over, er utbetalingsbeløpet per måned likt det Infotrygd
   utbetalte.
2. Fra den gamle migreringsdatoen og fremover er utbetalingsbeløpet per måned
   likt forrige behandling. Gjelder bare «Endre migreringsdato»-behandlinger.
3. Simuleringen viser 0 kr i etterbetaling og feilutbetaling.

Avvik kan ikke overstyres. Behandlingen må henlegges.

Migreringsdatoen kan senere flyttes bakover i en «Endre migreringsdato»-behandling,
med de samme kontrollene. Vanlige revurderinger kan aldri gi andeler eller
vilkårsperioder før migreringsdatoen.

Startmåned på tilkjent ytelse fjernes. Saker som aldri har vært i Infotrygd, har
ingen migreringsdato.

## Brukerhistorier

### Migreringsbehandling

1. Som saksbehandler vil jeg opprette en migreringsbehandling for en person med
   løpende grunnstønad i Infotrygd, slik at ny løsning kan overta utbetalingen.
2. Som saksbehandler vil jeg velge migreringsdatoen selv, slik at jeg bestemmer
   hvilken måned ny løsning overtar fra.
3. Som saksbehandler vil jeg kunne velge en migreringsdato som Infotrygd allerede
   har utbetalt for, slik at ny løsning kan eie måneder tilbake i tid.
4. Som saksbehandler vil jeg få vilkårene fylt ut med data fra Infotrygd, slik at
   jeg ikke vurderer saken på nytt.
5. Som saksbehandler vil jeg få periodene med sats og trygdetidsbrøk fra
   Infotrygd, slik at beregningen gir det samme som Infotrygd utbetalte.
6. Som saksbehandler vil jeg se hvilke verdier som kommer fra Infotrygd og hvilke
   jeg må fylle inn, slik at jeg vet hva jeg har ansvar for.
7. Som saksbehandler vil jeg fylle inn vilkår eller perioder der Infotrygd mangler
   data, slik at saken kan migreres selv om dataene er ufullstendige.
8. Som saksbehandler vil jeg fylle inn felt som er motstridende i Infotrygd, slik
   at saken kan migreres når evalueringen ikke gir én verdi.
9. Som saksbehandler vil jeg måtte skrive en begrunnelse for hver verdi jeg fyller
   inn, slik at beslutter og senere saksbehandlere ser hvorfor.
10. Som saksbehandler vil jeg ikke kunne endre verdier som er hentet fra
    Infotrygd, slik at migreringen ikke blir en skjult revurdering.
11. Som saksbehandler vil jeg få beskjed om hvilke måneder utbetalingsbeløpet
    avviker fra det Infotrygd utbetalte, med begge beløpene, slik at jeg kan finne
    feilen.
12. Som saksbehandler vil jeg få beskjed når simuleringen viser etterbetaling
    eller feilutbetaling, slik at jeg vet at migreringen vil endre utbetalingen.
13. Som saksbehandler vil jeg ikke kunne sende behandlingen til beslutter når en
    kontroll feiler, slik at en migrering aldri endrer utbetalingen.
14. Som saksbehandler vil jeg kunne henlegge en migreringsbehandling med avvik,
    slik at saken kan rettes i Infotrygd eller migreres med en annen dato.
15. Som saksbehandler vil jeg slippe vedtaksbrevsteget i en migreringsbehandling,
    fordi ingenting endres for stønadsmottaker.

### Beslutter

16. Som beslutter vil jeg godkjenne migreringsbehandlinger før de iverksettes,
    slik at en feil migreringsdato ikke setter i gang opphør i OS uten kontroll.
17. Som beslutter vil jeg se migreringsdatoen, kilden til hver verdi og
    begrunnelsene, slik at jeg kan kontrollere det saksbehandler har fylt inn.
18. Som beslutter vil jeg kunne underkjenne en migreringsbehandling, slik at
    saksbehandler kan rette den.

### «Endre migreringsdato»-behandling

19. Som saksbehandler vil jeg opprette en «Endre migreringsdato»-behandling for en
    migrert sak, slik at ny løsning kan overta flere måneder fra Infotrygd.
20. Som saksbehandler vil jeg få feil hvis ny migreringsdato er lik eller senere
    enn den gamle, slik at migreringsdatoen bare flyttes bakover.
21. Som saksbehandler vil jeg få vilkår og perioder fra den gamle
    migreringsdatoen og fremover kopiert uendret fra forrige behandling, slik at
    jeg bare trenger å se på månedene vi tar over.
22. Som saksbehandler vil jeg ikke kunne endre det som er kopiert fra forrige
    behandling, slik at behandlingen ikke blir en revurdering.
23. Som saksbehandler vil jeg få vilkår og perioder for månedene vi tar over fra
    Infotrygd, på samme måte som i den første migreringen.
24. Som saksbehandler vil jeg få beskjed om hvilke måneder utbetalingsbeløpet
    avviker fra forrige behandling, slik at jeg ser at noe annet enn en overføring
    har skjedd.

### Revurdering i en migrert sak

25. Som saksbehandler vil jeg få beskjed i vilkårssteget når en vilkårsperiode
    starter før migreringsdatoen, slik at jeg retter det tidlig.
26. Som saksbehandler vil jeg ikke kunne sende en revurdering til beslutter når en
    andel starter før migreringsdatoen, slik at bare en «Endre
    migreringsdato»-behandling kan ta over flere måneder fra Infotrygd.
27. Som saksbehandler vil jeg få vite at måneder før migreringsdatoen må tas over
    i en «Endre migreringsdato»-behandling først, slik at jeg vet hva jeg skal
    gjøre.
28. Som saksbehandler vil jeg kunne gi andeler før den første andelen i en sak som
    aldri har vært i Infotrygd, slik at etterbetaling bakover virker som før.

### Stønadsmottaker og økonomi

29. Som stønadsmottaker vil jeg få det samme utbetalt etter migreringen som før,
    slik at flyttingen ikke merkes.
30. Som stønadsmottaker vil jeg ikke få noe utbetalt to ganger for samme måned.
31. Som Nav vil jeg at Infotrygd slutter å utbetale fra migreringsdatoen, slik at
    bare én løsning utbetaler for hver måned.
32. Som Nav vil jeg at OS opphører alle kjeder fra ny migreringsdato bare i en
    «Endre migreringsdato»-behandling, slik at andre behandlinger ikke sender
    opphør for måneder Infotrygd eier.

### Teamet

33. Som utvikler vil jeg at migreringsdatoen for saken er den tidligste blant
    iverksatte migreringsbehandlinger, slik at henlagte og åpne behandlinger aldri
    påvirker sperrer eller opphør.
34. Som utvikler vil jeg at reglene er rene funksjoner, slik at de kan testes uten
    Spring og database.
35. Som utvikler vil jeg at startmåned er fjernet fra tilkjent ytelse, slik at
    ingen bruker et begrep som ikke lenger gjelder.

## Beslutninger om implementasjon

### Startmåned fjernes

- Startmåned på tilkjent ytelse, funksjonen som utleder den og testene fjernes.
- Migreringen som la til kolonnen (V44), er ikke deployet. Den slettes i stedet for
  å følges av en ny migrering. Lokale databaser der V44 er kjørt, må settes opp på
  nytt.
- Migreringen med ekstern behandlings-ID (V43) beholdes. Oppdrag trenger den.

### Årsak og migreringsdato

- Årsak for behandling får to nye verdier: `MIGRERING` og `ENDRE_MIGRERINGSDATO`.
  En migreringsbehandling er en behandling med en av disse årsakene.
- Migreringsdatoen lagres sammen med årsaken til behandlingen, som `DATE` med
  første dag i måneden. Den er satt for migreringsbehandlinger og tom for andre.
- Gjeldende migreringsdato for en sak utledes av en ren funksjon. Den får alle
  migreringsbehandlingene i saken med status og migreringsdato, og returnerer den
  tidligste blant iverksatte. Henlagte og åpne telles ikke. For en saks
  migreringsbehandling under arbeid brukes behandlingens egen migreringsdato.
- En ren funksjon avviser ny migreringsdato som er lik eller senere enn gjeldende.
- Sperren mot to åpne behandlinger på samme fagsak finnes allerede og gjelder også
  migreringsbehandlinger.

### Data fra Infotrygd

- Vilkår, perioder og utbetalte beløp hentes fra `/stonader` i `apps/infotrygd`:
  delytelser, trygdetid og sats fra `t_gh`, og evalueringen med diagnose, sats,
  hjemmelbokstav og virkningstidspunkt.
- Hver vilkårsverdi og periode i en migreringsbehandling har en kilde: Infotrygd,
  forrige behandling eller saksbehandler. Verdier saksbehandler fyller inn, lagres
  med begrunnelse.
- Skjermbildet låser verdier med kilde Infotrygd eller forrige behandling, og krever
  begrunnelse for verdier saksbehandler fyller inn. Backend validerer ikke
  vilkårene i migreringsbehandlinger.
- Hvordan hvert av de fem vilkårene utledes fra Infotrygd-dataene, er ikke avklart.
  Se «Andre merknader».

### Kontrollene

- Alle tre kontrollene er rene funksjoner som får data inn og returnerer en liste
  med avvik per måned. Tom liste betyr at kontrollen er oppfylt.
- Kontrollene sammenligner utbetalingsbeløp, altså tilkjent ytelse etter
  avrundingsfunksjonen i oppdrag. De sammenligner aldri tilkjent ytelse med
  desimaler.
- Kontroll 1 får utbetalingsbeløp per måned fra beregningen og det Infotrygd
  utbetalte per måned, for månedene fra ny migreringsdato til måneden før gammel
  migreringsdato. I den første migreringen er det alle måneder fra migreringsdatoen
  der Infotrygd har utbetalt.
- Kontroll 2 får utbetalingsbeløp per måned fra beregningen og fra forrige
  iverksatte behandling, fra gammel migreringsdato og fremover.
- Kontroll 3 får summene for etterbetaling og feilutbetaling fra
  simuleringsresultatet. Grensen er 0 kr.
- Kontroll 1 og 2 kjøres når vedtak og beregning fullføres. Kontroll 3 kjøres når
  behandlingen sendes til beslutter. Alle tre kjøres på nytt ved sending til
  beslutter, i tilfelle data er endret.
- Feilene er funksjonelle feil med måned og begge beløpene, slik at skjermbildet
  kan vise dem.

### Revurderingssperren

- En ren funksjon får andelene og gjeldende migreringsdato, og returnerer andelene
  som starter før migreringsdatoen. Den brukes når en behandling i en migrert sak
  sendes til beslutter.
- En tilsvarende funksjon for vilkårsperioder brukes når vilkårssteget fullføres.
- Uten migreringsdato returnerer begge funksjonene ingen avvik.

### Opphør og Infotrygd

- En ren funksjon utleder `opphørAlleKjederFra` fra årsaken og migreringsdatoen:
  ny migreringsdato for `ENDRE_MIGRERINGSDATO`, ellers `null`. Funksjonen tas i
  bruk når oppdrag-delen kaller generatoren.
- Den første migreringen sender `vedtaksmelding` til Infotrygd med
  `datoStartNyGrunnstønad` lik migreringsdatoen, gjennom den eksisterende
  feed-klienten. En «Endre migreringsdato»-behandling sender ingen
  `vedtaksmelding`.

### Steg

- Migreringsbehandlinger hopper over vedtaksbrevsteget og distribuerer ikke brev.
- Totrinnskontrollen er som for andre behandlinger.

## Beslutninger om testing

- Gode tester sjekker oppførsel gjennom funksjonens inn- og utdata, ikke hvordan
  den er skrevet. Testnavnene beskriver regelen på norsk, som i resten av repoet.
- **Hovedgrensen er rene funksjoner uten Spring.** Den dekker:
  - gjeldende migreringsdato for saken, med henlagte og åpne behandlinger
  - at ny migreringsdato bare kan flyttes bakover, også innenfor samme måned
  - kontroll 1, 2 og 3, med avvik i én måned, flere måneder og ingen
  - revurderingssperren for andeler og vilkårsperioder, med og uten
    migreringsdato
  - utledning av `opphørAlleKjederFra`
- Kontroll 3 testes med et simuleringsresultat som lages i testen.
- **En repository-test** sjekker at migreringsdatoen lagres og leses riktig som
  `DATE`, og at den er tom for andre behandlinger.
- Tidligere eksempler: `BeregningUtilsTest` for rene funksjoner og
  `TilkjentYtelseRepositoryTest` for repository med Postgres i Testcontainers.
- Testene for startmåned i tilkjent ytelse fjernes eller skrives om.

## Utenfor omfang

- Oppdrag-delen av ADR-0011: generatoren, tabellen for utbetalingsandeler og
  sending av `opphørAlleKjederFra` til OS. Avrundingsfunksjonen er unntaket, se
  «Andre merknader».
- En test av hele migreringsflyten med Postgres og WireMock. Den blir en egen
  ticket når oppdrag-delen er på plass.
- Automatisk migrering og batch med `migreringspersoner`.
- Forslag til migreringsdato fra systemet.
- Overstyring av avvik.
- Backend-validering av vilkår i migreringsbehandlinger.
- Hjelpestønad. Spec-en gjelder grunnstønad, som resten av saksbehandlingen.

## Andre merknader

- **Blokkert av Økonomi.** Vi bruker andre fagområder og klassekoder enn Infotrygd,
  og vet ikke om OS motregner. Hvis ikke, stopper kontroll 3 alle migreringer med
  dato tilbake i tid. «Endre migreringsdato» og migrering tilbake i tid bygges
  ikke før Økonomi har svart. Migrering fra neste måned er ikke blokkert.
- **Vilkår fra Infotrygd må avklares av et menneske.** Hvilke felt i delytelsene,
  `t_gh` og evalueringen som gir medlemskap, diagnose, nødvendige ekstrautgifter,
  institusjon og trygdetid, er ikke bestemt. Dette blir en ticket med status
  `ready-for-human`.
- **Hva Infotrygd utbetalte, er usikkert.** Kontroll 1 trenger utbetalt beløp per
  måned. Hvilke delytelser som teller, og om regelen om `oppdrag_id` fra EF gjelder,
  er ikke bekreftet (ADR-0005).
- **Avrundingsfunksjonen.** Kontroll 1 og 2 trenger utbetalingsbeløp. Hvis
  avrundingsfunksjonen fra ADR-0011 ikke finnes når arbeidet starter, lages den
  her som en ren funksjon, uten resten av oppdrag-delen.
- **Infotrygd** må bekrefte at `vedtaksmelding` stopper utbetalingen, og at det
  ikke trengs en ny melding når migreringsdatoen flyttes bakover.
- **Feature toggle** er ikke diskutert. Migrering bør trolig ligge bak en toggle i
  Unleash til aksjonspunktene i ADR-0011 er avklart.

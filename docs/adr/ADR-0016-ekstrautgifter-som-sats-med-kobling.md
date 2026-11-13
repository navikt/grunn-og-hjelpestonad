# ADR-0016: Ekstrautgifter som sats med kobling (B), med vei til D

**Dato:** 2026-11-09
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad, med designer
**Erstatter delvis:** [ADR-0006](ADR-0006-fem-vilkaar-med-inngangsvilkaar-som-foerste-steg.md)
(punktet om nødvendige ekstrautgifter under «Steg 2 er nødvendige ekstrautgifter
og trygdetid») og merknaden 2026-11-06 i
[ADR-0014](ADR-0014-tilkjent-ytelse-fra-ekstrautgifter-og-trygdetid.md)

## Kontekst

ADR-0006 lot modellen for nødvendige ekstrautgifter stå åpen til teamet og fag
hadde valgt mellom fire varianter i
[ticket 15](../../.scratch/sats-og-trygdetid/issues/15-avklar-modellen-for-noedvendige-ekstrautgifter.md):

- **A. Sats direkte:** ja eller nei og sats per periode.
- **B. Sats med kobling:** som A, pluss hjemmelbokstaver og diagnoser per periode.
- **C. Utgiftsposter:** hver utgift med bokstav, diagnoser, beløp og periode.
  Systemet regner ut satsen og lagrer den ikke.
- **D. Utgiftsposter og vedtatt sats:** som C, og saksbehandler bekrefter eller
  overstyrer satsen systemet foreslår.

Designer har valgt B. Innsikten bak valget er foreløpig, og D kan bli aktuelt
når vi vet mer om hvordan saksbehandlerne jobber med beløpene.

## Beslutning

### Saksbehandler setter satsen og kobler den til bokstaver og diagnoser

Vilkåret har én rad per periode i `vilkar_ekstrautgifter` med vurdering, sats,
begrunnelse og datoer. Hver periode har en liste med ekstrautgifter. En
ekstrautgift har én hjemmelbokstav og én eller flere diagnoser, og en periode har
høyst én ekstrautgift per bokstav.

Spring Data JDBC støtter ikke nestede samlinger i et `Set`. Ekstrautgiftene
lagres derfor som én rad per bokstav og diagnose i
`vilkar_ekstrautgift_diagnose`. Domenet leser dem som
`Ekstrautgift(hjemmelbokstav, diagnosekoder)`, og bare `medEkstrautgifter`
skriver dem.

Endepunktene er `GET`, `POST` og `DELETE` på
`/api/behandling/{behandlingId}/vilkar/ekstrautgifter`. De krever rollen
saksbehandler og sjekker tilgang til behandlingen i hver metode.

### Reglene ved lagring

Reglene ligger i `EkstrautgifterRegler`, som ren kode etter ADR-0008:

- Ved `JA` kreves fra og med-dato, sats og minst én ekstrautgift. Ved `NEI`
  skal satsen være tom, og fra og med-dato er valgfri. Databasen håndhever
  sats og fra og med-dato ved `JA` med CHECK-constraints.
- Hver ekstrautgift har minst én diagnose, og ingen bokstav står to ganger.
- Bokstav d og e avvises, med mindre perioden slik den er lagret i databasen,
  allerede har bokstaven. Da kan en sak fra Infotrygd beholde dem.
- Hver valgt diagnose må være vurdert til `JA` i hele perioden, på samme
  behandling. Det gjelder også ved `NEI`, fordi saksbehandler bare kan velge
  blant diagnosene som er vurdert til `JA`.

Et `NEI` kan ha ekstrautgifter. Da dokumenterer de hva som ble vurdert og ikke
godkjent.

### Endringshistorikken viser sats, aldri hjemmelbokstaver eller diagnosekoder

Koblingen mellom en ekstrautgift og en diagnose er en helseopplysning etter
GDPR artikkel 9. Bokstavene c (førerhund), f (fordyret kosthold) og h
(servicehund) sier også noe om helsen til søker. Endringshistorikken viser
verken hjemmelbokstaver, diagnosekoder eller begrunnelse. Den viser sats når
perioden har en sats, ellers vurderingen (`JA` eller `NEI`), samt perioden.

`VilkårEkstrautgifterService.endringsdetalj` bygger teksten av faste felter.
Hooken `endringsdetalj` i `PeriodeMedBegrunnelseService` returnerer en `String`.
Det er feltene underklassene bruker, ikke typen, som holder fritekst og
diagnosekoder ute av historikken.

### Kopiering og migrering

En ny behandling kopierer bare perioder med `JA`, som for de andre vilkårene.
Infotrygd har én hjemmelbokstav, én eller to diagnoser og én sats per periode.
Migreringen skal derfor gi én periode med det Infotrygd har, uten å vurdere
noe på nytt (migrering/16). I en revurdering kan saksbehandler endre og slette
den som alle andre perioder. Servicen har ingen sperre for det.

### Utledningen får sats per periode

Inn i utledningen av tilkjent ytelse (ADR-0014) går vurdering og sats per
periode. Bokstaver og diagnoser påvirker ikke beløpet. De fire stedene merknaden
i ADR-0014 pekte på, gjelder som skrevet: `JA`/`NEI` og sats per periode, hull
betyr «nei», minst én vurdert periode og kopiering av `JA`-rader.

### Veien til D

Tabellen har samme form som den vedtatte satsen i D. En overgang til D:

- legger kolonnen `foreslatt_sats` til `vilkar_ekstrautgifter`
- legger en tabell for utgiftsposter med beløp ved siden av
- flytter ingen data

Periodene fra B blir «tidligere vedtatt sats», på samme måte som satsen fra
Infotrygd i en migrert sak. En ekstrautgift per bokstav tilsvarer en utgiftspost
uten beløp, så B-perioder kan vises i D uten å regnes om.

## Alternativer vurdert

### A. Sats direkte

Forkastet. Bokstav og diagnose står bare i fritekst, så brev og statistikk kan
ikke bruke dem. Migreringen fra Infotrygd ville miste bokstaven og diagnosene.

### C. Utgiftsposter

Forkastet. Uten lagret sats kan nye satsbeløp eller en endret utgift gi en annen
sats i perioder ingen har vurdert på nytt. Det bryter punkt 2 og 5 under
«Avklart» i ticket 15.

### D. Utgiftsposter og vedtatt sats

Utsatt. D lar systemet foreslå satsen og gir sporbare beløp, men krever mer
arbeid og svar fra fag på spørsmål 5 til 9 i ticket 15. B er bygget slik at
overgangen er billig.

### Bokstavene som liste på raden

Forkastet. Bokstav og diagnoser hører sammen, og en egen tabell gjør det mulig
å lese hvilke diagnoser som gir hvilken utgift. Det er også formen en
utgiftspost får i D.

## Konsekvenser

- Saksbehandler må vurdere diagnose i steg 1 før ekstrautgiftene kan
  registreres.
- Steg 1 kan endres etter at ekstrautgiftene er lagret. Fullføringen av steg 2
  (ticket 10) må derfor sjekke at diagnosene fortsatt er vurdert til `JA` i hele
  perioden, også etter kopiering.
- Frontend markerer perioder utenfor periodene med oppfylte inngangsvilkår. Den
  rene funksjonen kan brukes av trygdetid (ticket 08).
- Beløpene for hver ekstrautgift lagres ikke, så ingen kan sjekke satsen mot
  utgiftene i ettertid. Regnestykket står i begrunnelsen.

## Aksjonspunkter

- [ ] Designer og fag bekrefter B etter mer innsikt, eller vi går over til D.
- [ ] Fullføringen av steg 2 sjekker dekningen av diagnoser på nytt (ticket 10).

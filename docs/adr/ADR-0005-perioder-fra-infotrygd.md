# ADR-0005: Perioder fra Infotrygd

**Dato:** 2026-09-29
**Status:** Godkjent
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`apps/infotrygd` er forket fra
[familie-ef-infotrygd-replika](https://github.com/navikt/familie-ef-infotrygd-replika),
appen til enslig forsørger (heretter EF-appen). EF-appen har `POST /api/perioder`
og `POST /api/perioder/sammenslatte`, som
gir vedtaksperioder for overgangsstønad, barnetilsyn og skolepenger. Endepunktet
for barnetilsyn og skolepenger ble fjernet fra forken fordi stønadene ikke
gjelder oss. Da fantes det ikke lenger noe endepunkt som gir periodene for
grunn- og hjelpestønad.

EF-appen henter beløp, aktivitet og inntekt fra `t_ef`, `t_beregn_grl` og
`t_rolle`. De tabellene gjelder enslig forsørger, og vi replikerer dem ikke. For
grunn- og hjelpestønad ligger beløp og periode i `t_delytelse`, og trygdetid og
sats i `t_gh`. Et vedtak kan ha flere delytelser, og vedtak uten sluttdato er
løpende, fordi stønaden ofte varer livet ut.

Vi vil holde oss så nær EF-appen som mulig, slik at det er lett å sammenligne
med den og hente over rettelser.

## Beslutning

Vi lager `POST /api/infotrygd/perioder` med samme oppbygning som EF-appen
(`PeriodeController`, `PeriodeService` og `PeriodeRepository`), men tilpasset
dataene våre:

1. **Én periode per delytelse.** Hver rad i `t_delytelse` gir én periode, med
   informasjon om vedtaket og stønaden den hører til. `stønadFom`, `stønadTom` og
   `beløp` kommer fra delytelsen. `innvilgetFom` og `innvilgetTom` kommer fra
   vedtaket.
2. **Alle registreringer i `t_gh`.** Et vedtak kan ha flere rader i `t_gh`
   (primærnøkkel `vedtak_id` og `tidspunkt_reg`). Vi vet ikke om den nyeste
   gjelder, eller om raden hører sammen med delytelsen med samme
   `tidspunkt_reg`. Derfor får hver periode alle radene for vedtaket i
   `trygdetidOgSats`, nyeste først, og konsumenten avgjør hvilken som gjelder.
   De hentes i en egen spørring, slik EF-appen henter barn i
   `hentBarnForPerioder`.
3. **Gruppert per stønadstype.** Svaret er `{ grunnstønad, hjelpestønad }`.
4. **Løpende vedtak tas med.** Vedtak uten `dato_innv_tom` er løpende. Vedtak
   der `dato_innv_tom` er før eller lik `dato_innv_fom`, filtreres bort, som i
   EF-appen.
5. **Koder som tekst.** Endringskode, sakstype, resultat og delytelsestyper
   returneres som tekst. EF-appen bruker enum-er som kaster feil på ukjente
   koder, og vi kjenner ikke alle kodene for grunn- og hjelpestønad ennå.
6. **Ingen sammenslåing enn så lenge.** Vi lager ikke `/perioder/sammenslatte`.

Dette tar vi med uendret fra EF-appen:

- Et vedtak med flere endringskoder i `t_endring` gir én periode per kode.
- Perioder uten `oppdrag_id` filtreres bort, med mindre beløpet er 0 kr.
- Periodene sorteres med nyeste `stønadFom` først.

## Alternativer vurdert

### Alternativ A: Én periode per vedtak

- **Fordeler:** Samme granularitet som EF-appen, og sammenslåingen derfra kan
  brukes nesten uendret.
- **Ulemper:** Et vedtak kan ha flere delytelser med ulike beløp og perioder.
  Da må delytelsene enten legges i en liste inne i perioden, eller slås sammen
  til én verdi. Da mister vi detaljer eller får en mer sammensatt modell.

### Alternativ B: Én periode per delytelse (valgt)

- **Fordeler:** Viser beløp og periode slik de ligger i Infotrygd, uten å tolke
  dataene.
- **Ulemper:** Sammenslåingen i EF-appen passer ikke direkte, fordi delytelser
  av ulik type kan overlappe.

### Alternativ C: Ta med sammenslåtte perioder nå

- **Fordeler:** Samme endepunkter som EF-appen.
- **Ulemper:** `InfotrygdPeriodeUtil` forutsetter at `tom` alltid finnes og at
  én periode tilsvarer ett vedtak. Den må skrives om til å tåle løpende vedtak
  og flere delytelser per vedtak. Hvordan vedtak skal overskrive hverandre for
  grunn- og hjelpestønad, er ikke avklart.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

- Svaret inneholder personidenter og stønadsopplysninger.
- `apps/infotrygd` krever én av rollene i `Rolle`, men gjør ingen tilgangssjekk
  per person. Tilgangssjekk mot tilgangsmaskinen og auditlogging må gjøres i
  `sak` før data vises for saksbehandler.

### Plattform

Endringen krever ingen nye Nais-ressurser. Endepunktet leser bare fra tabeller
som allerede replikeres.

### Team-påvirkning

`InfotrygdClient` i `sak` kaller allerede `/api/infotrygd/perioder`, men sender
`{ personident }` og forventer barnetilsyn og skolepenger i svaret. Klienten, og
siden «Infotrygd historikk» i frontend, må oppdateres før de kan bruke det nye
endepunktet.

## Konsekvenser

### Positive

- Periodene for grunn- og hjelpestønad kan hentes fra Infotrygd.
- Oppbygningen likner EF-appen, så det er enkelt å sammenligne.
- Ukjente koder gir ikke feil.

### Negative

- Konsumentene får rådata og må selv tolke overlappende perioder og
  endringskoder.
- Et vedtak med flere endringskoder gir duplikate perioder.

### Risiko

Vi kjenner ikke alle kodene og verdiene i `t_delytelse` og `t_gh` for grunn- og
hjelpestønad. Regelen om `oppdrag_id` er hentet fra enslig forsørger og er ikke
bekreftet for våre stønader.

## Aksjonspunkter

- [x] Godkjenn eller forkast ADR-en i teamet.
- [ ] Kartlegg kodene i `t_endring`, `t_vedtak.type_sak` og `t_delytelse` i dev.
- [ ] Bekreft at regelen om `oppdrag_id` gjelder for grunn- og hjelpestønad.
- [ ] Finn ut hvilken registrering i `t_gh` som gjelder for en delytelse.
- [ ] Oppdater `InfotrygdClient` i `sak` og siden «Infotrygd historikk» i
      frontend.
- [ ] Vurder sammenslåtte perioder når vi vet hvordan vedtak overskriver
      hverandre.

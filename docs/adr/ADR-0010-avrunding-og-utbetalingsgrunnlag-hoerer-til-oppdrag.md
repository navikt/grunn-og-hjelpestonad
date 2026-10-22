# ADR-0010: Avrunding og utbetalingsgrunnlag hører til oppdrag

**Dato:** 2026-10-22
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

### Startdato og migrert fra og med følger EF

- `tilkjent_ytelse` får `startmåned` på alle behandlinger, som `startdato` i EF.
  Den lagres som `YearMonth`-tekst (`YYYY-MM`), som andre måneder i databasen.
  I en migreringsbehandling er startmåneden lik migrert fra og med: den første
  måneden ny løsning eier utbetalingen.
- Når ny startmåned er tidligere enn forrige, setter oppdrag `opphørAlleKjederFra`
  lik ny startmåned. Det gjelder også når behandlingen ikke har andeler.
- Årsaken heter `ENDRE_MIGRERT_FRA_OG_MED`, ikke `ENDRE_MIGRERINGSDATO` som i BA.
  Migrert fra og med kan bare flyttes bakover.

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

### Migreringsdato med BA-semantikk

BA lagrer en dato og utbetaler fra måneden etter, så opphøret starter en måned
etter migreringsdatoen. Forkastet fordi EF-semantikken lar startmåned, migrert fra
og med og `datoStartNyGrunnstønad` i Infotrygd-feeden være samme måned.

## Konsekvenser

### Positive

- Avrundingsregelen kan endres uten at tilkjent ytelse eller beregningen endres.
- Konsistensavstemmingen leser det OS faktisk har fått, med avrundede beløp.
- Saken har ingen avhengighet til generatoren eller kontraktene fra
  `familie-oppdrag`.

### Negative

- Vi avviker fra både BA og EF. Kode fra referanseappene må tilpasses før den
  kan brukes.
- Tilkjent ytelse og utbetalingsbeløp kan avvike med inntil 50 øre per måned.
  Skjermbilder som viser beløp, må si hvilket av dem de viser.

### Risiko

- Hvis migrert fra og med egentlig skal tolkes som i BA, blir opphøret én måned
  feil. Før prod er det en endring i én funksjon med tester. Etter første
  migrering i prod må lagrede datoer og kjeder i OS rettes for hver sak.

## Aksjonspunkter

- [ ] Fagsiden bekrefter at migrert fra og med er den første måneden ny løsning
      utbetaler (EF), ikke måneden før (BA). Må være avklart før første migrering
      i prod.
- [ ] Fagsiden bestemmer avrundingsregelen før første utbetaling i prod.
- [ ] Økonomi gir fagområde og klassekode for prod. Dev bruker `IT28` og
      `GHGSORMG`.
- [ ] Infotrygd bekrefter at `vedtaksmelding` med `datoStartNyGrunnstønad` stopper
      utbetalingen i Infotrygd.

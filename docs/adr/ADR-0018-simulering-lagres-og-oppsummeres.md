# ADR-0018: Simuleringen lagres uendret og oppsummeres som i ba-sak

**Dato:** 2026-11-19
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad
**Bygger på:** [ADR-0008](ADR-0008-deterministisk-logikk-utenfor-spring.md),
[ADR-0011](ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md),
[ADR-0017](ADR-0017-backend-eier-naavaerende-steg.md)

## Kontekst

Simuleringsfanen viste posteringene fra Oppdrag i én flat tabell, én rad per
postering. Hver måned har flere posteringer, så en simulering over flere år ble
hundrevis av rader. Saksbehandler så ingen sum for feilutbetaling eller
etterbetaling, og resultatet ble ikke lagret. Beslutter kunne derfor ikke se hva
saksbehandler så.

ba-sak og ef-sak viser én verdi per måned (nytt beløp, tidligere utbetalt og
resultat) og en oppsummering med feilutbetaling, etterbetaling og neste
utbetaling. ef-sak får oppsummeringen ferdig fra familie-ef-iverksett. ba-sak
kaller familie-oppdrag-backend direkte, som oss, og regner den ut selv i
`SimuleringUtil.kt`.

## Beslutning

### Svaret fra Oppdrag lagres uendret

`data` fra `POST /api/simulering/v1` lagres som `JSONB` i tabellen `simulering`
(V31), én rad per behandling. En ny simulering erstatter den forrige. Statusen er
`SIMULERT`, `INGEN_ENDRING` eller `FEILET`. `opprettet_tid` er tidspunktet
simuleringen ble hentet, og utregningen bruker datoen.

Vi lagrer svaret som det er, også `mottakerNummer`, som er fødselsnummeret. Et
uendret svar er enklere å feilsøke mot Oppdrag, og fødselsnummeret ligger
allerede i databasen. Når Oppdrag svarer med `SUKSESS` og et `data`-felt som ikke
er `null`, lagrer vi `data` med status `SIMULERT` uten å oppsummere det først.
Hvis Oppdrag-kallet feiler, svarkroppen er tom, statusen ikke er `SUKSESS` eller
`data` mangler eller er `null`, lagrer vi `FEILET` uten `respons`.

### Vi simulerer etter ny tilkjent ytelse og når saksbehandler ber om det

- Når sats og trygdetid er fullført, og når inngangsvilkår er fullført med
  vedtak og brev som neste steg. Controlleren simulerer etter at transaksjonen er
  committet, i samme request og med saksbehandlerens OBO-token. Feiler kallet,
  lagres `FEILET`, og steget er likevel fullført.
- `POST /api/oppdrag/simulering/{behandlingId}` krever rollen saksbehandler,
  ansvarlig saksbehandler og `validerKanEndreSteg(VEDTAK_OG_BREV)`.
- `GET /api/oppdrag/simulering/{behandlingId}` leser den lagrede simuleringen og
  kaller ikke Oppdrag. Saksbehandler, beslutter og lesetilgang kan hente den.
  Vi prøver å oppsummere en `SIMULERT` rad ved hver henting. Hvis det feiler,
  svarer API-et `FEILET` uten å endre raden. Det manuelle POST-kallet leser raden
  rett etter lagring og gir samme svar. Vi prøver ikke å oppsummere rader som
  allerede er lagret som `FEILET`.
- Simuleringen slettes i samme transaksjon som tilkjent ytelse, i
  `TilkjentYtelseService.slettTilkjentYtelseHvisFinnes`.

En task ville kjørt uten innlogget saksbehandler og måtte brukt client
credentials. Det er en endring i auth, så vi valgte kallet i requesten. Vi
simulerer ikke inne i transaksjonen, fordi den da holder en databasetilkobling
mens Oppdrag svarer.

### Utregningen er oversatt fra ba-sak

`SimuleringOppsummerer` er en ren funksjon etter ADR-0008. Den grupperer
posteringene per `fom` og regner ut nytt beløp, tidligere utbetalt, resultat,
feilutbetaling, etterbetaling og manuell postering per måned, med samme regler
som `SimuleringUtil.kt` i ba-sak. Testene er oversatt fra `SimuleringUtilTest.kt`
med samme forventede beløp.

- Bare `YTELSE` og `FEILUTBETALING` gir beløp.
  Måneder uten slike posteringer tas ikke med, som i ba-sak. Filteret gjelder
  også tidligere lagrede simuleringer,
  fordi oppsummeringen regnes ut ved hver henting. Råsvaret fra Oppdrag er uendret.
- Mottakertype, posteringstype og betalingstype gjøres om til enum-ene
  `Mottakertype`, `Posteringstype` og `Betalingstype`, som speiler
  familie-kontrakter. Et navn vi ikke kjenner på disse gir `FEILET` uten
  oppsummering: en ny posteringstype kan gi beløp vi ellers ville oversett.
  `FagOmrådeKode` inneholder bare kodene vi bruker for grunn- og hjelpestønad.
  Manglende eller tom fagområdekode gir `FEILET`, mens andre verdier (for
  eksempel andre ytelser fra Oppdrag) legges i `ukjenteFagområdekoder` som tekst
  og vises som varsel i frontend.
- Alle posteringer som gir beløp i en måned, skal ha samme `tom` og forfallsdato.
  ba-sak antar det og tar verdiene fra `posteringListe[0]`. Vi sjekker det, og
  er de ulike, svarer API-et `FEILET` uten oppsummering.
- Etterbetaling teller bare posteringer som har forfalt den dagen simuleringen
  ble hentet.
- Neste utbetaling er den første måneden som ikke er utbetalt ennå og ikke har
  feilutbetaling. Summene for feilutbetaling og etterbetaling gjelder månedene
  før den.

### Like måneder slås sammen

`SimuleringsperiodeSammenslåer` slår sammen påfølgende måneder med
familie-tidslinje når alle de seks beløpene er like. Et endret beløp starter en
ny periode, og neste utbetaling står alltid alene. Et opphold mellom simulerte
måneder skiller periodene, også når beløpene på hver side er like. Oppholdet får
ingen egen rad. Hver periode har beløpet per måned og summen for perioden.
Ti år uten endringer blir én rad.

`perioder` slutter med neste utbetaling. Månedene etter er med i svaret fra
Oppdrag, men vises ikke, som i ba-sak.

### Manuelle posteringer

Manuelle posteringer på grunn- og hjelpestønad har kodene MIT28 og MGHTB.
familie-oppdrag sender navnet på `FagOmrådeKode` i familie-kontrakter, ikke
koden. `MANUELLE_FAGOMRÅDEKODER` har derfor navnene fra kontraktene:
`GRUNNSTØNAD_INFOTRYGD_MANUELT` og
`GRUNN_OG_HJELPESTØNAD_TILBAKEKREVING_MANUELT`. MIT29
(`HJELPESTØNAD_INFOTRYGD_MANUELT`) er bevisst ikke med. Perioder med manuell
postering får en tagg i tabellen. Har en periode både manuell postering og et
resultat som ikke er 0, viser frontend en advarsel om å sende oppgave til NØS,
som i ba-sak. En postering med en annen kode enn disse,
`GRUNNSTØNAD_INFOTRYGD` (IT28) og `HJELPESTØNAD_INFOTRYGD` (IT29), står i
`ukjenteFagområdekoder`, og frontend viser et varsel.

#### Finne simuleringer med manuelle posteringer

Spørringen under finner alle lagrede simuleringer der svaret fra Oppdrag har
minst én postering med en manuell fagområdekode, uansett posteringstype og
status. Den kan derfor gi treff som ikke har tagg eller advarsel i
skjermbildet, for eksempel når den manuelle posteringen bare er en `JUSTERING`.

`respons` inneholder fødselsnummeret i `mottakerNummer`. Spørringen henter bare
ID-er, koder og antall.

```sql
SELECT s.behandling_id,
       b.fagsak_id,
       s.status,
       s.opprettet_tid,
       array_agg(DISTINCT postering ->> 'fagOmrådeKode') AS manuelle_fagomradekoder,
       count(*)                                           AS antall_manuelle_posteringer
FROM simulering s
         JOIN behandling b ON b.id = s.behandling_id
         CROSS JOIN LATERAL jsonb_array_elements(s.respons -> 'simuleringMottaker') AS mottaker
         CROSS JOIN LATERAL jsonb_array_elements(mottaker -> 'simulertPostering') AS postering
WHERE postering ->> 'fagOmrådeKode' IN ('GRUNNSTØNAD_INFOTRYGD_MANUELT', 'GRUNN_OG_HJELPESTØNAD_TILBAKEKREVING_MANUELT')
GROUP BY s.behandling_id, b.fagsak_id, s.status, s.opprettet_tid
ORDER BY s.opprettet_tid DESC;
```

Kodene i `IN (...)` må stemme med `MANUELLE_FAGOMRÅDEKODER`.

## Alternativer vurdert

### Lagre bare oppsummeringen

Mindre data og ingen fødselsnummer. Forkastet: vi mister det Oppdrag svarte og
kan ikke regne ut på nytt når reglene endres.

### Fjerne `mottakerNummer` før lagring

Dataminimering, og utregningen trenger ikke feltet. Forkastet fordi svaret da
ikke lenger er uendret, og fødselsnummeret finnes i databasen fra før.

### Simulere på nytt ved hver henting, som ef-sak

Alltid ferske tall. Forkastet: hver visning av fanen kaller Oppdrag, og
beslutter ville sett andre tall enn saksbehandler.

### Måneder som kolonner med årsvelger, som ba-sak og ef-sak

Én kolonne per måned og ett år om gangen. Forkastet fordi sammenslåing gir færre
rader uten at saksbehandler må bla mellom år.

## Konsekvenser

- Fullføring av sats og trygdetid tar lengre tid, fordi requesten venter på
  Oppdrag. I verste fall venter den til timeouten i `OppdragClient`.
- Beslutter kan ikke simulere på nytt og ser tidspunktet saksbehandler simulerte.
  Har en forfallsdato passert siden, kan etterbetalingen være for lav.
- Endringer på fagsaken etter simuleringen, for eksempel at en annen behandling
  blir iverksatt, gir ikke ny simulering før tilkjent ytelse regnes ut på nytt
  eller saksbehandler trykker på knappen.
- Telleren `oppdrag_simulering` har labelene `resultat` (`ok`, `feilet`,
  `ingen_endring`) og `utloser` (`knapp`, `tilkjent_ytelse`). `ok` betyr at vi
  godtok og lagret svaret. Oppsummeringsfeil telles ikke, men logges ved lesing.

### Risiko

familie-oppdrag-backend leser fagområdekoden med `FagOmrådeKode.fraKode` og
kaster for koder den ikke kjenner (se `tmp/todo-it28-oppdrag.md`). En manuell
postering med en ukjent kode gir derfor trolig `FEILET`, ikke et varsel om ukjent
kode. Det er ikke verifisert.

## Nav-spesifikke vurderinger

### Sikkerhet og personvern

`respons` inneholder fødselsnummer og ytelsesbeløp. Vi logger aldri `respons`,
og ved feil logger vi bare behandlings-ID, utløser og feiltypen, ikke meldingen
fra RestClient. API-svaret har ikke `mottakerNummer`. Lagringen må føres i ROS og
behandlingsoversikten. Simuleringen slettes sammen med tilkjent ytelse, ellers
følger den behandlingen.

## Aksjonspunkter

- [ ] Godkjenn eller forkast ADR-en i teamet.
- [x] Få MIT28, MIT29 og MGHTB inn i `FagOmrådeKode` i familie-kontrakter.
- [ ] Oppgrader familie-kontrakter i familie-oppdrag-backend, så den kjenner
      igjen kodene.
- [ ] Før lagringen av simuleringen i ROS og behandlingsoversikten.
- [ ] Sjekk hva familie-oppdrag-backend gjør med en manuell postering på `IT28`.
- [ ] Vurder om manuelle posteringer skal stoppe migrering, slik de stopper
      automatisk beslutning i ba-sak.

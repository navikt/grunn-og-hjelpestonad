# Infotrygd

App som henter ut data fra Infotrygd for grunn- og hjelpestønad.

Denne appen er én av tre i monorepoet. Se [rot-README](../../README.md) for oversikt.
**Alle kommandoer under kjøres fra `apps/infotrygd`.**

## Bygg og test

```bash
cd apps/infotrygd
mvn verify --settings .m2/maven-settings.xml
```

`GITHUB_USERNAME` og `GITHUB_TOKEN` må være satt for å hente `no.nav`-pakker fra
GitHub Packages.

## Formatering

```bash
mvn antrun:run@ktlint --settings .m2/maven-settings.xml
```

## Swagger

- https://grunn-og-hjelp-infotrygd.intern.dev.nav.no/swagger-ui/index.html
- Trykk "Authorize" og logg inn med Azure AD (la `client_secret` stå tom)

## Datakilde

Appen er forket fra
[familie-ef-infotrygd-replika](https://github.com/navikt/familie-ef-infotrygd-replika),
appen til enslig forsørger (heretter EF-appen). Som EF-appen leser den ikke
Oracle direkte. Tabellene fra Infotrygd replikeres fra
`grunn-og-hjelp-exodus` til en egen Postgres-database (Cloud SQL) av en planlagt
jobb (`ExodusScheduler`, cron i `exodus.scheduler-cron`). Hvilke tabeller som
replikeres og primærnøklene deres står i
[`ExodusTabell`](src/main/kotlin/no/nav/grunn/og/hjelpestonad/infotrygd/exodus/ExodusTabell.kt),
og skjemaet står i
[`V2__replikerte_infotrygd_tabeller.sql`](src/main/resources/db/migration/V2__replikerte_infotrygd_tabeller.sql).

### Datamodell

| Tabell | Innhold |
|--------|---------|
| `t_lopenr_fnr` | Kobler `person_lopenr` til fnr (`personnr`) |
| `t_stonad` | Stønaden, med `kode_rutine`, opphørsdato og `oppdrag_id` |
| `t_vedtak` | Vedtakene på en stønad, med innvilget periode (`dato_innv_fom`/`dato_innv_tom`) |
| `t_endring` | Endringskoder per vedtak (F, O, AN, UA …). Ett vedtak kan ha flere. |
| `t_delytelse` | Beløp per periode for et vedtak, med type delytelse, sats og utbetaling |
| `t_gh` | Trygdetid og sats per vedtak, med én rad per registrering |
| `sa_*`, `ev_*` | Saker, statuser, hendelser og vedtaksdata fra saksbehandlingsdelen (DL1) |

`kode_rutine` er `GS` for grunnstønad og `HS` for hjelpestønad
([`Stønadstype`](src/main/kotlin/no/nav/grunn/og/hjelpestonad/infotrygd/stønad/Stønadstype.kt)).

Systemskissen for GH i Infotrygd og et utdrag av ER-diagrammet for vedtak og
stønad står i [docs/gh-i-infotrygd.md](docs/gh-i-infotrygd.md).

### Koblinger mellom tabellene

- **Vedtak og sak:** `t_vedtak.saksblokk` og `t_vedtak.saksnr` tilsvarer
  `sa_sak_10.s05_saksblokk` og `sa_sak_10.s10_saksnr`. `s10_saksnr` er tekst og
  kan ha ledende null, mens `t_vedtak.saksnr` er tall.
- **Evaluering og sak:** `ev_valg_05.ev05_sak` er saksblokk fulgt av
  saksnummer, altså `saksblokk` + `saksnr` fra `t_vedtak`, eller
  `s05_saksblokk` + `s10_saksnr` fra `sa_sak_10`. Opplysningen kommer fra en
  fagperson i Infotrygd. Lengde og ledende null i `ev05_sak` er ikke sjekket
  mot dataene ennå.
- **Evalueringsdata og evaluering:** `ev_data_10` kobles til `ev_valg_05` bare
  på `ev01_personkey` og `ev05_aar_mnd_seq`. Tabellen har ikke `ev05_sak` eller
  `ev05_valg`, så en rad kan ikke knyttes til én sak eller stønadstype. Se
  [Evaluering](#evaluering).

### Formater

- **Fnr:** `t_lopenr_fnr.personnr` er vanlig fnr (DDMMÅÅPPPPP). `f_nr` i `sa_*`
  og `ev_*` er reversert (ÅÅMMDDPPPPP), og `s01_personkey` er `tk_nr` fulgt av
  reversert fnr. Bruk `reverserFnr()` før oppslag.
- **Datoer:** Tabellene med prefiks `t_` har vanlige datoer. I `sa_*` og `ev_*`
  er datoer lagret som tall, og formatet varierer per kolonne. Se KDoc i
  [`InfotrygdFormat.kt`](src/main/kotlin/no/nav/grunn/og/hjelpestonad/infotrygd/util/InfotrygdFormat.kt).
- **Måned i `ev_*`:** `ev05_aar_mnd_seq` i `ev_valg_05` og `ev_data_10` er
  `999999 − ÅÅÅÅMM`, lagret som tekst. Oktober 2025 er `797489`. Høyeste verdi er
  eldste måned, så `max` gir første registrering og `min` gir siste.
- **Løpende vedtak:** Et vedtak uten `dato_opphor` og `dato_innv_tom` regnes som
  løpende, siden stønaden ofte varer livet ut. Dette er et bevisst avvik fra
  EF-appen.

## Endepunkter

| Endepunkt | Beskrivelse |
|-----------|-------------|
| `POST /api/infotrygd/perioder` | Perioder gruppert i `grunnstønad` og `hjelpestønad`, med siste evaluering for saken per vedtak |
| `POST /api/infotrygd/stonad/eksisterer` | Om personene har vedtak eller saker, og om vedtakene er løpende |
| `GET /api/infotrygd/stonad/migreringspersoner` | Personer med løpende stønad fra neste måned |
| `POST /api/infotrygd/saker/finn` | Saker fra `sa_sak_10` |
| `GET /api/infotrygd/tabeller/*` | Kolonner og antall rader i replikatabellene |

### Kontrakt mot sak

[`openapi.json`](openapi.json) er kontrakten mellom infotrygd og sak. Sak
genererer klientmodellene sine fra filen (pakken
`no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt`) med
`openapi-generator-maven-plugin`. `OpenApiKontraktTest` skriver filen fra
`/v3/api-docs` og feiler når den var utdatert. Endrer du API-et, kjør testene og
commit den oppdaterte filen.

### Perioder

`/perioder` har samme lagdeling (`PeriodeController`, `PeriodeService` og
`PeriodeRepository`), forespørsel, joins, filtre og sortering som i EF-appen.
Bakgrunnen står i [ADR-0005](../../docs/adr/ADR-0005-perioder-fra-infotrygd.md).
Dette er annerledes:

- Én periode per rad i `t_delytelse`. `stønadFom`, `stønadTom` og `beløp` kommer
  fra delytelsen, og `innvilgetFom` og `innvilgetTom` fra vedtaket.
- `trygdetidOgSats` inneholder alle registreringer i `t_gh` for vedtaket, nyeste
  først. Det er ikke avklart hvilken som gjelder for en delytelse, så det
  avgjør konsumenten.
- Vedtak uten `dato_innv_tom` er løpende og tas med.
- Koder returneres som tekst, ikke enum, så ukjente koder ikke gir feil.
- Perioder slås ikke sammen. EF-appen har `/perioder/sammenslatte`, men det har
  ikke vi.

Som i EF-appen gir et vedtak med flere endringskoder én periode per kode, og
perioder uten `oppdrag_id` filtreres bort, med mindre beløpet er 0 kr.

### Evaluering

Evalueringen er feltet `evaluering` på hver periode i `/perioder`. Alle periodene i et vedtak får samme evaluering. Vedtak uten evaluering får `null`.

Vedtaket kobles til evalueringen i fire steg:

1. `t_vedtak` kobles til GH-saken i `sa_sak_10` på personen (`f_nr` er fnr reversert), `s10_valg` lik `kode_rutine`, `s05_saksblokk` lik `saksblokk` og `s10_saksnr` lik `saksnr`. `s10_saksnr` sammenlignes som tall med `t_vedtak.saksnr`, så `0123` treffer `123`. Et saksnummer som ikke er et tall, gir feil for hele kallet.
2. Saken kobles til `ev_valg_05` på `ev05_sak`, som er `s05_saksblokk` og `s10_saksnr` satt sammen. Bare rader med `ev05_kap = 'GH'` og `ev05_tekst_kode = 'EVGH'` brukes.
3. Saken får evalueringen fra siste måned, altså den laveste `ev05_aar_mnd_seq`. Eldre måneder brukes ikke.
4. Radene i `ev_data_10` hentes for personen og den måneden.

I dev traff `ev05_sak` en sak i 99,7 % av 20 000 rader i `ev_valg_05`. Bommene var 48 med en annen GH-sak, 8 uten sak på `f_nr` og 1 i et annet kapittel.

Verdien står i `ev10_text`. `ev10_belop` brukes ikke. Spørringen henter bare posisjonene i tabellen under (`Ev10Posisjon`).

| `ev10_posisjon` | Felt | Innhold |
|-----------------|------|---------|
| `055` | `diagnose1` | Diagnose. De fire første tegnene. |
| `100` | `diagnose2` | Diagnose. De fire første tegnene. |
| `150` | `sats` | Hele teksten. |
| `160` | `hjemmelbokstav` | Hele teksten. |
| `165` | `virkningstidspunkt` | MMÅÅ, se under. |

Virkningstidspunkt står på formatet MMÅÅ. ÅÅ fra 36 til 99 betyr 1936 til 1999, og ÅÅ fra 00 til 35 betyr 2000 til 2035. Tom tekst og `0000` gir ingen verdi.

#### Motstridende felt

`ev_data_10` har ingen kolonne for sak, stønadstype eller tekstkode. Radene gjelder personen og måneden. For hver posisjon samler vi derfor alle radene i måneden, uansett `ev10_vedtaks_dag` og sak, og sammenligner verdien vi viser. Diagnoser sammenlignes på de fire første tegnene, og virkningstidspunkt som måned.

Feltet fylles når to vilkår er oppfylt:

- Alle radene har samme verdi. Tom tekst er en verdi, så tom tekst mot `G400` er en konflikt.
- Posisjonen har minst én rad per GH-sak (`ev05_sak`) i måneden. Har en sak ingen rad, kan raden vi ser høre til en annen sak.

Ellers er feltet `null`, og posisjonen står i `motstridendeFelt`. Et ugyldig virkningstidspunkt havner også der. Saksbehandler må lese disse feltene i Infotrygd. De andre feltene i evalueringen fylles som vanlig. Oppslaget gir ikke 500 på grunn av evalueringsdata.

#### Grunnlaget for regelen

Målt i dev 2026-10-20. Tallene gjelder personmåneder med `EVGH`, og 162 160 av dem har én GH-sak.

- Alle fem posisjonene finnes i hver personmåned. En verdi som mangler, ligger som tom tekst, for eksempel 111 092 rader i posisjon `100`.
- Med én sak har en posisjon én rad i 98,2 % av tilfellene. Uten `ev10_vedtaks_dag` får høyst 162 posisjonsmåneder ulik tekst: `055` 8, `100` 35, `150` 25 og `160` 94.
- Med flere saker (13 181 personmåneder) har posisjonene like mange rader som saker i 13 177 til 13 180 av dem. Én personmåned har færre rader enn saker, og fem har flere.
- Samme måned kan ha evalueringer med andre tekstkoder i GH, for eksempel `EVBE` (86 481 personmåneder), `EVZ1`, `EVGS` og `EVOM`. Disse skriver også til `055`, `100`, `150` og `160` i måneder uten `EVGH`. I måneder med `EVGH` gir de ingen ekstra rader i våre posisjoner, se tallene over. `165` brukes bare av `EVGH`.

`ev_data_10` har rundt 100 andre posisjoner, de fleste fra andre tekstkoder. Disse har innhold og kan være aktuelle senere: `265`, `300` og `332` (cirka 6 700 personmåneder, ingen tom tekst), `062` (7 065) og `060` (4 377). Vi vet ikke hva de betyr. Les [ADR-0011](../../docs/adr/ADR-0011-historiske-evalueringsdata-fra-infotrygd.md) før du legger til en posisjon, og mål på nytt.

Ugyldige virkningstidspunkt logges med verdi til team logs. Vanlig logg får bare antall evalueringer med motstridende felt per posisjon.

Diagnosene og hjemmelbokstaven er helseopplysninger (GDPR art. 9). Oppslagene auditlogges ikke ennå, se ADR-0009.

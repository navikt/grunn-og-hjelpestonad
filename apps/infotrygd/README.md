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

Et diagram over tabellene og koblingene står i
[docs/gh-i-infotrygd.md](docs/gh-i-infotrygd.md#tabellene-vi-replikerer).

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
| `POST /api/infotrygd/stonader` | Stønadene for én stønadstype, som et tre fra `t_stonad` til vedtak, endringer, delytelser, `t_gh` og evaluering |
| `POST /api/infotrygd/stonad/eksisterer` | Om personene har vedtak eller saker, og om vedtakene er løpende |
| `GET /api/infotrygd/stonad/migreringspersoner` | Personer med løpende stønad fra neste måned |
| `POST /api/infotrygd/saker/finn` | Saker fra `sa_sak_10` |
| `GET /api/infotrygd/tabeller/*` | Kolonner og antall rader i replikatabellene |

### Auditlogg

`stonader`, `stonad/eksisterer` og `saker/finn` skriver én linje per kall til
loggeren `auditLogger`, som bare går til team logs:
`Z123456 ba om tilgang til fnr: 12345678901. Svar: ja`. De tre endepunktene tar
én personident per kall. Flere gir `400`.
`Nav-Call-Id` følger med som eget felt. Svaret er `nei` både ved avslag og når
tilgangsmaskinen ikke svarer. `stonad/migreringspersoner` logges ikke.

Søk etter `logger_name:auditLogger` i team logs. Team logs er ikke en gyldig
auditlogg for prod, se [ADR-0009](../../docs/adr/ADR-0009-tilgangskontroll-mot-person.md).

### Kontrakt mot sak

[`openapi.json`](openapi.json) er kontrakten mellom infotrygd og sak. Sak
genererer klientmodellene sine fra filen (pakken
`no.nav.grunn.og.hjelpestonad.infotrygd.kontrakt`) med
`openapi-generator-maven-plugin`. `OpenApiKontraktTest` skriver filen fra
`/v3/api-docs` og feiler når den var utdatert. Endrer du API-et, kjør testene og
commit den oppdaterte filen.

### Stønader

`/stonader` erstattet `/perioder`, som fulgte `/api/perioder` i EF-appen
([ADR-0005](../../docs/adr/ADR-0005-perioder-fra-infotrygd.md)). `/perioder`
filtrerte bort vedtak og ga én rad per delytelse og endringskode. `/stonader`
ligger ikke nær EF-appen. Svaret følger tabellene i stedet:

- Forespørselen har én påkrevd `stønadstype`. Konsumenten kaller én gang per
  stønadstype.
- Svaret er et tre langs fremmednøklene: `t_stonad` → `t_vedtak` → `t_endring`,
  `t_delytelse` og `t_gh`. Klassene heter som tabellene og feltene som kolonnene,
  i camelCase. `personnr` kommer fra `t_lopenr_fnr`.
- Bare kolonnene vi trenger er med. Metadata fra replikeringen (`opprettet`,
  `oppdatert`, `db_splitt`) og løpenumre for andre personer er utelatt.
- Ingen rader filtreres bort, bortsett fra på person og `kode_rutine`. Vedtak
  uten endringer eller delytelser, vedtak med `dato_innv_tom` før eller lik
  `dato_innv_fom` og stønader uten `oppdrag_id` er med. Konsumenten bestemmer
  hva som vises.
- Et vedtak tas bare med når `t_vedtak.person_lopenr` hører til en av identene
  i forespørselen, ikke bare når stønaden gjør det. Tilgangen er bare sjekket for
  disse identene. Replikaen har ingen fremmednøkler, og i dev hadde fire vedtak
  et annet løpenummer enn stønaden sin, der det ene løpenummeret manglet i
  `t_lopenr_fnr`. Slike vedtak er ikke med.
- `evaluering` på vedtaket er det eneste avledede feltet, se
  [Evaluering](#evaluering).
- Tekst er trimmet, og listene er sortert på primærnøkkelen.

Hver tabell hentes i én spørring med `IN` på nøklene fra forelderen, så antall
spørringer er fast. Spørringene kjøres i én `REPEATABLE_READ`-transaksjon, slik
at de ser samme øyeblikksbilde. Replikaen er likevel ikke konsistent på tvers av
tabeller mens replikeringen pågår, så et vedtak kan mangle delytelser en stund.

### Evaluering

Evalueringen er feltet `evaluering` på hvert vedtak i `/stonader`. Vedtak uten evaluering får `null`.

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

Virkningstidspunkt står på formatet MMÅÅ. ÅÅ fra 51 til 99 betyr 1951 til 1999, og ÅÅ fra 00 til 50 betyr 2000 til 2050. Tom tekst og `0000` gir ingen verdi.

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

`ev_data_10` har rundt 100 andre posisjoner, de fleste fra andre tekstkoder. Disse har innhold og kan være aktuelle senere: `265`, `300` og `332` (cirka 6 700 personmåneder, ingen tom tekst), `062` (7 065) og `060` (4 377). Vi vet ikke hva de betyr. Les [ADR-0010](../../docs/adr/ADR-0010-historiske-evalueringsdata-fra-infotrygd.md) før du legger til en posisjon, og mål på nytt.

Ugyldige virkningstidspunkt logges med verdi til team logs. Vanlig logg får bare antall evalueringer med motstridende felt per posisjon.

Diagnosene og hjemmelbokstaven er helseopplysninger (GDPR art. 9). Oppslagene logges til team logs, se [Auditlogg](#auditlogg).

#### Kjente tilfeller uten evaluering

Målt i dev 2026-10-21 for én syntetisk person med en revurdering.

- Én stønad (`stonad_id`) kan spenne over flere saker. Vedtak 1 på sak A01 og vedtak 2 på sak A02 hadde samme `stonad_id`.
- Maskinvedtak kan mangle EVGH. Vedtak 2 hadde `brukerid` AUTO, `type_sak` MS, `kode_resultat` FI og endringskode S. Saken hadde tom `s10_eval_kode`. Saken med EVGH hadde `s10_eval_kode` lik `EVGH`.
- `s10_knyttet_til_sak` var `00` på begge sakene, og `t_vedtak.nokkel_dl1` var tom. Ingen av dem lenker til forrige sak.
- `ev10_vedtaks_dag` (06) samsvarte med vedtaksdagen for vedtaket med evalueringen. Virkningstidspunkt `0626` samsvarte med innvilget fom 2026-06-01.
- API-et returnerer evaluering bare for vedtakets egen sak. Frontend viser evalueringen fra nærmeste tidligere vedtak på samme stønad, merket som hentet fra et tidligere vedtak.

# Infotrygd

App som henter ut data fra Infotrygd for grunn- og hjelpestønad.

Denne appen er én av tre i monorepoet — se [rot-README](../../README.md) for oversikt.
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

### Formater

- **Fnr:** `t_lopenr_fnr.personnr` er vanlig fnr (DDMMÅÅPPPPP). `f_nr` i `sa_*`
  og `ev_*` er reversert (ÅÅMMDDPPPPP), og `s01_personkey` er `tk_nr` fulgt av
  reversert fnr. Bruk `reverserFnr()` før oppslag.
- **Datoer:** Tabellene med prefiks `t_` har vanlige datoer. I `sa_*` og `ev_*`
  er datoer lagret som tall, og formatet varierer per kolonne. Se KDoc i
  [`InfotrygdFormat.kt`](src/main/kotlin/no/nav/grunn/og/hjelpestonad/infotrygd/util/InfotrygdFormat.kt).
- **Løpende vedtak:** Et vedtak uten `dato_opphor` og `dato_innv_tom` regnes som
  løpende, siden stønaden ofte varer livet ut. Dette er et bevisst avvik fra
  EF-appen.

## Endepunkter

| Endepunkt | Beskrivelse |
|-----------|-------------|
| `POST /api/infotrygd/perioder` | Perioder gruppert i `grunnstønad` og `hjelpestønad` |
| `POST /api/infotrygd/stonad/eksisterer` | Om personene har vedtak eller saker, og om vedtakene er løpende |
| `GET /api/infotrygd/stonad/migreringspersoner` | Personer med løpende stønad fra neste måned |
| `POST /api/infotrygd/saker/finn` | Saker fra `sa_sak_10` |
| `GET /api/infotrygd/tabeller/*` | Kolonner og antall rader i replikatabellene |

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

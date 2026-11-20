# Sak (backend)

Saksbehandler-app for grunn- og hjelpestønad.

Denne appen er én av tre i monorepoet — se [rot-README](../../README.md) for oversikt.
**Alle kommandoer under kjøres fra `apps/sak`.**

## Opphav og referanser

`apps/sak` er forket fra [navikt/gjenlevende-bs-sak](https://github.com/navikt/gjenlevende-bs-sak), og bruker
[navikt/familie-ef-sak](https://github.com/navikt/familie-ef-sak) og
[navikt/familie-ba-sak](https://github.com/navikt/familie-ba-sak) som referanser. Vi velger det vi mener er best
fra hver. EF skal eie løsningen, så kode som ligner EF er et pluss, men kodekvalitet og arkitektur veier tyngst.

## Forutsetninger

- **Colima** må være installert og kjøre
- **IntelliJ IDEA** (anbefalt)

---

## Lokal kjøring

Applikasjonen kjøres lokalt med mock-profilen, som krever **ingen secrets** og fungerer fullt offline.

#### 1. Start mock-miljøet
```bash
cd apps/sak
./start-mock.sh
```
Dette starter følgende Docker-containere:
- PostgreSQL (persistent database)
- mock-oauth2-server (for token-validering)
- WireMock (mocker eksterne tjenester)

#### 2. Kjør applikasjonen
Kjør **ApplicationLocalMock** fra IntelliJ (ingen miljøvariabel-konfigurasjon nødvendig).

#### 3. Test med bruker-token

Frontendens lokale Authorization Code Flow fullføres automatisk mot mock-serveren. For manuell testing kan du fortsatt åpne http://localhost:8089/default/debugger. Mock-serveren konfigurerer tokenet med `NAVident` og saksbehandlergruppen.

#### 4. Stopp tjenestene
```bash
docker compose --profile mock down      # Behold data
docker compose --profile mock down -v   # Slett data
```

---

## Database

**Persistent PostgreSQL**-database via Docker-volume.
- Data overlever omstart av applikasjonen
- Slett data: `docker compose --profile mock down -v`
- Se data i Docker Desktop under "grunn-og-hjelpestonad"-gruppen

---

## Swagger

**Mock-profil (lokalt):**
- http://localhost:8082/swagger-ui/index.html
- Hent token og lim inn i "Authorize"

**Ingress (deployed):**
- https://grunn-og-hjelpestonad-backend.intern.dev.nav.no/swagger-ui/index.html
- Trykk "Authorize" og logg inn med Azure AD (la `client_secret` stå tom)

## Observability

Actuator eksponerer liveness og readiness på `/internal/health/liveness` og
`/internal/health/readiness`. Prometheus-metrikker er tilgjengelige på
`/internal/prometheus` og inkluderer Spring Boot-, JVM-, database- og Kafka-metrikker.

## Prosessering i efterlatte-verktoy (kun dev)

`ProsesseringTilgang.harRolleProsessering` kontrollerer brukertoken og
prosesseringsgruppe før hvert API-kall. Tilkoblingen må prøves i dev før bruk.

Admin-API-et følger `/api/prosessering/task`-kontrakten i
[efterlatte-verktoy](https://github.com/navikt/efterlatte-verktoy/pull/25):
statusoversikt, typer, paginert liste, enkeltoppslag, logg, rekjøring, avbryt og avvik.
Listefilteret bruker `status`, `type`, `side` (fra 1) og `antallPerSide` (1–100).
Skriveoperasjoner krever `versjon`; avvik krever også en ikke-tom `melding`.
Konflikter gir `409`, og task-endring og historikk lagres i samme transaksjon.
Rekjøring beholder `antallFeil`.

### Tilgang og sensitive opplysninger

Tilgangen krever Azure OBO-brukertoken med `NAVident` og gruppen
`928636f4-fd0d-4149-978e-a6fb68bb19de` (`prosessering.rolle`). Maskintoken
avvises: token med `idtyp=app`, lik `sub` og `oid`, eller manglende delegert
tilgang (`scp`) gir avslag. `sub` og `oid` må være satt.
Gruppen gir lesing og administrasjon av **alle tasks uten personsjekk**,
også tasks med sensitive opplysninger. Se det avgrensede driftsunntaket i
[ADR-0009](../../docs/adr/ADR-0009-tilgangskontroll-mot-person.md#driftsunntak-for-prosessering-i-dev).
Ingen saksbehandlerroller eller øvrige endepunkter skal åpnes.

API-et, sikkerhetskjeden og tilgangskontrollen er bare aktive med `dev`, og aldri
sammen med `prod` eller `local-mock`. Eksisterende validering av signatur,
utsteder, audience og utløp beholdes. Svar mellomlagres ikke. Utlevering av tasks
og hendelseslogg samt utførte handlinger logges til `secureLogger` med NAV-ident
og task-id, uten payload og avvikstekst. Sensitive feildetaljer må aldri sendes
til vanlig logg.

### Tilkobling, utrulling og tilbakeføring

Dev-manifestet tillater inbound fra `efterlatte-verktoy` i namespace `etterlatte`
og inkluderer prosesseringsgruppen i Azure-claims. På verktøysiden brukes:

| Innstilling | Verdi |
|---|---|
| Base-URL | `http://grunn-og-hjelpestonad.grunn-og-hjelp` |
| OBO-scope | `api://dev-gcp.grunn-og-hjelp.grunn-og-hjelpestonad/.default` |
| API-variant | `kontrakt` |

Begge repoer må deployes til dev med tilgangskontrollen på plass. Bruk
eksisterende backend-workflows; ingen nye ressurser eller migrasjoner kreves.

Før API-et regnes som åpnet:

- Bekreft at riktig gruppe får tilgang, mens feil gruppe, manglende `NAVident`
  og maskintoken avvises.
- Bekreft liste, statusoversikt og historikk, samt rekjøring og avvik på testtasks.
- Følg HTTP-statuskoder og responstider i eksisterende HTTP-metrikker, og
  handlingslogger i Team Logs. Bekreft at sensitivt innhold ikke finnes i vanlig logg.
- Bekreft at API-et ikke registreres med prod-profil.

Tilbakeføring: deploy forrige backend-versjon og fjern inbound-regelen for
verktøyet. Dette stenger tilgang, men angrer ikke utførte operatørhandlinger.

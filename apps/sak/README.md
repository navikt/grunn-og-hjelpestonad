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

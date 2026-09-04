# Sak (backend)

Saksbehandler-app som tar for seg barnetilsyn og skolepenger for etterlatte/gjenlevende.

Denne appen er én av tre i monorepoet — se [rot-README](../../README.md) for oversikt.
**Alle kommandoer under kjøres fra `apps/sak`.**

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
- WireMock (mocker alle eksterne tjenester)

#### 2. Kjør applikasjonen
Kjør **ApplicationLocalMock** fra IntelliJ (ingen miljøvariabel-konfigurasjon nødvendig).

#### 3. Test med mock-token
```bash
# Hent token
TOKEN=$(curl -s -X POST http://localhost:8089/default/token \
  -d 'grant_type=client_credentials&client_id=test&client_secret=test' | jq -r '.access_token')

# Test API
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/internal/health
```

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
- https://grunn-og-hjelpestonad.intern.dev.nav.no/swagger-ui/index.html

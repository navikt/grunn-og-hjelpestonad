# Frontend

Saksbehandlingsflate for grunn- og hjelpestønad.

Denne appen er én av tre i monorepoet — se [rot-README](../../README.md) for oversikt.
**Alle kommandoer under kjøres fra `apps/frontend`.**

## Kom i gang

### 1. Installasjon

Installering av avhengigheter:

```bash
cd apps/frontend
npm ci
```

#### Installering av @navikt pakker

For å kunne installere private @navikt-pakker fra GitHub Package Registry trenger du et Personal Access Token (PAT).

1. **Opprett et Personal Access Token:**
   - Gå til [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)]
   - Gi tokenet `read:packages` scope
   - Kopier tokenet

2. **Logg inn med npm:**

   ```bash
   npm login --scope=@navikt --registry=https://npm.pkg.github.com
   ```

   - **Username:** Ditt GitHub brukernavn
   - **Password:** Personal Access Token (PAT) som du genererte

### 2. Hent og sett miljøvariabler

Scriptet oppretter en lokal `.env`-fil og konfigurerer Authorization Code Flow mot mock OAuth-serveren.

```bash
sh hent-og-lagre-miljovariabler.sh
```

### 3. Start utviklingsserver

```bash
npm run dev
```

Applikasjonen er tilgjengelig på http://localhost:8080/. Første sidevisning fullfører Authorization Code Flow automatisk mot mock-serveren.

## Observability

Frontend-serveren eksponerer følgende endepunkter:

- `GET /isAlive` for liveness
- `GET /isReady` for frontend readiness
- `GET /metrics` for Prometheus-metrikker

Klientfeil, web-vitals og nettverkstracing sendes via Grafana Faro i dev- og
produksjonsmiljø. Lokal kjøring pauser Faro-eksport.

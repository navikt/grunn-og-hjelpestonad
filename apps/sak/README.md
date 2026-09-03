# Sak (backend)

Saksbehandler-app som tar for seg barnetilsyn og skolepenger for etterlatte/gjenlevende.

Denne appen er én av tre i monorepoet — se [rot-README](../../README.md) for oversikt.
**Alle kommandoer under kjøres fra `apps/sak`.**

## Forutsetninger

- **Docker Desktop** må være installert og kjøre
- **IntelliJ IDEA** (anbefalt)
- **nais CLI** (kun for dev-profil)

---

## Lokal kjøring

Applikasjonen har to lokale utviklingsprofiler:

| Profil | Bruk | Fordeler |
|--------|------|----------|
| **Mock** (anbefalt) | Daglig utvikling | Ingen secrets, fullt offline, rask oppstart |
| **Dev** | Testing mot ekte dev-tjenester | Ekte data fra PDL, SAF, etc. |

---

### Mock-profil (Anbefalt for daglig utvikling)

Denne profilen krever **ingen secrets** og fungerer fullt offline.

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

### Dev-profil (For testing mot ekte tjenester)

Bruk denne kun når du må teste mot ekte dev-tjenester (PDL, SAF, Tilgangsmaskin, etc.).

#### 1. Logg på Nais

```bash
nais login
```

> **Husk:** Du må ha Nais device installert og kjørende!

#### 2. Bytt til riktig Kubernetes-kontekst

```bash
kubectl config use-context dev-gcp
```

> ⚠️ **ADVARSEL:** Du **MÅ** bruke `dev-gcp` - scriptet fungerer **IKKE** med `prod-gcp`!
>
> Verifiser at du er i riktig kontekst:
> ```bash
> kubectl config current-context
> ```
> Skal vise: `dev-gcp`

#### 3. Sett namespace til etterlatte

```bash
kubectl config set-context --current --namespace=etterlatte
```

#### 4. Finn riktig Azure-hemmelighet

```bash
kubectl get secrets | grep gjenlevende-bs-sak
```

Du vil se noe lignende dette:
```
azure-gjenlevende-bs-sak-1a2345bc-1337-1      Opaque   7      2d
```

> **VIKTIG:** Kopier navnet på hemmeligheten som starter med `azure-gjenlevende-bs-sak-` og har en roterende ID (f.eks. `azure-gjenlevende-bs-sak-1a2345bc-1337-1`).

#### 5. Oppdater hent-og-lagre-miljøvariabler.sh

Åpne filen `hent-og-lagre-miljøvariabler.sh` og finn linje 11. Erstatt hemmelighetsnavnet med det du kopierte:

```bash
GJENLEVENDE_BS_SAK_LOKAL_SECRETS=$(get_secrets azure-gjenlevende-bs-sak-WHATEVER)
```

#### 6. Kjør scriptet for å hente hemmeligheter

```bash
./hent-og-lagre-miljovariabler.sh
```

Dette oppretter en skjult `.env.local`-fil i `apps/sak`.

#### 7. Start dev-miljøet

```bash
./start-dev.sh
```

Dette starter PostgreSQL og Texas (token-proxy) i Docker.

#### 8. Konfigurer IntelliJ med miljøvariabler

Dette er viktig - følg stegene nøye:

1. Finn **ApplicationLocalDev** i prosjekt-treet (`apps/sak/src/test/kotlin/.../ApplicationLocalDev.kt`)
2. Klikk på den **grønne play-knappen** ▶️ ved siden av `fun main()`
3. Velg **Modify Run Configuration...**
4. I vinduet som åpnes, se på høyre side under **Build and run**
5. Klikk på **Modify options** (eller "More options")
6. Velg **Environment variables**
7. Et nytt felt for miljøvariabler vises
8. Klikk på **mappe-ikonet** 📁 til høyre for feltet
9. En fil-utforsker åpnes - naviger til `apps/sak` i repositoryet
10. Filen `.env.local` er **skjult**. På Mac: trykk `Shift + Cmd + .` for å vise skjulte filer
11. Velg `.env.local` og klikk **OK**
12. Klikk **Apply** og deretter **OK**

#### 9. Kjør applikasjonen

Kjør **ApplicationLocalDev** fra IntelliJ (trykk ▶️ eller `Ctrl+R` / `Cmd+R`).

#### 10. Stopp tjenestene

```bash
docker compose --profile dev down       # Behold data
docker compose --profile dev down -v    # Slett data
```

---

## Database

Begge profiler bruker en **persistent PostgreSQL**-database via Docker-volume.
- Data overlever omstart av applikasjonen
- Slett data: `docker compose --profile <mock|dev> down -v`
- Se data i Docker Desktop under "gjenlevende-bs-sak"-gruppen

---

## Swagger

**Mock-profil (lokalt):**
- http://localhost:8082/swagger-ui/index.html
- Hent token og lim inn i "Authorize"

**Dev-profil (lokalt):**
- http://localhost:8082/swagger-ui/index.html

**Ingress (deployed):**
- https://gjenlevende-bs-sak.intern.dev.nav.no/swagger-ui/index.html

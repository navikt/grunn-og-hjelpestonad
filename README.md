# grunn-og-hjelpestonad-backend

Monorepo for saksbehandling av **grunn- og hjelpestønad**.

> [!NOTE]
> Koden er flyttet fra [navikt/grunn-og-hjelpestonad](https://github.com/navikt/grunn-og-hjelpestonad) med en clean-slate tilnærming.

## Innhold

| App | Katalog | Teknologi | Nais-app | Cluster |
|-----|---------|-----------|----------|---------|
| **Sak** (backend) | [`apps/sak`](apps/sak) | Kotlin / Spring Boot / Maven / PostgreSQL | `grunn-og-hjelpestonad` | `dev-gcp` |
| **Frontend** | [`apps/frontend`](apps/frontend) | TypeScript / React Router 7 / Express / Node | `grunn-og-hjelpestonad-frontend` | `dev-gcp` |
| **Infotrygd** | [`apps/infotrygd`](apps/infotrygd) | Kotlin / Spring Boot / Maven / Oracle | `grunn-og-hjelpestonad-infotrygd` | `dev-fss` |

Appene er selvstendige og har hver sin build- og deploy-pipeline. Det finnes ingen felles
parent-pom eller workspace — hver app bygges fra sin egen katalog.

```
.
├── .github/
│   ├── dependabot.yml            # Samlet Dependabot for alle tre appene
│   └── workflows/                # Én build- og én deploy-workflow per app
└── apps/
    ├── sak/                      # Backend
    ├── frontend/                 # Saksbehandlerflate
    └── infotrygd/                # Integrasjon mot Infotrygd (dev-fss)
```

## Kom i gang

Alle kommandoer kjøres **fra app-katalogen**, ikke fra rota.

### Sak (backend)

```bash
cd apps/sak
./start-mock.sh          # Docker: PostgreSQL, mock-oauth2-server, WireMock
```

Kjør deretter `ApplicationLocalMock` fra IntelliJ. Se [apps/sak/README.md](apps/sak/README.md)
for Swagger og databasehåndtering.

### Frontend

```bash
cd apps/frontend
npm ci
sh hent-og-lagre-miljovariabler.sh   # Krever pålogget naisdevice
npm run dev                          # http://localhost:8080
```

Se [apps/frontend/README.md](apps/frontend/README.md) for oppsett av `@navikt`-pakker fra
GitHub Package Registry og hvordan du peker frontend mot lokal backend.

### Infotrygd

```bash
cd apps/infotrygd
mvn verify --settings .m2/maven-settings.xml
```

Se [apps/infotrygd/README.md](apps/infotrygd/README.md).

## Bygg og deploy

Workflowene er sti-filtrerte, så en endring i én app trigger kun den appens pipeline.

| Workflow | Trigger |
|----------|---------|
| `sak-build.yaml` | PR som endrer `apps/sak/**` |
| `sak-deploy-dev.yaml` | Push til `main` som endrer `apps/sak/**` |
| `sak-deploy-topics.yaml` | Manuell (`workflow_dispatch`) |
| `frontend-build.yaml` | PR som endrer `apps/frontend/**` |
| `frontend-deploy-dev.yaml` | Push til `main` som endrer `apps/frontend/**` |
| `infotrygd-build.yaml` | PR som endrer `apps/infotrygd/**` |
| `infotrygd-deploy-dev.yaml` | Push til `main` som endrer `apps/infotrygd/**` |
| `codeql.yml` | Push/PR mot `main` og ukentlig |

Docker-images bygges med `nais/docker-build-push` og skilles med `image_suffix`:

| App | Image |
|-----|-------|
| Sak | `.../etterlatte/grunn-og-hjelpestonad` |
| Frontend | `.../etterlatte/grunn-og-hjelpestonad-frontend` |
| Infotrygd | `.../etterlatte/grunn-og-hjelpestonad-infotrygd` |

## Avhengigheter

Dependabot er konfigurert i [`.github/dependabot.yml`](.github/dependabot.yml) med ett
oppdateringssett per app. PR-er merkes med `sak`, `frontend` eller `infotrygd` slik at det
er tydelig hvilken app som berøres.

| Økosystem | Katalog | Gruppering |
|-----------|---------|------------|
| maven | `/apps/sak` | Spring Boot, Kotlin, no.nav, test, logging, øvrige |
| maven | `/apps/infotrygd` | Alle i én gruppe |
| npm | `/apps/frontend` | Minor+patch i én gruppe, major i én gruppe |
| github-actions | `/` | Minor+patch i én gruppe, major i én gruppe |

Alle kjører ukentlig mandag kl. 06:00 (Europe/Oslo) med fem dagers cooldown.
npm-oppdateringer henter `@navikt`-pakker via `READER_TOKEN`.

## Lisens

[MIT](LICENSE)

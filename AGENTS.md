# Frontend
(`apps/frontend`): React 19 + React Router 8 (framework mode, Express-server) i TypeScript, med Aksel (`@navikt/ds-react` 8) som designsystem, React Compiler slått på i `vite.config.ts` og API-klient generert fra backendens OpenAPI med `@hey-api/openapi-ts` (`npm run generate:api`).
- Bruk aksel-builder skill når du skal jobbe med aksel-komponenter i frontend. For eksmpel når du lager skjemaer.
- `npm run typecheck` feiler med `EPERM ... open '.env'` fordi `.env` er blokkert av content exclusion. Kjør `npx tsc` direkte i stedet.

# Backend
(`apps/sak`): Kotlin 2.4 på Java 25 med Spring Boot 4 (web, security/OAuth2, Data JDBC), PostgreSQL med Flyway-migrasjoner, springdoc-openapi for API-spesifikasjonen, og bygg med Maven.

# Infotrygd
(`apps/infotrygd`): Forket fra `navikt/familie-ef-infotrygd-replika` (EF-appen). Som EF-appen leser den Infotrygd-tabeller som er replikert fra Exodus til Postgres, ikke Oracle direkte. Samme stack som `apps/sak`.
- Les [`apps/infotrygd/README.md`](apps/infotrygd/README.md) før du endrer spørringer. Den beskriver tabellene, fnr- og datoformatene og hvordan `/perioder` avviker fra EF-appen.
- Hold nye endepunkter så nær EF-appen som dataene tillater, og skriv avvikene i README-en.



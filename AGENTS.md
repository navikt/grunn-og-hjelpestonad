# Frontend
(`apps/frontend`): React 19 + React Router 8 (framework mode, Express-server) i TypeScript, med Aksel (`@navikt/ds-react` 8) som designsystem, React Compiler slått på i `vite.config.ts` og API-klient generert fra backendens OpenAPI med `@hey-api/openapi-ts` (`npm run generate:api`).
- Bruk aksel-builder skill når du skal jobbe med aksel-komponenter i frontend. For eksmpel når du lager skjemaer.
- `npm run typecheck` feiler med `EPERM ... open '.env'` fordi `.env` er blokkert av content exclusion. Kjør `npx tsc` direkte i stedet.

# Backend
(`apps/sak`): Kotlin 2.4 på Java 25 med Spring Boot 4 (web, security/OAuth2, Data JDBC), PostgreSQL med Flyway-migrasjoner, springdoc-openapi for API-spesifikasjonen, og bygg med Maven.




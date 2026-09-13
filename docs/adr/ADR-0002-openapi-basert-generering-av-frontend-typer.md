# ADR-0002: OpenAPI-basert generering av frontend-typer

**Dato:** 2026-09-11
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

Backend i `apps/sak` publiserer et OpenAPI-dokument gjennom Springdoc på
`/v3/api-docs`. Frontend hadde flere manuelt vedlikeholdte TypeScript-modeller
for de samme request- og response-DTO-ene. Det gir risiko for at frontend og
backend utvikler seg med ulike kontrakter, særlig når felter eller enum-verdier
endres.

Vi trenger en løsning som:

- kan kjøres i frontendens eksisterende Node- og npm-pipeline
- støtter OpenAPI 3.1 og TypeScript 6
- genererer typer uten å legge en HTTP-klient eller runtime-avhengighet i
  frontend-bundlen
- kan regenerere typer fra lokal backend og holde generert output oppdatert i Git

Hvis vi ikke gjør noe, fortsetter applikasjonen å fungere, men kontraktsdrift
og duplisert typeinformasjon vil fortsette å øke.

## Beslutning

Vi bruker `@hey-api/openapi-ts` versjon `0.99.0`, låst til eksakt versjon, med
kun TypeScript-pluginen aktivert. Generatoren leser som standard
`http://localhost:8082/v3/api-docs` og skriver typer til
`apps/frontend/app/api/generated/types.gen.ts`. `OPENAPI_URL` kan brukes til å
peke på en annen OpenAPI-kilde.
Frontend har scriptet `npm run generate:api`. Generering er opt-in: den kjøres
verken av `npm run dev` eller av builden, slik at utviklerkommandoer aldri
endrer innsjekkede filer uten at det er bedt om. Genererte filer skal ikke
redigeres manuelt.
API-eide frontend-modeller skal gradvis erstattes av typer fra den genererte
filen, mens lokale redigeringsmodeller kan beholdes når de representerer en bevisst
transformasjon av API-data.

Server-listen i OpenAPI-dokumentet settes av `OpenApiServerConfig` uavhengig av
Spring-profil, slik at dokumentet — og dermed generert output — blir identisk
enten typene genereres fra lokal backend eller fra et deployet miljø.

### Modellansvar i frontend

Følgende modeller er API-eide og skal importeres fra
`app/api/generated/types.gen.ts`:

| Modell | Bruk |
|---|---|
| `FagsakRequest` og `FagsakDto` | Opprette/hente fagsak og personkontekst |
| `VedtakDto` | Lagret vedtak fra API-et |
| `HistoriskVedtakResponse` | Grunnlag for revurdering |
| `BarnetilsynBeregningRequest` og `BeløpsperioderDto` | Beregning av vedtaksperioder |
| `Barnetilsynperiode` | API-representasjon av en lagret periode |
| `HentBarnResponse` | Barn knyttet til en behandling |

Hooks som snakker med API-et bruker de genererte typene med backend-navnene
sine, både inn og ut. Konvertering skjer kun der redigeringsbildet trenger en
annen form enn API-et.

Vedtaksredigeringen har derfor to bevisste frontend-modeller i
`app/komponenter/behandling/vedtak/vedtak.ts`, begge avledet fra de genererte
typene med `Omit`:

- `Barnetilsynperiode` gjør persistens-ID valgfri og tillater tomme
  `periodetype`-/`aktivitetstype`-felter mens saksbehandleren fyller ut skjemaet.
- `Vedtak` er det samme vedtaket med redigerbare perioder.

Disse modellene er skjematilstand, ikke alternative beskrivelser av API-et.
Fordi de er avledet fra kontrakten, er en lagret `VedtakDto` direkte
tilordnbar til `Vedtak` uten konvertering.

Backend bruker i dag samme Kotlin-modell for lesing og lagring av vedtak, og
`Barnetilsynperiode.id` er derfor påkrevd også i lagre-requesten selv om
frontend ikke har noen ID før lagring. Requesten er dermed ikke typesjekket mot
kontrakten. Se aksjonspunktene.

Når en backend-DTO heter det samme som generatorens navn på en operasjons
responstype, får responstypen et suffiks. `HentBarnResponse` er schema-typen fra
backend, mens `HentBarnResponse2` er hey-api sitt alias for responsen fra
`hentBarn`-operasjonen. Frontend bruker schema-typen `HentBarnResponse`;
aliaset er ubrukt.

Kotlin bruker ASCII-only kapitalisering når det genererer JavaBean-gettere.
For REST-DTO-er med egenskaper som starter med `å`, `æ` eller `ø` bruker
backend `@get:JvmName` for å eksponere korrekt getter-navn til Springdoc, uten
å endre Kotlin- eller JSON-egenskapen. Dette gjelder de lokale DTO-ene
`ÅrsakBehandlingRequest`, `ÅrsakBehandlingDto`, `BeslutteVedtakDto` og
`TotrinnskontrollDto`. De dependency-eide task-endepunktene under
`/api/task/**` holdes utenfor OpenAPI-kontrakten, og `AvvikshåndterDTO` blir
derfor ikke med i frontend-typene.

## Alternativer vurdert

### Alternativ A: `@hey-api/openapi-ts` ✅ (valgt)

**Beskrivelse:** Bruke Hey API-generatoren med bare TypeScript-pluginen, uten
generert SDK eller HTTP-klient.

**Fordeler:**

- Støtter prosjektets TypeScript 6 og Node 26 uten peer-dependency-konflikt.
- Støtter OpenAPI 3.1 og genererer både DTO-typer og endpoint-spesifikke
  request-/response-typer.
- Kan kjøres fra npm-scriptet og ta OpenAPI-dokumentet direkte fra en URL.
- Holder runtime- og bundle-overflaten uendret fordi outputen er type-only.
- Eksakt versjon kan låses for forutsigbare CI-builds.

**Ulemper:**

- Pakken er i initial utvikling og må oppgraderes kontrollert.
- Generering krever at en tilgjengelig OpenAPI-kilde finnes.
- Generert output må holdes synkronisert med backend.

**Nav-vurdering:** Dette er den minste løsningen som løser kontraktsdrift uten
å bygge en ny API-klient. Den støtter Team First og essential complexity ved å
la teamet eie arbeidsflyten i frontend-repositoriet.

### Alternativ B: `openapi-typescript`

**Beskrivelse:** Bruke den etablerte generatoren som produserer runtime-frie
TypeScript-typer.

**Fordeler:**

- Enkel og velkjent modell for typegenerering.
- Genererer små, runtime-frie typefiler.
- Har god støtte for OpenAPI 3.0 og 3.1.

**Ulemper:**

- Versjon `7.13.0` deklarerer peer dependency `typescript: ^5.x`, mens
  frontend bruker TypeScript 6.0.3.
- Installasjon med prosjektets avhengigheter gir `ERESOLVE` uten
  `--legacy-peer-deps`, som ville gjort CI-installasjonen mindre forutsigbar.
- Krever egne typer eller hjelpefunksjoner dersom endpoint-spesifikke
  request-/response-kontrakter skal eksponeres på samme måte.

**Nav-vurdering:** Et godt alternativ dersom prosjektet går tilbake til
TypeScript 5, men ikke riktig valg for dagens frontend uten å svekke
avhengighetskontrollen.

### Alternativ C: OpenAPI Generator med `typescript-fetch`

**Beskrivelse:** Generere en komplett TypeScript-klient fra OpenAPI.

**Fordeler:**

- Kan generere klientmetoder, modeller og request-bygging samlet.
- Har et stort økosystem med flere templates.

**Ulemper:**

- Introduserer en større generator- og runtime-overflate enn behovet tilsier.
- Generert klient vil overlappe med frontendens eksisterende `apiCall` og
  Express-proxy.
- Java/JAR- eller Docker-baserte generatorsteg gir mer CI-kompleksitet.
- Krever en større migrering av eksisterende kall og feilhåndtering.

**Nav-vurdering:** Unødvendig accidental complexity for et behov som primært
er statisk typeinformasjon.

### Alternativ D: Gjøre ingenting

**Beskrivelse:** Beholde manuelt vedlikeholdte TypeScript-modeller.

**Fordeler:**

- Ingen ny avhengighet eller endring i utviklerflyten.
- Ingen migrasjonskostnad nå.

**Ulemper:**

- Fortsatt risiko for at frontend- og backend-kontrakter divergerer.
- Nye enum-verdier og felter må oppdateres flere steder manuelt.
- Kontraktsfeil oppdages senere, ofte først ved integrasjon eller i runtime.

**Nav-vurdering:** Lavest kortsiktig kostnad, men i strid med målet om
produktutvikling med en tydelig og gjenbrukbar API-kontrakt.

## Nav-spesifikke vurderinger

### Arkitektur

- OpenAPI-kontrakten er allerede produsert av backend; beslutningen gjenbruker
  en eksisterende kapabilitet fremfor å etablere en parallell kontrakt.
- Løsningen er avgrenset til frontendens build- og utviklerflyt og påvirker
  ikke runtime-kommunikasjonen mellom frontend og backend.
- Den genererte typen er en kontrakt mellom to deler av samme produkt. Den
  introduserer ikke delt database, synkron runtime-avhengighet eller ny
  tjeneste.
- Teamet beholder autonomi over når typer regenereres og kan gradvis migrere
  lokale redigeringsmodeller.

### Sikkerhet og personvern

- OpenAPI-dokumentet beskriver API-strukturer som blant annet inneholder
  personidenter, dokumenter og saksdata, men generatoren lagrer ikke faktiske
  personopplysninger i outputen.
- Genereringen legger ikke til nye kall i produksjon og endrer ikke eksisterende
  OAuth2-/JWT-autentisering, Express-proxy eller tilgangskontroll.
- Den genererte klienten skal ikke aktiveres uten en ny sikkerhetsvurdering;
  denne ADR-en velger bevisst kun typegenerering.
- OpenAPI-kilden må ikke inneholde tokens, eksempeldata med personopplysninger
  eller andre hemmeligheter. Logger fra generatoren skal ikke publiseres med
  slike data.
- OpenAPI-kilden hentes fra lokal backend ved typegenerering. Generatoren skal
  ikke lagre tokens, eksempeldata med personopplysninger eller andre hemmeligheter
  i outputen.
- Dataklassifisering: Intern for kildekode og API-kontrakt. Faktiske
  personopplysninger skal fortsatt behandles som fortrolige i runtime.

### Plattform (Nais/GCP)

- Ingen nye Nais-ressurser, containere, nettverkstillatelser eller runtime-
  avhengigheter kreves.
- Frontendens eksisterende Node 26-image og npm-baserte CI brukes uendret.
- Generert output er sjekket inn, slik at frontend-builden ikke trenger
  backend, nettverk eller OpenAPI-tilgang i CI.
- Generering fra lokal backend er en utvikleraktivitet. CI regenererer ikke
  typer, så backend-endringer må følges av regenerering og gjennomgang av
  frontend-diff-en.
- Det finnes ingen nye runtime-metrikker eller logger å overvåke. Feil i
  generering bør synes som feil i utviklerkommando eller CI.

### Team og organisasjon

- Berørte team: teamet som eier `sak`-backend og frontend.
- Backend-endringer som påvirker DTO-er eller enum-verdier må følges av
  regenerering og gjennomgang av frontend-kompilering.
- Ingen eksterne konsumenter påvirkes av endringen, siden runtime-API-et er
  uendret.

### Migrasjon

- **Bakoverkompatibilitet:** Runtime-API og JSON-kontrakter er uendret.
  Endringen gjelder kun kildekode og typekilder.
- **Utrullingsstrategi:** Gradvis. API-eide modeller migreres én gruppe om
  gangen; lokale redigeringsmodeller beholdes der de representerer transformasjoner.
- **Feature toggle:** Ikke nødvendig; det er ingen endret runtime-adferd.
- **Rollback-trigger:** Kompilasjonsfeil, uventede typeendringer eller
  generatorfeil som ikke kan løses innen samme endring.
- **Tilbakerulling:** Reverter generator-konfigurasjon, dependency,
  genererte filer og alias-endringer i én Git-endring.
- **Exit criteria:** Alle API-eide modeller som fortsatt dupliseres manuelt er
  enten migrert eller dokumentert som bevisste frontend-modeller. Generering kan
  kjøres fra lokal backend, og generert output er oppdatert.
- **Dekommisjonering:** Fjern manuelle API-modeller når tilsvarende generated
  types er verifisert i produksjonsflyten.

## Konsekvenser

### Positive

- Én maskinlesbar API-kontrakt kan brukes til å lage frontend-typer.
- Mindre duplisering og lavere risiko for type- og enum-drift.
- Ingen ny runtime-klient eller endring i autentiseringsflyten.
- Frontend-teamet får en enkel og dokumentert regenereringskommando.

### Negative

- Frontend får en ny devDependency og en generator i vedlikeholdsflaten.
- Genererte filer gir større diff-er ved backend-endringer.
- Dependency-eide task-endepunkter er fortsatt tilgjengelige i runtime, men
  dokumenteres ikke i frontendens OpenAPI-kontrakt.

### Risiko

| Risiko | Sannsynlighet | Konsekvens | Mitigering |
|--------|---------------|------------|------------|
| Generert output blir foreldet | Middels | Middels | Regenerer fra lokal backend og gjennomgå frontend-diff-en ved backend-endringer |
| Generatorens API endres ved oppgradering | Middels | Lav | Eksakt versjon, Dependabot-review og målrettet typecheck |
| OpenAPI-kilde inneholder sensitive eksempeldata | Lav | Høy | Bruk kun schema-dokumenter, gjennomgå logger og blokker eksempeldata med PII |

## Teknisk gjeld

| # | Gjeldspost | Alv. | Frekv. | Nedslagsfelt | Prioritet | Anbefalt tiltak |
|---|-----------|------|--------|--------------|-----------|-----------------|
| G1 | Dupliserte frontend- og backend-typer | 2 | 3 | 2 | 12 | Migrer API-eide modeller til genererte typer |
| G2 | Manglende stale-output-sjekk i CI | 2 | 2 | 2 | 8 | Etabler deterministisk spec-artifact og CI-validering |
| G3 | Ekstern DTO med Unicode-getter | 1 | 2 | 2 | 4 | Følg opp i `prosessering` dersom task-endepunktene senere skal inn i OpenAPI-kontrakten |
| G4 | Lagre-vedtak deler DTO med hent-vedtak, så `Barnetilsynperiode.id` er påkrevd i requesten | 2 | 2 | 2 | 8 | Eget request-DTO i backend uten påkrevd persistens-ID, så frontend kan typesjekke requesten mot kontrakten |

## Aksjonspunkter

- [ ] Teamet — godkjenn eller forkast ADR-en etter Architecture Advice Process.
- [x] Frontend-teamet — migrer resterende API-eide modeller og dokumenter
  bevisste redigeringsmodeller.
- [x] Frontend-teamet — etabler opt-in npm-basert generering fra lokal backend.
- [ ] Teamet (G2) — etabler CI-vern mot drift: en backend-test som skriver et
  kanonisk `/v3/api-docs` til et innsjekket spec-artifact, og en CI-jobb som
  regenererer frontend-typene og feiler dersom artifactet eller de genererte
  typene avviker. Uten dette feiler ingenting når generert output blir foreldet.
- [ ] Backend-teamet (G4) — rydd i lagre-/hent-kontrakten for vedtak slik at
  lagre-requesten kan typesjekkes mot de genererte typene. Tas som egen endring.
- [ ] Backend-teamet — følg opp `prosessering`-avhengigheten dersom
  `/api/task/**` senere skal inkluderes i OpenAPI, slik at `AvvikshåndterDTO`
  selv eksponerer korrekt Unicode-getter.
- [x] Teamet — oppdater frontend-dokumentasjonen når kilde- og
  regenereringsflyten er fastsatt.

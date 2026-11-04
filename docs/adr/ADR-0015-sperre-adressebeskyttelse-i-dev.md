# ADR-0015: Sperre adressebeskyttelse og skjerming for Infotrygd i dev

**Dato:** 2026-11-04
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

Infotrygd i dev har skarpe data (ADR-0009). Tilgangsmaskinen gir tilgang til en
person med strengt fortrolig eller fortrolig adresse, eller en skjermet person, når
saksbehandleren har gruppen for det. I dev kan alle gi seg selv disse gruppene. Da
slipper hvem som helst forbi kjernereglene og ser ekte personer med
adressebeskyttelse.

Tilgangsmaskinen svarer bare tilgang eller avslag. Den sier ikke om personen har
adressebeskyttelse når saksbehandleren har tilgang, så vi kan ikke avvise på
personen ut fra svaret.

## Beslutning

`apps/infotrygd` avviser alle personoppslag fra saksbehandlere som har én av disse
gruppene i dev:

| Gruppe | ID i `trygdeetaten.no` |
|---|---|
| `0000-GA-Strengt_Fortrolig_Adresse` (også § 19) | `a1b518e2-3947-478d-8929-e5d685c47cac` |
| `0000-GA-Fortrolig_Adresse` | `2e1dc582-f762-4510-a660-88bf68fb7128` |
| `0000-GA-Egne_ansatte` | `dbe4ad45-320b-4e9a-aaa1-73cca4ee124d` |

ID-ene er de samme som tilgangsmaskinen bruker i dev-gcp
(`navikt/populasjonstilgangskontroll`, `.nais/dev-gcp.json`).

- `SperretGruppeIDev` har ID-ene som konstanter, slik `Rolle` har rollegruppene.
- Gruppene står i `claims.groups` i `.nais/dev.yaml`, ellers kommer de ikke med i
  tokenet. `NaisManifestTest` feiler hvis en av dem mangler.
- `TilgangService.validerTilgangTilPersoner` sjekker gruppene først, før
  tilgangsmaskinen. Avslaget gir `403` med en melding som ikke sier noe om
  personen, og auditloggen får utfallet `avslag`.
- `TilgangService.filtrerPersonerMedTilgang` gir tom mengde, slik at
  `migreringspersoner` ikke returnerer noen.
- `SikkerhetContext.hentGrupper()` kaster feil når `groups` mangler eller tokenet har
  overage (`_claim_names` eller `hasgroups`). En tom liste ville åpnet sperren.
- Saksbehandleren må fjerne gruppen for å bruke Infotrygd i dev.

Sperren gjelder bare infotrygd. Sak endres ikke.

## Alternativer vurdert

### Avvise bare personer med adressebeskyttelse

Krever personens gradering fra PDL. I dev betyr det dev-PDL, som ikke har alle
personene i de skarpe dataene. Forkastet.

### Avvise i `AzureJwtAuthenticationConverter`

Stopper alle kall, også `/tabeller`. Avvisningen blir en `AuthenticationException`
og gir `401`, som frontend kan tolke som at saksbehandleren er logget ut. Ingen
auditlinje. Forkastet.

### Gruppe-ID-er som miljøvariabel i `dev.yaml`

Hvis variabelen mangler, er sperren av uten at noen merker det. Forkastet.

### Slå opp gruppemedlemskap i Microsoft Graph

Lukker tidsvinduet under, men gir et nytt kall og mer kode for et dev-miljø.
Forkastet.

## Konsekvenser

### Positive

- Saksbehandlere i dev ser ikke ekte personer med adressebeskyttelse eller
  skjerming, selv om de gir seg selv tilgangen.
- Fødselsnumrene sendes ikke til tilgangsmaskinen når saksbehandleren er sperret.
- Avslagsmeldingen sier ingenting om personen.

### Negative

- Saksbehandlere med én av gruppene kan ikke bruke Infotrygd i dev i det hele tatt.
- Testing av kode 6 og 7 mot Infotrygd må skje med syntetiske data, ikke i dev.

### Risiko

- Tokenet viser gruppene fra da det ble utstedt. Sak cacher OBO-tokenet, og
  tilgangsmaskinen cacher grupper i tre timer. Den som nettopp har gitt seg selv
  gruppen, kan slippe gjennom til tokenet fornyes. Vi godtar det i dev.
- Nais tildeler gruppene i `claims.groups` til appen i Entra. Medlemmer av gruppene
  kan da få token til infotrygd, men uten rollegruppe gir `Rolle`-sjekken `401`.
- Sperren gjelder ikke data som sak har lagret fra Infotrygd, for eksempel ved
  migrering (ADR-0013).
- ID-ene finnes ikke i prod-tenanten, så sperren slår ikke til i prod. Det hviler på
  at ingen legger dev-ID-ene inn i et prod-oppsett.

## Aksjonspunkter

- [ ] Prod-manifest for infotrygd: skill `Rolle` og `SperretGruppeIDev` per miljø.
      Sperren skal ikke ha prod-ID-er. `NaisManifestTest` feiler til dette er gjort.
- [ ] Sjekk at avslagsmeldingen fra infotrygd når fram til Infotrygd-fanen i sak.

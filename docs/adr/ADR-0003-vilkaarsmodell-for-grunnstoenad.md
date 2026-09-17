# ADR-0003: Vilkårsmodell for grunnstønad

**Dato:** 2026-09-16
**Status:** Besluttet
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`apps/sak` er en fork av saksbehandlingsløsningen for gjenlevende og
barnetilsyn, men skal dekke **grunnstønad** etter folketrygdloven kapittel 6.
Vilkårsmodellen var ikke migrert. `VilkårType` inneholdt verdiene
`INNGANGSVILKÅR`, `AKTIVITET`, `INNTEKT`, `ALDER_PÅ_BARN` og
`DOKUMENTASJON_TILSYNSUTGIFTER`.

Ingen av disse gjelder grunnstønad:

- Grunnstønad er **ikke behovsprøvd** — det finnes ikke noe inntektsvilkår.
- Det finnes ingen **aktivitetsplikt**.
- Ytelsen gjelder **medlemmet selv**, ikke barn. `ALDER_PÅ_BARN` og
  `DOKUMENTASJON_TILSYNSUTGIFTER` er artefakter fra barnetilsyn.

Løsningen kunne derfor ikke fatte et rettslig holdbart vedtak om grunnstønad.
Saksbehandler fylte ut fem vilkår som ikke hadde hjemmel i ytelsen som
behandles.

Vi har brukt `navikt/familie-ba-sak` som referanse. Barnetrygd og grunnstønad
er ulike ytelser med ulike vilkår, så det som er relevant å hente derfra er
**mønsteret** for hvordan vilkår modelleres, ikke vilkårene selv.

Beslutningen måtte tas nå fordi vilkårsmodellen er inngangen til vedtaksperioder,
beregning og brev. Hvert steg som bygges på den gamle modellen øker kostnaden
ved å rette den.

### Begrensninger

- Applikasjonen kjører kun i `dev-gcp`. Det finnes ingen produksjonsdata.
- Hjelpestønad (§§ 6-4 og 6-5) er utenfor scope i denne omgang.
- `VilkårType` er en generert TypeScript-union i frontend. Endring av
  enum-verdiene brekker frontend-bygget.

## Beslutning

Vi erstatter de forkede vilkårstypene med vilkår avledet fra folketrygdloven
kapittel 6, og innfører periodisering i backend.

### Vilkår

| Vilkår | Hjemmel |
|---|---|
| `MEDLEM_I_TRYGDEN_ELLER_OMFATTET_AV_EØS_FORORDNINGEN` | ftrl. kapittel 2, jf. § 6-3 første ledd, og § 6-1 a |
| `DIAGNOSE` | § 6-2 |
| `IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM` | § 6-8 |

Medlemskapsvilkåret følger av at § 6-3 første ledd gir ytelsen «til et medlem».
§ 6-1 a fraviker reglene så langt det følger av trygdeforordningen (EF)
883/2004, slik at også den som er omfattet av EØS-forordningen oppfyller
vilkåret. Dette er modellert som feltet `regelverk` på vilkåret, med verdiene
`NASJONALE_REGLER` og `EØS_FORORDNINGEN`, ikke som to ulike vilkår.

Institusjonsvilkåret er bevisst **negert i navnet**. § 6-8 er en
bortfallsbestemmelse, og et vilkår som het `OPPHOLD_I_INSTITUSJON...` ville hatt
`JA` = negativt for søker. Alle andre vilkår har `JA` = positivt. Negeringen
bevarer polariteten, slik at en framtidig «alle vilkår oppfylt»-aggregering ikke
gir feil resultat.

**Nødvendige ekstrautgifter er ikke et vilkår i denne modellen.** Vurderingen
etter § 6-3 hører hjemme i satssteget, se «Beløp og sats flyttes senere i
behandlingsløpet».

### Periodisering

Et vilkår kan ha **flere perioder** per behandling. Hver vilkårstabell har
`fra_og_med_dato` og `til_og_med_dato`, begge nullbare. Åpen `til_og_med_dato`
betyr løpende.

Dette medfører:

- Uthenting per behandling returnerer `List<T>`, ikke `T?`. Den tidligere
  signaturen `findByBehandlingIdAndVilkårType(): VilkårVurdering?` ga
  `IncorrectResultSizeDataAccessException` i runtime så snart det fantes
  duplikater.
- Lagring nøkler på `id` i requesten, ikke på `(behandlingId, vilkårType)`.
  `id == null` betyr ny periode.
- Det finnes et reelt slette-endepunkt for én periode:
  `DELETE /api/vilkar/{behandlingId}/{vilkår}/{vilkårPeriodeId}`.
- Perioder som gjelder samme forhold kan ikke overlappe, men overlapp avvises
  ikke. Den nye eller endrede perioden vinner på tidslinjen i
  `no.nav.familie.tidslinje`, og lagrede perioder den overlapper blir forkortet,
  splittet i to hvis den nye ligger midt inni, eller slettet hvis de dekkes helt.
  Diagnose er unntaket, se under.
- Det legges **ikke** inn unique-constraint på `(behandling_id, …)`. Fraværet er
  en bevisst beslutning, ikke en forglemmelse.

### Tre sidestilte vilkårstabeller, ingen felles vilkårstabell

Hvert vilkår har **sin egen tabell**: `vilkar_medlemskap`, `vilkar_diagnose` og
`vilkar_institusjon`. Det finnes ingen felles `vilkar_vurdering`-tabell med
utdypingstabeller hengende under.

Alle tre bærer de samme fellesfeltene — `id`, `behandling_id`, `vurdering`,
`begrunnelse`, `fra_og_med_dato`, `til_og_med_dato` og `Sporbar`-kolonnene — og
i tillegg de feltene vilkåret faktisk trenger:

| Tabell | Hjemmel | Vilkårsspesifikke felt |
|---|---|---|
| `vilkar_medlemskap` | ftrl. kap. 2, § 6-1 a | `regelverk` (`NASJONALE_REGLER` / `EØS_FORORDNINGEN`) |
| `vilkar_diagnose` | § 6-2, § 6-9 | `diagnose` (fritekst), `er_yrkesskade` |
| `vilkar_institusjon` | § 6-8 | `oppholdstype`, `unntakshjemmel` |

Med bare tre vilkår gir ikke en felles `VilkårVurdering`-klasse nok igjen til å
bære kostnaden. Den tvang fram enten et diskriminatorfelt med CHECK-constraints,
eller barnetabeller med sammensatte fremmednøkler — begge deler for å uttrykke
noe strukturen kan uttrykke direkte. Fellesskapet som faktisk finnes, ligger i
*livssyklusen*, ikke i *dataene*, og den deles i kode:

- `VilkårPeriode` / `VilkårPeriodeRequest` — grensesnittene med fellesfeltene.
- `VilkårPeriodeService` — abstrakt baseservice med redigerbarhetssjekk,
  ansvarlig saksbehandler, forkorting ved overlapp og endringshistorikk.
- `VilkårTidslinje` — felles forkorting ved overlapp og datovalidering.
- `VilkårPeriodeRepository` — felles `findByBehandlingId`.

Referansemodellen i familie-ba-sak bruker ett felles listefelt
(`utdypendeVilkårsvurderinger`) for alle vilkår. Den modellen krever
runtime-validering i `UtdypendeVilkårsvurderingerUtils`, fordi ingenting hindrer
at en utdyping settes på et vilkår den ikke gjelder for. Med egne tabeller er
det databasestrukturen som håndhever gyldigheten.

| | Felles tabell + utdypinger | Tre sidestilte tabeller ✅ |
|---|---|---|
| Gyldighet per vilkår | Diskriminator + CHECK | Databasestruktur |
| Strukturerte data | Krever JSON eller nytt felt | Egne kolonner |
| Antall tabeller | 1 + 3 | 3 |
| Særskilt tilgangsstyring | Vanskelig | Per tabell |
| Deling av logikk | Arv i én klasse | Abstrakt baseservice |

**Diagnose** er i tillegg begrunnet i personvern: dette er helseopplysning etter
**GDPR artikkel 9**. Egen tabell gir egen tilgangsstyring, eget auditspor og
mulighet til å holde opplysningen utenfor generiske vilkårsresponser, logger og
eksport. ICD-10/ICPC-2 er heller ikke en lukket mengde, og komorbiditet gjør
flere diagnoser per behandling til normalsituasjonen. Diagnosen lagres som
`String` inntil videre; vi bygger ikke kodeverksintegrasjon eller validering nå,
ut over at tom eller whitespace-only verdi avvises i service og av CHECK-
constrainten `vilkar_diagnose_ikke_tom`.

Fordi flere diagnoser kan løpe samtidig, gjelder overlappforbudet for diagnose
**kun innenfor samme diagnose** (sammenlignet trimmet og case-insensitivt). To
ulike diagnoser kan dekke samme periode.

Yrkesskade (§ 6-9) lagres som flagget `er_yrkesskade` på diagnoseraden, ikke som
eget vilkår. Det er en egenskap ved diagnosen. Skadedatoen er radens
`fra_og_med_dato`; vi har ikke et eget `yrkesskade_dato`-felt. Merk at
rettsvirkningen av § 6-9 lander et annet sted enn lagringen: bestemmelsen lemper
på medlemskapskravet, og må derfor leses av utledningen av vedtaksperioder.

`vurdering`-feltet lagres per rad, men det finnes **ingen aggregeringslogikk**
som utleder «alle vilkår oppfylt». Det er bevisst utsatt.

### Tekstkolonner bruker `TEXT`

Nye Flyway-migreringer bruker `TEXT`, ikke `VARCHAR(n)`. PostgreSQL gir ingen
ytelsesgevinst av lengdebegrensningen, og en lengde som må endres senere koster
en migrering. Der en faktisk regel finnes, uttrykkes den som CHECK-constraint.
Eldre migreringer røres ikke, siden det ville brutt Flyway-checksums.


### `Vurdering` beholdes som `JA`/`NEI`

Vi innfører ikke `IKKE_VURDERT`. Med periodisering opprettes en rad først når
noe faktisk er vurdert, slik at **fravær av rad betyr ikke vurdert**. Det gir
mindre å migrere og færre tilstander å håndtere.

### Eksisterende data slettes

Migreringen `V35` kjører `DROP TABLE` på `vilkar_vurdering` og den gamle
`vilkar_diagnose` før de tre nye tabellene opprettes. Radene hadde vilkårstyper
som ikke finnes i grunnstønad, og en omforming ville gitt rettslig meningsløse
vurderinger. Det finnes ingen produksjonsdata.

Eldre migreringer røres ikke, selv om de oppretter tabeller vi nå dropper.
Å redigere dem ville brutt Flyway-checksums for miljøer som allerede har kjørt
dem.

## Avgrensninger

Følgende er bevisst holdt utenfor denne ADR-en:

| Tema | Hjemmel | Hvor det hører hjemme |
|---|---|---|
| Nødvendige ekstrautgifter | § 6-3 første ledd | Satssteget |
| Beløpsvurdering og sats | § 6-3 tredje ledd | Satssteget |
| Aldersgrense ved transportutgifter | § 6-3 fjerde ledd | Satssteget, sammen med utgiftstypene |
| Reduksjon ved manglende trygdetid | § 6-6 | Sammen med satsene |
| Revurdering | § 6-7 | `ÅrsakBehandling` |
| Rettsvirkningen av yrkesskade | § 6-9 | Utledning av vedtaksperioder |
| Aggregering av «alle vilkår oppfylt» | — | Utledning av vedtaksperioder |
| Hjelpestønad | §§ 6-4, 6-5 | Egen ytelse |

Merk at yrkesskade *lagres* i vilkårsmodellen som `er_yrkesskade` på
diagnoseraden, men at bestemmelsens rettsvirkning — lemping av
medlemskapskravet — hører til utledningen av vedtaksperioder.

### Ekstrautgifter og sats flyttes ut av vilkårssteget

Nødvendige ekstrautgifter etter § 6-3 var opprinnelig modellert som et eget
vilkår i denne ADR-en. Det er forlatt. Både **hvilke** utgiftstyper som
kvalifiserer og **hvor mye** de utgjør vurderes i satssteget.

Begrunnelsen er at de to spørsmålene ikke lar seg skille i praksis: en
utgiftstype uten beløp avgjør ingenting, og saksbehandler vurderer dem i samme
operasjon. Å splitte dem over to steg ville gitt et vilkår som alltid måtte
leses sammen med et annet steg for å gi mening.

To konsekvenser følger, og begge må håndteres når satssteget bygges:

1. **Vilkårssteget kan ikke konkludere med innvilgelse alene.** Alle tre vilkår
   kan være oppfylt og saken likevel ende i avslag fordi ekstrautgiftene ligger
   under laveste sats. Dagens frontend antar det motsatte:
   `alleVilkårHarSvar` i `VilkårInnhold.tsx` styrer `settErVilkårUtfylt`.
2. **Hjemmelen må følge med.** § 6-3 tredje ledd sier ordrett «Det er et vilkår
   for rett til grunnstønad at …». Å flytte vurderingen er en prosessbeslutning,
   ikke en rettslig omklassifisering. Vedtaksbrevet må fortsatt hjemle avslag i
   § 6-3 tredje ledd. Brevet utledes etter at satsene er satt.

### Enum-verdiene for institusjon må fagverifiseres

`Oppholdstype` og `Unntakshjemmel` er utledet av § 6-8 og Navs rundskriv R06-00,
ikke av en fagvurdering i teamet. § 6-8 nevner bare «institusjon» og «en boform
med heldøgns omsorg og pleie» i lovteksten selv; inndelingen i
`HELSE_OG_OMSORGSINSTITUSJON`, `SPESIALISTHELSETJENESTEN`, `PSYKISK_HELSEVERN`
og `FENGSEL_ELLER_ANNEN_LOVREGULERT_BOFORM` er vår tolkning.

Det samme gjelder unntakene. `KORTTIDSOPPHOLD` og
`BARN_UNDER_18_I_SPESIALISTHELSETJENESTEN` følger av forskriftshjemmelen i § 6-8
andre ledd slik rundskrivet omtaler den, ikke av lovteksten. Særlig
`BARN_UNDER_18_I_SPESIALISTHELSETJENESTEN` må vurderes mot om ytelsesløpet vårt
i det hele tatt dekker barn.

`EKSTRAUTGIFTER_IKKE_DEKKET_AV_INSTITUSJONEN` er dessuten ikke en ren
vilkårsvurdering: den begrenser hvilke utgifter som kan legges til grunn.
Satssteget må lese flagget, ikke bare vilkårets `vurdering`.

Verdiene må avstemmes med fag før vilkåret tas i bruk i saksbehandling.

## Alternativer vurdert

### Alternativ A: Vilkår avledet fra ftrl. kap. 6, med mønster fra familie-ba-sak ✅ (valgt)

**Beskrivelse:** Definere vilkårene direkte fra folketrygdloven kapittel 6, og
låne den strukturelle mekanismen fra familie-ba-sak som faktisk trengs
— periodisering med tidslinje — men ikke vilkårene, persondimensjonen eller den
felles vilkårstabellen. Hvert vilkår får sin egen tabell, og fellesskapet
uttrykkes i kode gjennom en abstrakt baseservice.

**Fordeler:**

- Vilkårene har hjemmel i den ytelsen som faktisk behandles.
- Vi låner en periodiseringsmodell som er prøvd i produksjon.
- Omfanget er avgrenset til det grunnstønad trenger; persondimensjon og
  aggregering kan legges til når behovet er reelt.
- Diagnose får en plassering som er forenlig med GDPR artikkel 9.
- Hvert vilkår får gyldighet håndhevet av databasestrukturen i stedet for
  runtime-validering mot et diskriminatorfelt.

**Ulemper:**

- Bryter frontend-kontrakten og krever en frontend-migrering.
- Enum-verdiene for institusjon må fagverifiseres før vilkåret tas i bruk.
- Vilkårssteget alene kan ikke avgjøre utfallet av saken.

**Nav-vurdering:** Dette er den minste endringen som gjør vilkårsvurderingen
rettslig korrekt. Den følger essential complexity ved å utelate
persondimensjon og EØS-koordinering som barnetrygd trenger, men som
grunnstønad ikke har behov for i dag.

### Alternativ B: Kopiere familie-ba-sak sin vilkårsmodell

**Beskrivelse:** Ta inn `Vilkårsvurdering`, `PersonResultat`, `VilkårResultat`,
`Regelverk` og felles `utdypendeVilkårsvurderinger` mer eller mindre som de er,
og bytte ut enum-verdiene.

**Fordeler:**

- Velprøvd modell med periodisering, tidslinjer og regelverksstøtte.
- Samme begrepsapparat som et annet Nav-team allerede bruker.

**Ulemper:**

- `PersonResultat` modellerer vilkår per person i en søker/barn-struktur.
  Grunnstønad gjelder ett medlem, så dimensjonen ville stått tom.
- Arver det felles utdypingsfeltet og behovet for runtime-validering.
- Drar inn JPA-baserte entiteter i en kodebase som bruker Spring Data JDBC.
- Persondimensjonen er reell kompleksitet vi ikke trenger.

**Nav-vurdering:** Accidental complexity. Modellen er formet av barnetrygdens
persondimensjon, som grunnstønad ikke har.

### Alternativ C: Beholde eksisterende vilkårstyper og tolke dem om

**Beskrivelse:** La `INNGANGSVILKÅR` bety medlemskap, `AKTIVITET` bety sykdom
og så videre, uten å endre enum eller database.

**Fordeler:**

- Ingen migrering, ingen frontend-endring.
- Raskest vei til noe som ser komplett ut.

**Ulemper:**

- Navn i kode, database og endringshistorikk ville ikke stemt med det som
  faktisk vurderes.
- Auditspor og endringshistorikk ville vært misvisende for en ytelse som fatter
  enkeltvedtak etter forvaltningsloven.
- Gjelden ville vokst inn i vedtak, beregning og brev.

**Nav-vurdering:** Forkastet. Sporbarhet i saksbehandling er et krav, ikke en
preferanse.

### Alternativ D: Gjøre ingenting

**Beskrivelse:** Beholde de forkede vilkårstypene som de er.

**Fordeler:**

- Ingen endringskostnad nå.
- Frontend fortsetter å kompilere.

**Ulemper:**

- Løsningen kan ikke fatte et rettslig holdbart vedtak om grunnstønad.
- Saksbehandler vurderer vilkår uten hjemmel i ytelsen.
- Kostnaden øker for hvert steg som bygges på modellen.

**Nav-vurdering:** Ikke et reelt alternativ når ytelsen er bestemt.

## Nav-spesifikke vurderinger

### Arkitektur

- Endringen er avgrenset til `vilkår`-pakken i `apps/sak` og de tilhørende
  Flyway-migreringene. Ingen nye tjenester eller integrasjoner.
- Vi gjenbruker eksisterende mønstre i kodebasen: `RepositoryInterface`,
  `InsertUpdateRepository`, `Sporbar` og `EndringshistorikkService`.
- Hvert vilkår får sin egen tabell og sin egen service, mens den delte
  livssyklusen ligger i `VilkårPeriodeService`. Det som er felles er oppførsel,
  ikke data.
- Å utelate ekstrautgifter, sats og aggregering fra vilkårsmodellen holder hvert
  behandlingssteg ansvarlig for én ting.

### Sikkerhet og personvern

- **Dataklassifisering:** Diagnoseopplysninger er særlige kategorier av
  personopplysninger etter GDPR artikkel 9 og behandles som strengt fortrolig.
  Øvrige vilkårsdata er fortrolige.
- **Auth-mekanisme:** Uendret. Azure AD for saksbehandler, med
  `@PreAuthorize("hasRole('SAKSBEHANDLER')")` på hver av de tre kontrollerne
  `VilkårMedlemskapController`, `VilkårDiagnoseController` og
  `VilkårInstitusjonController`.
- **Tilgangsstyring:** Eksisterende `tilgangskontroll` og `felles/auditlogger`
  gjelder uendret. Egen diagnosetabell gjør det mulig å legge på strengere
  tilgang senere uten å endre de to andre vilkårene.
- **PII-håndtering:** Diagnose skal ikke logges. `VilkårPeriodeService` skriver
  `detaljer = "$vilkårType: $vurdering"` til endringshistorikken. Feltet må
  aldri utvides med diagnosetekst. Dette er dokumentert som et eksplisitt
  forbud i KDoc på metoden, slik at det overlever en refaktorering.
- Diagnose skal ikke eksponeres i generiske vilkårsresponser uten at behovet er
  vurdert særskilt. Hver vilkårstype har derfor sin egen response-type.

### Plattform (Nais/GCP)

- **Infrastrukturkrav:** Eksisterende PostgreSQL. Ingen nye Nais-ressurser,
  nettverkstillatelser eller secrets.
- **Ressursbehov:** Uendret. Tabellene er små og skrives kun av saksbehandler.
- **Observerbarhet:** Ingen nye metrikker. Overlapp løses ved at tidslinjen i
  `no.nav.familie.tidslinje` forkorter de lagrede periodene, mens ugyldig
  datorekkefølge avvises av samme tidslinje og mappes til HTTP 400 av
  `ApiExceptionHandler`. Databasen har i tillegg CHECK-constraints som bakstopp.
- **CI/CD-endringer:** Ingen. `sak-build.yaml` og `sak-deploy-dev.yaml` dekker
  endringen. Frontend-migreringen går gjennom `frontend-build.yaml` som egen PR.

### Team og organisasjon

- **Berørte team:** Kun teamet som eier `sak` og `frontend`. Ingen eksterne
  konsumenter av `/api/vilkar`.
- **Migrasjonsstrategi:** Backend først, frontend i egen PR.
- **Tilbakerulling:** Revert av kode og migrering i én endring. Ingen datatap av
  betydning siden dataene slettes uansett.
- **Tidsramme:** Backend-endringen bør ligge foran arbeidet med vedtaksperioder
  og sats.

### Migrasjon

- **Bakoverkompatibilitet:** Nei. `VilkårType`-verdiene byttes ut, og
  `/api/vilkar` endrer kontrakt med `id` og datofelter. Dette er akseptabelt
  fordi løsningen kun kjører i `dev-gcp` uten eksterne konsumenter.
- **Utrullingsstrategi:** Big bang i backend. Frontend følger i egen PR.
  Frontend-bygget vil feile i mellomperioden, se risikotabellen.
- **Feature toggle:** Ikke aktuelt. Toggling ville krevd at begge
  vilkårsmodellene fantes samtidig, noe som er dyrere enn selve endringen.
- **Rollback-trigger:** Migrering som feiler, eller vilkårsmodell som ikke lar
  seg avstemme mot § 6-2, § 6-3 og § 6-8 i fagvurdering.
- **Exit criteria:** De tre vilkårene kan opprettes, periodiseres, endres og
  slettes gjennom API-et; diagnoser kan registreres; frontend kompilerer og
  saksbehandler kan fullføre vilkårssteget.
- **Dekommisjonering:** De fem forkede vilkårstypene er fjernet fra backend.
  Frontend-komponentene `Aktivitet.tsx`, `AlderPåBarn.tsx`, `Inntekt.tsx`,
  `DokumentasjonTilsynsutgifter.tsx` og `Inngangsvilkår.tsx`, samt
  `useVilkårVurdering.ts` og `app/types/vilkår.ts`, fjernes når
  frontend-migreringen er ferdig.

## Konsekvenser

### Positive

- Vilkårene har hjemmel i folketrygdloven kapittel 6, og endringshistorikk og
  auditspor blir meningsfulle.
- Periodisering gjør det mulig å behandle § 6-8-bortfall og endringer over tid.
- Diagnose får en plassering som tåler kravene til særlige kategorier
  personopplysninger, og flere diagnoser kan løpe samtidig.
- Hvert vilkår kan utvides med egne felter uten å røre de to andre.
- Modellen er mindre enn familie-ba-sak sin, uten å låse oss fra å legge til
  persondimensjon eller aggregering senere.

### Negative

- Frontend brekker til migreringen er gjennomført.
- `Oppholdstype` og `Unntakshjemmel` må fagverifiseres før institusjonsvilkåret
  kan brukes i saksbehandling.
- Vilkårssteget alene avgjør ikke utfallet; satssteget må levere avslagsløpet
  for § 6-3 tredje ledd.
- Feilmeldingene ved ugyldige datoer kommer fra tidslinjebiblioteket og er ikke
  formulert i saksbehandlerens språk.
- Forkorting av naboperioder skjer stille. Saksbehandleren ser resultatet i
  periodelista, men får ingen varsling om at en lagret periode ble endret eller
  fjernet.

### Risiko

| Risiko | Sannsynlighet | Konsekvens | Mitigering |
|--------|---------------|------------|------------|
| Diagnose havner i logg, endringshistorikk eller generisk API-respons | Middels | Høy | Egen tabell, egen response-type, eksplisitt forbud i KDoc på `VilkårPeriodeService.registrerEndring` |
| Frontend-bygget står rødt for lenge | Middels | Middels | Frontend-migrering er registrert som egen oppgave og bør tas umiddelbart etter backend |
| Institusjonsvilkåret tas i bruk med ufagverifiserte enum-verdier | Middels | Høy | Dokumentert i KDoc og i «Enum-verdiene for institusjon må fagverifiseres»; aksjonspunkt til fag |
| Overlappende perioder lagres | Lav | Middels | Tidslinjen forkorter lagrede perioder ved overlapp, dekket av test, og CHECK-constraints i databasen |
| § 6-3 tredje ledd glemmes i satssteget, så avslag mangler hjemmel | Lav | Høy | Eksplisitt aksjonspunkt og hjemmelsreferanse i brevbygging |

## Teknisk gjeld

| # | Gjeldspost | Alv. | Frekv. | Nedslagsfelt | Prioritet | Status |
|---|-----------|------|--------|--------------|-----------|--------|
| G1 | Vilkårstyper uten hjemmel i grunnstønad | 3 | 3 | 3 | 27 | Løst — `VilkårType` erstattet, gamle tabeller droppet i `V35` |
| G2 | Uthenting returnerte ett treff uten at databasen garanterte det | 3 | 2 | 1 | 6 | Løst — `findByBehandlingId` returnerer `List<T>` |
| G3 | `fra_og_med_dato`/`til_og_med_dato` fantes i databasen, men var ubrukt | 2 | 2 | 2 | 8 | Løst — i bruk i entitet, request og response |
| G4 | Sletting av vilkår fantes kun i klientstate | 2 | 2 | 2 | 8 | Løst — `DELETE /api/vilkar/{behandlingId}/{vilkår}/{vilkårPeriodeId}` |
| G5 | Frontend antar én vurdering per vilkårstype i `Record<VilkårType, VilkårState>` | 2 | 2 | 1 | 4 | Åpen — håndteres i frontend-migreringen |

## Aksjonspunkter

- [ ] Teamet — godkjenn eller forkast ADR-en etter Architecture Advice Process.
- [ ] Fag — verifiser verdiene i `Oppholdstype` og `Unntakshjemmel` mot § 6-8 og
  rundskriv R06-00, inkludert om `BARN_UNDER_18_I_SPESIALISTHELSETJENESTEN`
  er relevant for vårt ytelsesløp. Blokkerer bruk av institusjonsvilkåret.
- [x] Backend (G1) — erstatt `VilkårType` med de tre vilkårene, med
  Flyway-migrering som sletter eksisterende rader.
- [x] Backend (G2, G3, G4) — innfør periodisering: liste-retur fra repository,
  lagring nøklet på `id`, datofelter i request og response, slette-endepunkt og
  forkorting av overlappende perioder.
- [x] Backend — opprett `vilkar_diagnose` med støtte for flere diagnoser,
  diagnose som `String`, og `er_yrkesskade` der skadedatoen er `fra_og_med_dato`.
- [x] Backend — verifiser at diagnose ikke havner i `detaljer` i
  `EndringshistorikkService` eller i logger.
- [ ] Frontend — migrer vilkårskomponenter og hook til de tre nye endepunktene,
  og regenerer `app/api/generated/types.gen.ts`. Egen PR.
- [ ] Teamet — sørg for at satssteget dekker § 6-3 tredje ledd som
  avslagshjemmel når det bygges, og at det leser
  `EKSTRAUTGIFTER_IKKE_DEKKET_AV_INSTITUSJONEN` fra institusjonsvilkåret.
- [ ] Teamet — oppdater `apps/sak/README.md` med vilkårsmodellen når den er
  implementert.

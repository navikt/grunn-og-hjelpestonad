# ADR-0003: Vilkårsmodell for grunnstønad

**Dato:** 2026-09-16
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad

## Kontekst

`apps/sak` er en fork av saksbehandlingsløsningen for gjenlevende og
barnetilsyn, men skal dekke **grunnstønad** etter folketrygdloven kapittel 6.
Vilkårsmodellen er ikke migrert. `VilkårType` inneholder fortsatt verdiene
`INNGANGSVILKÅR`, `AKTIVITET`, `INNTEKT`, `ALDER_PÅ_BARN` og
`DOKUMENTASJON_TILSYNSUTGIFTER`.

Ingen av disse gjelder grunnstønad:

- Grunnstønad er **ikke behovsprøvd** — det finnes ikke noe inntektsvilkår.
- Det finnes ingen **aktivitetsplikt**.
- Ytelsen gjelder **medlemmet selv**, ikke barn. `ALDER_PÅ_BARN` og
  `DOKUMENTASJON_TILSYNSUTGIFTER` er artefakter fra barnetilsyn.

Løsningen kan derfor ikke fatte et rettslig holdbart vedtak om grunnstønad i
dag. Saksbehandler fyller ut fem vilkår som ikke har hjemmel i ytelsen som
behandles.

Vi har brukt `navikt/familie-ba-sak` som referanse. Barnetrygd og grunnstønad
er ulike ytelser med ulike vilkår, så det som er relevant å hente derfra er
**mønsteret** for hvordan vilkår modelleres, ikke vilkårene selv.

Beslutningen må tas nå fordi vilkårsmodellen er inngangen til vedtaksperioder,
beregning og brev. Hvert steg som bygges på dagens modell øker kostnaden ved å
rette den.

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
| `VARIG_SYKDOM_SKADE_ELLER_LYTE` | § 6-2 |
| `NØDVENDIGE_EKSTRAUTGIFTER` | § 6-3 første ledd |
| `IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM` | § 6-8 |

Institusjonsvilkåret er bevisst **negert i navnet**. § 6-8 er en
bortfallsbestemmelse, og et vilkår som het `OPPHOLD_I_INSTITUSJON...` ville hatt
`OPPFYLT` = negativt for søker. Alle andre vilkår har `OPPFYLT` = positivt.
Negeringen bevarer polariteten, slik at en framtidig «alle vilkår oppfylt»-
aggregering ikke gir feil resultat.

### Periodisering

Et vilkår kan ha **flere perioder** per behandling. Kolonnene
`fra_og_med_dato` og `til_og_med_dato` finnes allerede i `vilkar_vurdering`
(migrering `V33`), men brukes verken i service, request eller response.

Dette medfører:

- `findByBehandlingIdAndVilkårType` må returnere `List<VilkårVurdering>`.
  I dag returnerer den `VilkårVurdering?`, og duplikater gir
  `IncorrectResultSizeDataAccessException` i runtime.
- Lagring må nøkle på `id` i requesten, ikke på `(behandlingId, vilkårType)`.
  `id == null` betyr ny periode.
- Det må finnes et reelt slette-endepunkt for én periode. `slettVilkår` i
  `useVilkårVurdering.ts` nullstiller i dag bare klientstate.
- Perioder for samme vilkårstype kan ikke overlappe. Åpen `til_og_med_dato`
  betyr løpende.
- Det legges **ikke** inn unique-constraint på `(behandling_id, vilkar_type)`.
  Fraværet er nå en bevisst beslutning, ikke en forglemmelse.

### Utdypende vilkårsvurdering i egne tabeller per vilkår

Utdypende vilkårsvurderinger lagres i **én tabell per vilkår**, knyttet til den
enkelte vilkårsperioden. Vi bruker ikke ett felles felt delt av alle vilkår.

| Tabell | Vilkår | Innhold |
|---|---|---|
| `vilkar_diagnose` | `VARIG_SYKDOM_SKADE_ELLER_LYTE` | Diagnoser med yrkesskadeflagg |
| `vilkar_ekstrautgift` | `NØDVENDIGE_EKSTRAUTGIFTER` | Utgiftstyper etter § 6-3 første ledd bokstav a–h |
| `vilkar_institusjonsopphold` | `IKKE_OPPHOLD_I_INSTITUSJON_ELLER_LOVREGULERT_BOFORM` | Oppholdstype og unntakshjemmel etter § 6-8 |

Referansemodellen i familie-ba-sak bruker ett felles listefelt
(`utdypendeVilkårsvurderinger`) for alle vilkår. Den modellen krever
runtime-validering i `UtdypendeVilkårsvurderingerUtils`, fordi ingenting hindrer
at en utdyping settes på et vilkår den ikke gjelder for. Vi ønsker ikke å arve
den svakheten.

| | Felles listefelt (ba-sak) | Egne tabeller per vilkår ✅ |
|---|---|---|
| Gyldighet per vilkår | Runtime-validering | Databasestruktur |
| Strukturerte data | Krever JSON eller nytt felt | Egne kolonner |
| Antall tabeller | 1 | 3 |
| Særskilt tilgangsstyring | Vanskelig | Per tabell |

Tre tabeller er ikke en tabelleksplosjon, og utdypingene har genuint forskjellig
form. Vi forventer også at de trenger strukturerte data over tid — for eksempel
beløp per utgiftstype når satssteget bygges, og hjemmelsreferanse på
institusjonsoppholdet. En felles `kode`-kolonne måtte da vike uansett.

Periodiseringen gjør utdypingsradene enklere: de trenger ikke egne datoer, fordi
vilkårsperioden de henger på allerede bærer dem.

**Diagnose** er i tillegg begrunnet i personvern: dette er helseopplysning etter
**GDPR artikkel 9**. Egen tabell gir egen tilgangsstyring, eget auditspor og
mulighet til å holde opplysningen utenfor generiske vilkårsresponser, logger og
eksport. ICD-10/ICPC-2 er heller ikke en lukket mengde, og komorbiditet gjør
flere diagnoser per vurdering til normalsituasjonen. Diagnosekoden lagres som
`String` inntil videre; vi bygger ikke kodeverksintegrasjon eller validering nå.

Yrkesskade (§ 6-9) lagres som flagg med dato på diagnoseraden, ikke som eget
vilkår. Det er en egenskap ved diagnosen. Merk at rettsvirkningen av § 6-9
lander et annet sted enn lagringen: bestemmelsen lemper på medlemskapskravet,
og må derfor leses av utledningen av vedtaksperioder.

Aldersgrensen i § 6-3 fjerde ledd er heller ikke et eget vilkår, men en utdyping
knyttet til hver periode der transportutgifter inngår.

### `Vurdering` beholdes som `JA`/`NEI`

Vi innfører ikke `IKKE_VURDERT`. Med periodisering opprettes en rad først når
noe faktisk er vurdert, slik at **fravær av rad betyr ikke vurdert**. Det gir
mindre å migrere og færre tilstander å håndtere.

### Eksisterende data slettes

Migreringen kjører `DELETE FROM vilkar_vurdering`. Radene har vilkårstyper som
ikke finnes i grunnstønad, og en omforming ville gitt rettslig meningsløse
vurderinger. Det finnes ingen produksjonsdata.

## Avgrensninger

Følgende er bevisst holdt utenfor denne ADR-en:

| Tema | Hjemmel | Hvor det hører hjemme |
|---|---|---|
| Medlemskap i folketrygden | § 6-3, § 6-1 a, § 6-9 | Utledning av vedtaksperioder |
| Beløpsvurdering og sats | § 6-3 tredje ledd | Eget steg senere i behandlingsløpet |
| Aldersgrense ved transportutgifter | § 6-3 fjerde ledd | Utdyping på `vilkar_ekstrautgift`, ikke eget vilkår |
| Reduksjon ved manglende trygdetid | § 6-6 | Sammen med satsene |
| Revurdering | § 6-7 | `ÅrsakBehandling` |
| Regelverk og EØS-koordinering | § 6-1 a | Senere iterasjon |
| Hjelpestønad | §§ 6-4, 6-5 | Egen ytelse |

### Beløp og sats flyttes senere i behandlingsløpet

Skillet er at **hvilke** utgiftstyper som kvalifiserer etter § 6-3 første ledd
er en rettslig subsumsjon som hører til vilkårssteget, mens **hvor mye**
utgiftene utgjør er skjønn som hører til satssteget.

To konsekvenser følger, og begge må håndteres når satssteget bygges:

1. **Vilkårssteget kan ikke konkludere med innvilgelse alene.** Alle vilkår kan
   være oppfylt og saken likevel ende i avslag fordi ekstrautgiftene ligger
   under laveste sats. Dagens frontend antar det motsatte:
   `alleVilkårHarSvar` i `VilkårInnhold.tsx` styrer `settErVilkårUtfylt`.
2. **Hjemmelen må følge med.** § 6-3 tredje ledd sier ordrett «Det er et vilkår
   for rett til grunnstønad at …». Å flytte vurderingen er en prosessbeslutning,
   ikke en rettslig omklassifisering. Vedtaksbrevet må fortsatt hjemle avslag i
   § 6-3 tredje ledd. Brevet utledes etter at satsene er satt.

### Utformingen av utdypingstabellene er ikke besluttet her

At utdypende vilkårsvurderinger går i egne tabeller per vilkår er besluttet over.
Det som gjenstår er **innholdet i hver tabell**: kolonner, enum-verdier for
utgiftstypene i § 6-3 første ledd bokstav a–h og unntakene i § 6-8, og hvordan
aldersgrensen i § 6-3 fjerde ledd knyttes til en transportperiode.

Dette tas som egen beslutning før de to avhengige vilkårene implementeres.
`vilkar_diagnose` er ikke blokkert av dette og kan bygges nå.

## Alternativer vurdert

### Alternativ A: Vilkår avledet fra ftrl. kap. 6, med mønster fra familie-ba-sak ✅ (valgt)

**Beskrivelse:** Definere vilkårene direkte fra folketrygdloven kapittel 6, og
låne de strukturelle mekanismene fra familie-ba-sak som faktisk trengs
(periodisering og utdypende vurdering), men ikke vilkårene, persondimensjonen
eller regelverksdimensjonen. Utdypende vurdering legges i egne tabeller per
vilkår i stedet for ba-sak sitt felles listefelt.

**Fordeler:**

- Vilkårene har hjemmel i den ytelsen som faktisk behandles.
- Vi låner en modell som er prøvd i produksjon for en tilsvarende
  vilkårsvurdering.
- Omfanget er avgrenset til det grunnstønad trenger; EØS, personresultat og
  regelverk kan legges til når behovet er reelt.
- Diagnose får en plassering som er forenlig med GDPR artikkel 9.
- Utdypende vilkårsvurderinger får gyldighet håndhevet av databasestrukturen i
  stedet for runtime-validering.

**Ulemper:**

- Bryter frontend-kontrakten og krever en frontend-migrering.
- Krever en ny beslutning om innholdet i utdypingstabellene før modellen er
  komplett.
- Vilkårssteget alene kan ikke lenger avgjøre utfallet av saken.

**Nav-vurdering:** Dette er den minste endringen som gjør vilkårsvurderingen
rettslig korrekt. Den følger essential complexity ved å utelate
persondimensjon, regelverk og EØS-koordinering som barnetrygd trenger, men som
grunnstønad ikke har behov for i dag.

### Alternativ B: Kopiere familie-ba-sak sin vilkårsmodell

**Beskrivelse:** Ta inn `Vilkårsvurdering`, `PersonResultat`, `VilkårResultat`,
`Regelverk` og felles `utdypendeVilkårsvurderinger` mer eller mindre som de er,
og bytte ut enum-verdiene.

**Fordeler:**

- Velprøvd modell med periodisering, tidslinjer og regelverksstøtte.
- Gir EØS-koordinering etter § 6-1 a uten ekstra arbeid senere.
- Samme begrepsapparat som et annet Nav-team allerede bruker.

**Ulemper:**

- `PersonResultat` modellerer vilkår per person i en søker/barn-struktur.
  Grunnstønad gjelder ett medlem, så dimensjonen ville stått tom.
- Arver det felles utdypingsfeltet og behovet for runtime-validering.
- Drar inn JPA-baserte entiteter i en kodebase som bruker Spring Data JDBC.
- Regelverk og EØS er reell kompleksitet vi ikke trenger ennå.

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
- Utdypende vilkårsvurdering legges i egne tabeller per vilkår, mens innholdet i
  hver tabell skilles ut som egen beslutning framfor å låses nå.
- Å utelate medlemskap, regelverk og sats fra vilkårsmodellen holder hvert
  behandlingssteg ansvarlig for én ting.

### Sikkerhet og personvern

- **Dataklassifisering:** Diagnoseopplysninger er særlige kategorier av
  personopplysninger etter GDPR artikkel 9 og behandles som strengt fortrolig.
  Øvrige vilkårsdata er fortrolige.
- **Auth-mekanisme:** Uendret. Azure AD for saksbehandler, med
  `@PreAuthorize("hasRole('SAKSBEHANDLER')")` på `VilkårVurderingController`.
- **Tilgangsstyring:** Eksisterende `tilgangskontroll` og `felles/auditlogger`
  gjelder uendret. Egen diagnosetabell gjør det mulig å legge på strengere
  tilgang senere uten å endre vilkårsmodellen.
- **PII-håndtering:** Diagnosekode og fritekstbeskrivelse skal ikke logges.
  Dette er en konkret risiko i dagens kode: `VilkårVurderingService` skriver
  `detaljer = "${request.vilkårType}: ${request.vurdering}"` til
  endringshistorikken. Feltet må aldri utvides med diagnosetekst, og
  endringshistorikk for diagnose må registreres uten innhold.
- Diagnose skal ikke eksponeres i generiske vilkårsresponser uten at behovet er
  vurdert særskilt.

### Plattform (Nais/GCP)

- **Infrastrukturkrav:** Eksisterende PostgreSQL. Ingen nye Nais-ressurser,
  nettverkstillatelser eller secrets.
- **Ressursbehov:** Uendret. Tabellene er små og skrives kun av saksbehandler.
- **Observerbarhet:** Ingen nye metrikker. Feil ved lagring av overlappende
  perioder skal gi validerings­feil med tydelig melding, ikke
  databaseunntak.
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
- **Dekommisjonering:** De fem forkede vilkårstypene og
  frontend-komponentene `Aktivitet.tsx`, `AlderPåBarn.tsx`, `Inntekt.tsx`,
  `DokumentasjonTilsynsutgifter.tsx` og `Inngangsvilkår.tsx` fjernes når
  frontend-migreringen er ferdig.

## Konsekvenser

### Positive

- Vilkårene har hjemmel i folketrygdloven kapittel 6, og endringshistorikk og
  auditspor blir meningsfulle.
- Periodisering gjør det mulig å behandle § 6-8-bortfall og endringer over tid.
- Diagnose får en plassering som tåler kravene til særlige kategorier
  personopplysninger.
- Modellen er mindre enn familie-ba-sak sin, uten å låse oss fra å legge til
  regelverk og EØS senere.

### Negative

- Frontend brekker til migreringen er gjennomført.
- Vilkårsmodellen er ikke komplett før innholdet i utdypingstabellene er
  besluttet.
- Vilkårssteget alene avgjør ikke lenger utfallet; satssteget må levere
  avslagsløpet for § 6-3 tredje ledd.
- Manglende regelverksdimensjon må legges til når § 6-1 a blir aktuell.

### Risiko

| Risiko | Sannsynlighet | Konsekvens | Mitigering |
|--------|---------------|------------|------------|
| Diagnose havner i logg, endringshistorikk eller generisk API-respons | Middels | Høy | Egen tabell, eksplisitt forbud i denne ADR-en, og review av `detaljer`-feltet i `EndringshistorikkService` |
| Frontend-bygget står rødt for lenge | Middels | Middels | Frontend-migrering er registrert som egen oppgave og bør tas umiddelbart etter backend |
| Innholdet i utdypingstabellene blir utsatt, så § 6-3 fjerde ledd og § 6-8-unntak mangler | Middels | Middels | Beslutningen tas før de to avhengige vilkårene implementeres; `vilkar_diagnose` er ikke blokkert |
| Overlappende perioder lagres | Lav | Middels | Valideringsregel i service, med test |
| § 6-3 tredje ledd glemmes i satssteget, så avslag mangler hjemmel | Lav | Høy | Eksplisitt aksjonspunkt og hjemmelsreferanse i brevbygging |

## Teknisk gjeld

| # | Gjeldspost | Alv. | Frekv. | Nedslagsfelt | Prioritet | Anbefalt tiltak |
|---|-----------|------|--------|--------------|-----------|-----------------|
| G1 | Vilkårstyper uten hjemmel i grunnstønad | 3 | 3 | 3 | 27 | Erstatt enum og slett eksisterende rader |
| G2 | `findByBehandlingIdAndVilkårType` returnerer ett treff uten at databasen garanterer det | 3 | 2 | 1 | 6 | Returner liste som del av periodiseringen |
| G3 | `fra_og_med_dato`/`til_og_med_dato` finnes i databasen, men brukes ikke | 2 | 2 | 2 | 8 | Ta i bruk i service, request og response |
| G4 | Sletting av vilkår finnes kun i klientstate, ikke som endepunkt | 2 | 2 | 2 | 8 | Innfør slette-endepunkt for én periode |
| G5 | Frontend antar én vurdering per vilkårstype i `Record<VilkårType, VilkårState>` | 2 | 2 | 1 | 4 | Håndteres i frontend-migreringen |

## Aksjonspunkter

- [ ] Teamet — godkjenn eller forkast ADR-en etter Architecture Advice Process.
- [ ] Teamet — beslutt innholdet i `vilkar_ekstrautgift` og
  `vilkar_institusjonsopphold`: kolonner og enum-verdier. Blokkerer
  § 6-3 første ledd bokstav a–h, § 6-3 fjerde ledd og § 6-8-unntakene.
- [ ] Backend (G1) — erstatt `VilkårType` med de tre vilkårene, med
  Flyway-migrering som sletter eksisterende rader.
- [ ] Backend (G2, G3, G4) — innfør periodisering: liste-retur fra repository,
  lagring nøklet på `id`, datofelter i request og response, slette-endepunkt og
  validering mot overlappende perioder.
- [ ] Backend — opprett `vilkar_diagnose` med støtte for flere diagnoser,
  diagnosekode som `String`, og yrkesskadeflagg med dato.
- [ ] Backend — verifiser at diagnose ikke havner i `detaljer` i
  `EndringshistorikkService` eller i logger.
- [ ] Frontend — migrer vilkårskomponenter og hook til de nye vilkårstypene, og
  regenerer `app/api/generated/types.gen.ts`. Egen PR.
- [ ] Teamet — sørg for at satssteget dekker § 6-3 tredje ledd som
  avslagshjemmel når det bygges.
- [ ] Teamet — oppdater `apps/sak/README.md` med vilkårsmodellen når den er
  implementert.

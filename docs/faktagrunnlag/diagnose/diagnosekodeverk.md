# Kodeverk for diagnosevilkåret

I diagnosevilkåret velger saksbehandler diagnosen som en kode fra ICD-10, se
[ADR-0003](../../adr/ADR-0003-vilkaarsmodell-for-grunnstoenad.md). Dette
dokumentet forklarer hvorfor vi bruker ICD-10 og ikke ICPC-2 eller ICD-11. Det
handler om språk, hvor mye kodeverkene endrer seg, tilgang, lisens og hva
valget betyr for lagringen hos oss. Til slutt beskriver det hvordan vi har
tatt i bruk ICD-10.

Begrepene er definert i [CONTEXT.md](../../../CONTEXT.md). Opplysningene om
kodeverkene er hentet 8. oktober 2026, se [Kilder](#kilder).

## Kort fortalt

- **Valgt:** Vi bruker bare ICD-10. Infotrygd bruker bare ICD-10 for
  grunnstønad og hjelpestønad, og vi gjør det samme.
- Leger i Norge koder i dag med ICD-10 i spesialisthelsetjenesten og med ICPC-2
  hos fastleger og legevakt. Har legeerklæringen en ICPC-2-kode, velger
  saksbehandler ICD-10-koden som passer.
- Det finnes ingen norsk ICD-11 ennå. Helsedirektoratet har som foreløpig mål
  at helsetjenesten kan ta i bruk ICD-11 i 2028–2029. Den endelige nasjonale
  tidsplanen er ikke satt.
- Den norske utgaven av ICD-10 oppdateres ikke lenger, med unntak av
  feilrettinger og ekstraordinære endringer. WHO gir ut en ny utgave av ICD-11
  hvert år.
- Vi bruker Navs felles pakke
  [navikt/diagnosekoder](https://github.com/navikt/diagnosekoder). AAP bruker
  den allerede i saksbehandlingsløsningen sin. Vi har ikke en egen kopi av
  kodeverket i databasen.
- Vi lagrer ikke hvilket kodeverk koden kommer fra. Når vi tar inn ICD-11, må vi
  legge til en kolonne for kodeverk med en migrering.

## Hva vi bruker diagnosen til

1. **Dokumentasjon.** Saksbehandler viser hvilken diagnose vurderingen gjelder.
2. **Hva som er «samme diagnose».** Overlappforbudet og tidslinjen per diagnose
   grupperer på koden. Se
   [DiagnoseRegler](../../../apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/vilkår/diagnose/DiagnoseRegler.kt)
   og
   [MedlemskapOgDiagnoseTidslinje](../../../apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/steg/vilkårVurdering/MedlemskapOgDiagnoseTidslinje.kt).
3. **Faste satser.** Noen diagnoser, for eksempel cøliaki, gir en fast sats, se
   [vilkår.md](../../vilkår/vilkår.md). Med kode kan systemet kjenne igjen disse
   diagnosene senere.

## Kodeverkene

| | ICD-10 ✅ | ICPC-2 | ICD-11 (MMS) |
|---|---|---|---|
| Forvalter | Helsedirektoratet (norsk utgave) | Helsedirektoratet | WHO. Helsedirektoratet oversetter. |
| Brukes av | Spesialisthelsetjenesten, Infotrygd for grunnstønad og hjelpestønad | Fastleger og legevakt | Ingen i Norge ennå |
| Norsk tekst | Ja | Ja | Nei |
| Endringer | Fryst, bare feilrettinger | Hvert år, tas i bruk 1. januar | Ny utgave hvert år |
| Tilgang | FinnKode, navikt/diagnosekoder | FinnKode, navikt/diagnosekoder | WHOs ICD-API, i skyen eller som container |
| Lisens | Må bekreftes | Må bekreftes | CC BY-ND 3.0 IGO |

## Hvorfor bare ICD-10

Infotrygd bruker bare ICD-10 for grunnstønad og hjelpestønad. Med samme
kodeverk kan vi sammenligne diagnoser i saker som flyttes fra Infotrygd, og
saksbehandlerne jobber på samme måte som før.

ICD-10 er også mer detaljert enn ICPC-2. ICPC-2 har 708 koder, mens ICD-10 har
19 634. Med ett kodeverk har hver sykdom bare én kode, så overlappforbudet
fanger opp at samme sykdom vurderes to ganger. Med både ICD-10 og ICPC-2 ville
ADHD vært både `F900` og `P81`, og systemet ville sett dem som to
diagnoser.

Ulempen er at saksbehandler må finne ICD-10-koden selv når legeerklæringen fra
fastlegen har en ICPC-2-kode. Helsedirektoratet har en konverteringstabell fra
ICPC-2 til ICD-10 som kan hjelpe. Vi vet ikke om alle legeerklæringer for
grunnstønad og hjelpestønad har en diagnosekode. Se
[Åpne spørsmål](#åpne-spørsmål).

## ICD-11

### Språk

WHOs ICD-API har ICD-11 MMS 2026-01 på blant annet engelsk, tysk og svensk,
men ikke på norsk. Velger vi ICD-11 nå, ser saksbehandler diagnosene på engelsk
eller svensk.

Vi kan ikke oversette selv. Lisensen tillater ikke bearbeidinger uten egen
avtale med WHO, og en oversettelse regnes som en bearbeiding. Helsedirektoratet
oversetter ICD-11 til norsk. Så langt har de mest jobbet med kapittel 6 om
psykiske lidelser, som er om lag en fjerdedel av teksten.

### Oversetting fra legeerklæringen

Legen koder med ICD-10 eller ICPC-2. Velger vi ICD-11 nå, må saksbehandler
oversette koden fra legen til ICD-11. Det er ekstra arbeid og krever kunnskap
om koding. Det gir også rom for feil, fordi en kode i ICD-10 ikke alltid svarer
til én kode i ICD-11.

### Endringer

WHO gir ut en ny utgave av ICD-11 hvert år og publiserer hva som er endret.
WHO beskriver 2026-utgaven som presisert klinisk innhold og flere
tilleggskoder. Hele kodeverket endres ikke hvert år, men enkeltkoder og
tekster kan bli lagt til eller endret. Kildene sier ikke hvor stor del som
vanligvis endres.

Bruker vi ICD-11, må vi derfor lagre hvilken utgave koden er hentet fra.

### Kombinerte koder

I ICD-11 kan en kode kombineres med tilleggskoder som sier noe om for eksempel
alvorlighetsgrad, årsak eller lokasjon. Det kalles postkoordinering.
Helsedirektoratet bruker «Sporadic Parkinson disease» (`8A00.00`) som eksempel.
Koden kan utvides med alvorlighetsgrad, varighet og demens.

En diagnose er da ikke lenger én kode, men en kombinasjon av koder. Vi må
avgjøre om vi tillater kombinasjoner. Vi må også avgjøre om to kombinasjoner
med samme hovedkode er «samme diagnose». Tillater vi bare hovedkoder, slipper
vi spørsmålene, men vi mister presisjon.

### Tilgang

| Måte | Hvordan | Merk |
|---|---|---|
| WHOs ICD-API i skyen | OAuth2 med klient-ID og -hemmelighet. Tokenet varer om lag én time. | Kall fra backend, ikke fra nettleseren. Søkeordene sendes til WHO. Vi blir avhengige av at WHOs tjeneste er oppe. |
| WHOs ICD-API som container (`whoicd/icd-api`) | Kjører hos oss, for eksempel i Nais. Trenger ikke OAuth eller internett etter oppstart. | Vi må drifte containeren. Sett `saveAnalytics=false`. Containeren har ikke ICD-10. |
| Egen kopi i databasen | Vi synkroniserer kodeverket inn i egne tabeller. | Krever kode for synkronisering og migrering for noe containeren gir oss ferdig. |

Velger vi ICD-11, er containeren det beste alternativet. Søkeordene blir hos
oss, og vi slipper både hemmeligheten og avhengigheten til WHO.

### Lisens

ICD-11 har lisensen CC BY-ND 3.0 IGO. Vi kan bruke og vise kodeverket, også i
saksbehandlingen, så lenge vi oppgir WHO som kilde. Vi kan ikke lage
bearbeidinger, som oversettelser, uten egen avtale med WHO. Containeren krever
at vi godtar lisensen ved oppstart (`acceptLicense=true`). Lisensen bør
vurderes av noen med juridisk kompetanse før vi tar ICD-11 i bruk.

## ICD-10

### Endringer

Den norske ICD-10 er i praksis fryst. Helsedirektoratet endrer den bare ved
feil eller ekstraordinære forhold.

Vi må likevel tåle at en kode vi har lagret, ikke finnes i neste utgave.
navikt/diagnosekoder filtrerer bort koder som er utløpt. Derfor kontrollerer vi
bare koden når saksbehandler lagrer en ny eller endret vurdering. Eldre
vurderinger viser vi med teksten vi lagret.

### navikt/diagnosekoder

Pakken finnes både for npm og Kotlin. Den inneholder kode og tekst for ICD-10
og ICPC-2, og den lages fra Helsedirektoratets kodeverks-API. Det kommer en ny
versjon når kodeverkene oppdateres. Teamet for K9-saksbehandling forvalter
pakken. Vi bruker bare ICD-10 fra pakken.

Versjon `1.2026.0` har 19 634 koder i ICD-10. Den er en JSON-fil på 1,9 MB.

AAP bruker npm-pakken i
[DiagnoseSøker.tsx](https://github.com/navikt/aap-saksbehandling/blob/main/lib/diagnosesøker/DiagnoseSøker.tsx).
Saksbehandler velger kodeverk og søker på kode eller tekst. Søket kjører i
nettleseren, så ingen søkeord sendes ut.

Kotlin-pakken har også Infotrygds nummer for kodeverkene, `3` for ICD-10.

Med Kotlin-pakken kan backend både søke og kontrollere koden uten å kalle noen
tjeneste. Frontend søker gjennom et endepunkt i backend, så vi slipper å sende
1,9 MB med ICD-10 til nettleseren. Da finnes kodeverket bare ett sted, og vi
oppdaterer det ved å bumpe én versjon. Vi kunne også kalt
Helsedirektoratets API direkte. Da får vi alltid nyeste versjon, men vi blir
avhengige av en tjeneste til. For et stabilt kodeverk er pakken nok.

## Lagring

I `vilkar_diagnose` er kolonnen `diagnose` erstattet av disse kolonnene
(`V40__diagnosekode_i_vilkaar_diagnose.sql`):

| Kolonne | Eksempel | Hvorfor |
|---|---|---|
| `kode` | `F900` | ICD-10-koden uten punktum. Koden er diagnosens identitet. |
| `tekst` | `Forstyrrelser av aktivitet og oppmerksomhet` | Teksten fra kodeverket da saksbehandler valgte koden. Da kan vi vise eldre vurderinger selv om kodeverket endres. |

Kolonnene er `TEXT`. Frontend sender bare koden. Backend kontrollerer at koden
finnes i ICD-10 i pakken og henter teksten derfra. En diagnose må alltid ha en
kode. Eksisterende vurderinger ble slettet i migreringen, siden ingenting var i
produksjon.

ICD-10 i navikt/diagnosekoder har bare de mest detaljerte kodene, uten punktum.
Hyperkinetiske forstyrrelser er `F900` til `F909`, og kategorien `F90` finnes
ikke. ADHD er `F900`. Får søker også en atferdsforstyrrelse, kan koden bli
`F901` (hyperkinetisk atferdsforstyrrelse). Vi grupperer på
hele koden, så da blir det to diagnoser. Grupperer vi på kategorien (de tre
første tegnene), blir det én. Fag har ikke bestemt dette ennå, se
[Åpne spørsmål](#åpne-spørsmål).

## Søk

Frontend søker gjennom `POST /api/diagnosekoder/sok` med søketeksten i body.
Søket bruker POST og ikke GET fordi søketeksten kan være en diagnose. I en URL
ville den havnet i tracing (Grafana Faro i frontend og OpenTelemetry i backend)
og i tilgangslogger.

Backend rangerer treffene slik:

1. Koden er lik søketeksten. Punktum, mellomrom og små bokstaver ignoreres, så
   `f90.0` treffer `F900`.
2. Koden starter med søketeksten.
3. Teksten inneholder alle ordene i søketeksten og starter med det første.
4. Teksten inneholder alle ordene i søketeksten.

Backend returnerer høyst 50 treff. Frontend viser kodene med punktum, for
eksempel «F90.0 Forstyrrelser av aktivitet og oppmerksomhet».

## Personvern

- Diagnosekoden er en helseopplysning etter GDPR artikkel 9, akkurat som
  teksten. Den skal heller ikke inn i logger eller i endringshistorikken, se
  `registrerEndring` i
  [VilkårPeriodeService](../../../apps/sak/src/main/kotlin/no/nav/grunn/og/hjelpestonad/vilkår/VilkårPeriodeService.kt).
- Med navikt/diagnosekoder eller en egen container forlater ikke søkeordene
  Nav. Med WHOs API i skyen sendes søkeordene til WHO. De er ikke knyttet til en
  person, men vi bør unngå det når vi har et alternativ.

## Overgang til ICD-11

Når helsetjenesten går over til ICD-11, kommer legeerklæringene gradvis med
ICD-11-koder. Da må vi legge til kolonnene `kodeverk` og `kodeverksversjon`
med en migrering. Eksisterende rader får `kodeverk = 'ICD10'`. «Samme diagnose»
blir da `(kodeverk, kode)`. Eldre vurderinger beholder ICD-10-koden. Ved
revurdering velger saksbehandler kode på nytt.

Hvis Infotrygd fortsatt bruker ICD-10 når vi tar inn ICD-11, må vi også avgjøre
hvordan vi sammenligner diagnoser i saker som flyttes fra Infotrygd.

WHO har tabeller mellom ICD-10 og ICD-11, men kodene svarer ikke én til én.
Tabellene kan hjelpe saksbehandler, men de kan ikke oversette lagrede
vurderinger automatisk. Statistikk på tvers av årene må ta hensyn til begge
kodeverkene.

## Alternativer

| Alternativ | Fordeler | Ulemper |
|---|---|---|
| ICD-10 med navikt/diagnosekoder ✅ | Norsk tekst. Samme kodeverk som Infotrygd. Én kode per sykdom. Ingen tjeneste å kalle. Brukes allerede i Nav. | Saksbehandler må finne ICD-10-koden når legeerklæringen har ICPC-2. Krever migrering når ICD-11 kommer. |
| ICD-10 og ICPC-2 | Saksbehandler kan bruke koden fra legeerklæringen direkte. | Samme sykdom kan ha kode i begge kodeverkene. Avviker fra Infotrygd. |
| ICD-11 med WHOs API i skyen | Kodeverket helsetjenesten skal over på | Ikke norsk. Saksbehandler må oversette fra legeerklæringen. Avhengig av WHO, hemmelighet å forvalte, og søkeordene sendes til WHO. |
| ICD-11 med WHOs container | Som over, men uten avhengighet til WHO | Ikke norsk. Vi må drifte containeren. |
| Egen kopi av kodeverket i databasen | Full kontroll | Kode for synkronisering og migrering for noe pakken eller containeren gir oss ferdig. |

## Åpne spørsmål

- Har legeerklæringene for grunnstønad og hjelpestønad diagnosekode, og i
  hvilket kodeverk? Saksbehandler kan i dag ikke registrere en diagnose uten
  kode. AAP har et eget valg for «Ingen diagnose».
- Hvordan finner saksbehandlerne ICD-10-koden i Infotrygd når legeerklæringen
  har en ICPC-2-kode? Trenger de hjelp til det i den nye løsningen?
- Skal «samme diagnose» være hele koden eller kategorien? Vi bruker hele koden
  til fag har bestemt seg.
- Hvor i Infotrygd ligger diagnosen? Den finnes ikke i tabellene vi replikerer
  i `apps/infotrygd`.
- Hvilken lisens har ICD-10? Sekundærkilder oppgir Norsk lisens for offentlige
  data (NLOD), men vi har ikke bekreftet det i en primærkilde.
- Skal faste satser knyttes til diagnosekoder?

## Kilder

- WHO: [ICD-API versjon 2](https://icd.who.int/docs/icd-api/APIDoc-Version2/),
  [støttede versjoner og språk](https://icd.who.int/docs/icd-api/SupportedClassifications/),
  [autentisering](https://icd.who.int/docs/icd-api/API-Authentication/),
  [ICD-API som container](https://icd.who.int/docs/icd-api/ICDAPI-DockerContainer/),
  [ICD-11 2026](https://www.who.int/news/item/16-02-2026-icd-11-2026-release),
  [lisens for ICD-11](https://icd.who.int/en/docs/ICD11-license.pdf)
- Helsedirektoratet:
  [ICD-10 og ICD-11](https://www.helsedirektoratet.no/digitalisering-og-e-helse/helsefaglige-kodeverk/icd),
  [ICPC-2](https://www.helsedirektoratet.no/digitalisering-og-e-helse/helsefaglige-kodeverk/icpc),
  [om overgangen til ICD-11](https://www.helsedirektoratet.no/digitalisering-og-e-helse/helsefaglige-kodeverk/icd/om-overgangen-til-icd-11-i-norge),
  [status og fremdrift](https://www.helsedirektoratet.no/digitalisering-og-e-helse/helsefaglige-kodeverk/icd/status-og-fremdrift-i-arbeidet-med-overgang-til-icd-11),
  [dette er nytt i ICD-11](https://www.helsedirektoratet.no/digitalisering-og-e-helse/helsefaglige-kodeverk/icd/dette-er-nytt-i-icd-11),
  [FinnKode](https://finnkode.helsedirektoratet.no/)
- Nav: [navikt/diagnosekoder](https://github.com/navikt/diagnosekoder),
  [DiagnoseSøker i aap-saksbehandling](https://github.com/navikt/aap-saksbehandling/blob/main/lib/diagnosesøker/DiagnoseSøker.tsx)
- Teamet: Infotrygd bruker bare ICD-10 for grunnstønad og hjelpestønad.

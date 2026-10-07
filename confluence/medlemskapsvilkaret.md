# Medlemskapsvilkåret i grunnstønad

**Status:** Utkast til diskusjon med fag
**Gjelder:** Grunnstønad og hjelpestønad etter folketrygdloven kapittel 6
**Målgruppe:** Fagpersoner, saksbehandlere og teamet

Siden beskriver to ting:

1. Hvilket grunnlag vi henter for å vurdere om søker er medlem i folketrygden.
2. Hvilke regler vi bruker for å komme fram til en vurdering ut fra grunnlaget.

Det viktigste er å bli enige om grunnlaget. Reglene kommer etterpå og blir
innført i steg.

## Avgrensning

- **Bare nasjonale regler.** Siden gjelder medlemskap etter kapittel 2 i
  folketrygdloven. Grunnstønad og hjelpestønad er ytelser ved sykdom etter
  trygdeforordningen (§ 6-1 a), men EØS-reglene er ikke beskrevet her ennå.
  Perioder der EØS-reglene kan gjelde, vurderer saksbehandler manuelt.
- **Yrkesskade vurderes i et annet vilkår.** Etter § 6-9 kan søker få
  grunnstønad og hjelpestønad ved yrkesskade selv om hen ikke lenger er medlem.
  Det er nok at søker var medlem da yrkesskaden oppsto. I slike saker ser vi
  bort fra medlemskapsvilkåret.

## Steg for steg

Vi går fra helt manuell vurdering til gradvis mer automatisering. Hver regel
lenger ned er merket med steget den hører til.

| Steg | Hva systemet gjør | Hva saksbehandler gjør |
|---|---|---|
| 1 (MVP) | Henter grunnlaget og viser det | Vurderer alle periodene |
| 2 | Foreslår JA i klare tilfeller: søker er bosatt og nordisk statsborger og har ingenting i MEDL, eller MEDL har gyldig trygdedekning for kapittel 6 | Godtar eller endrer forslaget, og vurderer resten |
| 3 | Foreslår NEI ut fra gyldige unntaksperioder i MEDL | Som over |
| 4 | Foreslår JA etter § 2-2 for arbeidstakere i Norge, ut fra Aa-registeret og A-ordningen | Som over |
| 5 | Foreslår JA for tredjelandsborgere med lovlig opphold, ut fra opphold i PDL | Som over |
| Senere | EØS, regler for åpne saker i Gosys og Joark, Lånekassen og særreglene i «Punkter til senere» | |

Systemet foreslår, men saksbehandler bestemmer. Perioder systemet ikke kan
avgjøre, får ingen vurdering, bare en grunn til at de må vurderes manuelt.

## Hva loven sier

### Kapittel 6 krever bare medlemskap

Grunnstønad og hjelpestønad gis til «et medlem» (§§ 6-3 og 6-4). Kapittel 6
har verken krav om forutgående medlemskap eller krav om opphold i Norge. Det
skiller oss fra:

- **Enslig mor eller far (kapittel 15):** krav om fem års forutgående
  medlemskap (§ 15-2) og om opphold i Norge eller et annet EØS-land (§ 15-3).
- **Sykdom i familien (kapittel 9):** krav om opphold (§ 9-4) og opptjeningstid
  i arbeid (§ 9-2).

For oss er spørsmålet derfor bare: **Var søker medlem i folketrygden med
trygdedekning for kapittel 6 i perioden?**

### Medlemskap etter kapittel 2

| Paragraf | Hva den sier | Betydning for oss |
|---|---|---|
| § 2-1 | Den som er bosatt i Norge, er pliktig medlem. Bosatt er den som oppholder seg i Norge i minst 12 måneder. Medlemskapet krever lovlig opphold. Fravær under 12 måneder endrer ikke bostedet. | Hovedregelen. Vi leser bosted fra Folkeregisteret (PDL). |
| § 2-2 | Den som ikke er bosatt, men er arbeidstaker i Norge, er pliktig medlem hvis hen har lovlig adgang til arbeid. | Kan avgjøres med Aa-registeret og A-ordningen (steg 4). |
| § 2-5 | Noen er pliktige medlemmer selv om de er i utlandet, for eksempel statsansatte, militære og studenter med lån fra Lånekassen. | Registreres i MEDL. |
| § 2-6 | Utenlandske statsborgere på norske skip og fly er medlemmer bare ved yrkesskade og dødsfall. | Gir ikke trygdedekning for kapittel 6. |
| § 2-7 og § 2-7 a | Frivillig medlemskap i Norge. Dekningen er enten full (bokstav a) eller bare kapittel 5, 8, 9 og 14 (bokstav b). | Bare bokstav a dekker kapittel 6. |
| § 2-8 og § 2-9 | Frivillig medlemskap utenfor Norge. Dekningen etter § 2-9 er bokstav a (kapittel 5, 7 og § 14-17), bokstav b (blant annet kapittel 6) eller bokstav c (a og b). | Bare bokstav b og c dekker kapittel 6. |
| § 2-10 | Begrenset medlemskap gir bare de ytelsene medlemskapet omfatter. | Derfor må vi se på trygdedekningen, ikke bare om søker er medlem. |
| § 2-11, § 2-12 og § 2-13 andre ledd | Unntak fra pliktig medlemskap. | Registreres i MEDL. |
| § 2-13 første ledd | For den som bosatte seg i Norge etter 1992 og har utenlandsk pensjon, dekker medlemskapet kapittel 6 bare i perioder med pensjonsgivende inntekt eller pensjon fra folketrygden. | Vurderes manuelt. Se «Punkter til senere». |
| § 2-14 | Medlemskap etter § 2-1 opphører ved opphold i utlandet over 12 måneder. Medlemskap etter § 2-2 varer i opptil en måned etter at arbeidsforholdet er slutt. | Brukes i regelen for § 2-2. |
| § 2-16 | Forskrift om asylsøkere. | Vurderes manuelt. |
| § 2-17 | Vilkårene for medlemskap anses ikke oppfylt under varetekt, soning og lignende, med mindre søker var medlem da frihetsberøvelsen startet. | Vurderes manuelt. |

**Trygdedekning** betyr hvilke kapitler i folketrygdloven medlemskapet gir rett
til ytelser etter. Bare trygdedekning som omfatter kapittel 6, gir medlemskap
for grunnstønad og hjelpestønad.

## Del 1: Grunnlaget

Grunnlaget hentes når behandlingen opprettes, og saksbehandler kan hente det på
nytt. Vi lagrer det slik kilden leverte det, og tolker det først når vilkåret
vurderes. Da kan vi endre reglene uten å endre det som er lagret.

### Oversikt

| Kilde | Hva | Tidsrom | Brukes i reglene fra |
|---|---|---|---|
| PDL (Folkeregisteret) | Personstatus, bostedsadresse, statsborgerskap, opphold, inn- og utflytting, land for oppholdsadresse | Hele historikken | Steg 2 |
| PDL (felles for behandlingen) | Dødsfall | Hele historikken | Steg 2 |
| MEDL (medlemskapsregisteret) | Alle unntaksperioder for medlemskap, med alle statuser | Alle perioder | Steg 2 |
| Aa-registeret | Arbeidsforhold | Fra tre måneder før kravet til i dag | Steg 4 |
| A-ordningen | Inntekt per måned og arbeidsgiver | Fra tre måneder før kravet til i dag | Steg 4 |
| Gosys og Joark | Åpne oppgaver og journalposter om medlemskap | Det som er åpent nå | Senere |
| Søknaden | Opplysninger fra søker | – | Leses av saksbehandler |

Grunnstønad kan gis fra tidligst tre måneder før kravet ble satt fram
(§ 22-13 tredje ledd), og vilkåret vurderes fra det tidspunktet. Fra PDL og
MEDL henter vi likevel alt, fordi saksbehandler ofte trenger å se hva som
skjedde før.

### PDL

Medlemskap følger i utgangspunktet av bosted, og bostedet står i
Folkeregisteret.

| Opplysning | Hva vi tar med | Hvorfor |
|---|---|---|
| Personstatus | Status og periode | Viser om søker er bosatt, utflyttet, har D-nummer (midlertidig eller inaktiv) eller er død |
| Bostedsadresse | Om adressen er norsk, utenlandsk eller ukjent, kommune eller land, og periode. Ikke gate og nummer. | Viser om og når søker bor i Norge |
| Statsborgerskap | Land og periode | Avgjør om søker er nordisk statsborger, EØS-borger eller tredjelandsborger |
| Opphold | Type oppholdstillatelse og periode, fra UDI | Grunnlag for å vurdere lovlig opphold (§ 2-1 tredje ledd) |
| Innflytting og utflytting | Dato, land og sted | Viser saksbehandler når og hvorfra søker flyttet |
| Oppholdsadresse | Bare landet, og bare hvis adressen er utenlandsk | Kan vise at søker har vært borte i mer enn 12 måneder selv om hen er registrert som bosatt. Vises bare, brukes ikke i reglene. |
| Dødsfall | Dødsdato | Avslutter tidslinjen. Felles grunnlag for hele behandlingen. |

Dette henter vi ikke fra PDL, og hvorfor:

| Opplysning | Hvorfor ikke |
|---|---|
| Kontaktadresse | Det er en postadresse og sier lite om hvor søker bor |
| Fødselsnummer eller D-nummer | Personstatus viser det allerede |
| Fødested og fødeland | Vi har ikke krav om forutgående medlemskap |
| Sivilstand og familierelasjoner | Medlemskap som følger av familien (§§ 2-5, 2-7 og 2-8), står i MEDL |
| Utenlandsk identifikasjonsnummer | Svakt signal. Vurderes på nytt når EØS kommer. |

### MEDL

MEDL registrerer unntak fra hovedregelen om medlemskap etter bosted. Vanlig
medlemskap for folk som bor i Norge, står normalt ikke i MEDL. **At søker ikke
har noe i MEDL, betyr altså ikke at søker ikke er medlem.**

Vi henter alle unntaksperioder for medlemskap, også avviste og uavklarte, slik
at saksbehandler ser hele bildet. For hver periode tar vi med:

| Felt | Betydning |
|---|---|
| Fra og til | Perioden |
| Status og statusårsak | Om registreringen gjelder: gyldig (`GYLD`), uavklart (`UAVK`) eller avvist (`AVST`) |
| Lovvalg og lovvalgsland | Om lovvalget er endelig (`ENDL`), foreløpig (`FORL`) eller under avklaring (`UAVK`), og hvilket land |
| Medlem | Om søker er medlem eller ikke i perioden |
| Dekning | Trygdedekningen. Se «Trygdedekning for kapittel 6». |
| Grunnlag | Hjemmel eller avtale perioden er registrert etter |
| Kilde | Hvem som registrerte perioden, for eksempel Melosys eller Lånekassen |
| Helsedel | Om perioden gjelder helsedelen |

### Aa-registeret og A-ordningen

Brukes til å avgjøre om søker er medlem etter § 2-2 som arbeidstaker i Norge.

| Kilde | Hva vi tar med |
|---|---|
| Aa-registeret | Arbeidsforhold med arbeidsgiver, type (ordinært, maritimt, frilanser), periode og eventuelle utenlandsopphold |
| A-ordningen | Pensjonsgivende inntekt per måned og arbeidsgiver |

Kildene kan vise at søker *er* arbeidstaker i Norge. De kan ikke vise at søker
*ikke* er det. De gir derfor aldri forslag om NEI.

### Gosys og Joark

Åpne oppgaver i Gosys og journalposter i Joark med tema medlemskap (`MED`),
unntak fra medlemskap (`UFM`) eller trygdeavgift (`TRY`). De viser at søker kan
ha en sak om medlemskap som ikke er ferdig, og som derfor ikke står i MEDL ennå.
LovMe bruker det samme signalet. Reglene for dette kommer senere. Til da viser
vi opplysningene til saksbehandler.

### Søknaden

Søker oppgir i søknaden blant annet om hen er fast bosatt i Norge, og om hen
har pensjon fra utlandet. Saksbehandler leser dette. Reglene bruker det ikke
før søknaden er strukturert.

### Kilder vi kan ta i bruk senere

- **Lånekassen**: studieopplysninger for studenter i utlandet (§ 2-5 bokstav
  h). Perioder Lånekassen har registrert i MEDL, vurderes manuelt til da.
- **UDI direkte**: arbeidsadgang for tredjelandsborgere (§ 2-2 andre ledd).
  LovMe henter dette fra UDI.

## Del 2: Reglene

### Slik vurderer vi

Vilkåret vurderes per periode, ikke for hele behandlingen. Hver opplysning blir
en tidslinje, og en ny periode begynner der én av dem endrer seg. Periodene
dekker hele tidsrommet fra tidligste mulige virkningstidspunkt, uten hull.
Søkers død avslutter tidslinjen.

For hver periode går vi gjennom reglene i denne rekkefølgen og stopper ved
første regel som avgjør:

1. MEDL
2. Bosted og statsborgerskap fra PDL
3. Arbeid i Norge etter § 2-2

I steg 1 vurderer saksbehandler alle periodene selv, men med det samme
grunnlaget.

### 1. MEDL

**Hvilke unntaksperioder teller.** Vi følger LovMe:

| Unntaksperiode | Resultat |
|---|---|
| Status `GYLD` og lovvalg `ENDL` | Avgjør perioden, se tabellen under |
| Status `UAVK`, eller lovvalg `FORL` eller `UAVK` | Manuell vurdering: MEDL må avklares |
| Status `AVST` (avvist) | Ignoreres, men vises for saksbehandler |
| Ingen unntaksperioder | PDL avgjør |

**Gyldige unntaksperioder:**

| Gyldig unntaksperiode i perioden | Forslag | Steg |
|---|---|---|
| Trygdedekning for kapittel 6 | JA | 2 |
| Trygdedekning som ikke omfatter kapittel 6, inkludert `Unntatt` | NEI | 3 |
| Både perioder med og uten trygdedekning for kapittel 6 samtidig | Manuell vurdering | 2 |
| Registrert av Lånekassen | Manuell vurdering | 2 |
| Ingen dekningskode, ukjent kode eller kode som må vurderes manuelt | Manuell vurdering | 2 |
| Ikke medlem, og lovvalgsland er USA, Canada eller Papua Ny-Guinea | Manuell vurdering | 3 |

En gyldig unntaksperiode går foran PDL, både for JA og for NEI. MEDL
registrerer nettopp unntakene fra medlemskap etter bosted, så at MEDL og PDL er
uenige, er forventet.

Vi tar utgangspunkt i LovMe, men LovMe er laget for sykepenger og andre
ytelser til arbeidstakere, og de har andre unntak enn oss. LovMe krever blant
annet at unntaksperioden dekker en kontrollperiode på 12 måneder, og at
arbeidsforholdet er uendret. Det trenger ikke vi, fordi vi vurderer hver
periode for seg og ikke har krav om arbeid.

#### Trygdedekning for kapittel 6

**Til kontroll hos fag.** Tabellen er laget ut fra ordlyden i §§ 2-6 til 2-9.

| Dekningskode i MEDL | Dekker kapittel 6? | Hvorfor |
|---|---|---|
| `Full` | Ja | Full trygdedekning |
| `FTL_2-7_3_ledd_a`, `FTL_2-7a_2_ledd_a` | Ja | Full trygdedekning ved frivillig medlemskap i Norge |
| `FTL_2-9_1_ledd_b`, `FTL_2-9_b` | Ja | § 2-9 bokstav b omfatter kapittel 6 |
| `FTL_2-9_1_ledd_c`, `FTL_2-9_c`, `FTL_2-9_2_ld_jfr_1c`, `FTL_2-9_2_ld_3_ld_jfr_1c`, `FTL_2-9_3_ld_jfr_1c` | Ja | § 2-9 bokstav c omfatter bokstav b |
| `FTL_2-9_3_ld_jfr_1b` | Ja | Bokstav b med særfordeler ved yrkesskade |
| `FTL_2-6` | Nei | Bare yrkesskade og dødsfall |
| `FTL_2-7_3_ledd_b`, `FTL_2-7a_2_ledd_b` | Nei | Bare kapittel 5, 8, 9 og 14 |
| `Helsetjenester_sykepenger_sykdom_i_familie_svangerskap_fødsel_adopsjon` | Nei | Bare kapittel 5, 8, 9 og 14 |
| `FTL_2-9_1_ledd_a`, `FTL_2-9_a`, `FTL_2-9_2_ld_jfr_1a` | Nei | § 2-9 bokstav a omfatter ikke kapittel 6. Tillegget for sykepenger og foreldrepenger endrer ikke det. |
| `Unntatt` | Nei | Unntatt fra medlemskap |
| `IHT_Avtale`, `IHT_Avtale_Forord` | Manuell | Dekningen følger av en trygdeavtale eller forordningen |
| `Opphor` | Manuell | Uklart hva koden betyr for perioden |
| `PENDEL`, `IKKEPENDEL` | Manuell | Uklart hvilke kapitler som omfattes |
| `FTL_2-9_2_ledd`, `FTL_2-7_bok_a`, `FTL_2-7_bok_b` | Manuell | Eldre eller uklare koder |
| `IT_DUMMY`, `IT_DUMMY_EOS` | Manuell | Tekniske koder fra Infotrygd |
| Alle andre koder | Manuell | Ukjent kode |

### 2. Bosted og statsborgerskap fra PDL

Gjelder perioder der MEDL ikke avgjør.

| Situasjon i perioden | Forslag | Steg |
|---|---|---|
| Ingen personstatus | Manuell vurdering: mangler opplysninger | 2 |
| Personstatus er ikke bosatt (for eksempel utflyttet eller D-nummer), eller bostedsadressen er utenlandsk eller ukjent | Se «Arbeid i Norge etter § 2-2». Ellers manuell vurdering av om søker er bosatt. | 2 |
| Bosatt, nordisk statsborger | JA | 2 |
| Bosatt, EØS-borger | Manuell vurdering: EØS er ikke dekket ennå | 2 |
| Bosatt, tredjelandsborger | Manuell vurdering av lovlig opphold (§ 2-1 tredje ledd). Fra steg 5 kan opphold i PDL gi forslag om JA. | 2 og 5 |
| Bosatt, statsløs eller ukjent statsborgerskap | Manuell vurdering | 2 |

Har søker flere statsborgerskap samtidig, gjelder det med høyest rang:
nordisk over EØS, og EØS over tredjeland.

PDL gir aldri forslag om NEI. At søker ikke er registrert som bosatt, kan ha
flere forklaringer: arbeid i Norge (§ 2-2), fravær under 12 måneder (§ 2-1
fjerde ledd) eller et medlemskap som mangler i MEDL.

At søker er bosatt i Folkeregisteret, er ikke det samme som at søker er bosatt
etter folketrygdloven. Folkeregisteret kan regne opphold på seks måneder som
bosetting, mens folketrygdloven krever 12. Vi godtar den forskjellen når
systemet foreslår JA i steg 2. Saksbehandler kan endre forslaget.

### 3. Arbeid i Norge etter § 2-2

Gjelder fra steg 4, for perioder der søker ikke er bosatt og MEDL ikke
avgjør.

**JA** når alt dette er oppfylt i perioden:

- Søker har et aktivt arbeidsforhold i Aa-registeret, uten registrert
  utenlandsopphold.
- Søker har inntekt i A-ordningen fra samme arbeidsgiver.
- Søker har lovlig adgang til arbeid (§ 2-2 andre ledd). Nordiske statsborgere
  og EØS-borgere har det.

Medlemskapet varer i opptil en måned etter at arbeidsforholdet er slutt
(§ 2-14 andre ledd).

**Manuell vurdering** for:

- tredjelandsborgere, fordi arbeidsadgangen må avklares
- arbeid på skip (maritime arbeidsforhold), fordi §§ 2-5, 2-6 og 2-12 har egne
  regler
- frilansere og selvstendig næringsdrivende, fordi de ikke er arbeidstakere
  etter § 1-8

Regelen gir aldri NEI.

### Det saksbehandler alltid må vurdere selv

Disse reglene kan ikke leses ut av grunnlaget og gir alltid manuell vurdering
i MVP-en. I neste runde lager vi så enkle regler som mulig, og senere eventuelt
mer avanserte.

| Regel | Hva saksbehandler ser etter |
|---|---|
| § 2-13 første ledd | Søker bosatte seg i Norge etter 1992 og har utenlandsk pensjon på minst minste pensjonsnivå |
| § 2-16 | Søker er asylsøker |
| § 2-17 | Søker sitter i varetekt, soner straff eller er under tvungent psykisk helsevern eller tvungen omsorg |
| Unntak etter avtale med USA, Canada eller Papua Ny-Guinea | Om avtalen gjelder ytelser ved sykdom |
| Åpne saker i Gosys og Joark | Om saken kan endre medlemskapet |

### Grunner til manuell vurdering

| Grunn | Når |
|---|---|
| MEDL må avklares | Status `UAVK`, lovvalg `FORL` eller `UAVK`, Lånekassen, perioder med og uten trygdedekning samtidig, eller ukjent dekning |
| Unntatt etter trygdeavtale | Ikke medlem, og lovvalgsland er USA, Canada eller Papua Ny-Guinea |
| Mangler opplysninger | Ingen personstatus i perioden |
| Vurder om søker er bosatt | Ikke bosatt, eller utenlandsk eller ukjent bostedsadresse, og § 2-2 avgjør ikke |
| Vurder lovlig opphold | Bosatt tredjelandsborger, statsløs eller ukjent statsborgerskap |
| EØS | Bosatt EØS-borger |
| Vurder arbeid | Arbeidstaker uten avklart arbeidsadgang, arbeid på skip, frilanser eller selvstendig næringsdrivende |

## Hvor vi avviker fra K9 og LovMe

### K9

K9 (pleiepenger og omsorgspenger) har et medlemskapsvilkår som ligner vårt. Vi
avviker på disse punktene:

| K9 | Vi | Hvorfor |
|---|---|---|
| Regner `FTL_2-9_1_ledd_a` som medlem og `FTL_2-9_1_ledd_b` som ikke medlem | Omvendt | § 2-9 bokstav a dekker ikke kapittel 6, men bokstav b gjør det. K9 sin gruppering kommer trolig fra foreldrepenger. |
| Regner alle `FTL_2-7_*` som medlem | Bare bokstav a | Bokstav b dekker ikke kapittel 6 |
| Ser bort fra ukjente dekningskoder | Manuell vurdering | En ny kode skal ikke forsvinne uten at noen ser den |
| Ser på om søker er statsborger i USA eller Papua Ny-Guinea | Ser på lovvalgslandet i MEDL | Det er avtalen som avgjør, ikke statsborgerskapet. Fag må bekrefte. |
| Bruker det første statsborgerskapet i listen fra PDL | Bruker det med høyest rang | Rekkefølgen i listen er tilfeldig |
| Vurderer bestemte datoer | Vurderer perioder | Vilkårene våre er periodisert |

Kilde: `AvklaringFaktaMedlemskap` og `MedlemskapsperiodeKoder` i
[navikt/k9-sak](https://github.com/navikt/k9-sak/blob/2e6390aa03a6e7d6e18106975e9e756372c89249/domenetjenester/medlem/src/main/java/no/nav/k9/sak/domene/medlem/impl/AvklaringFaktaMedlemskap.java).

### LovMe

LovMe ([navikt/medlemskap-oppslag](https://github.com/navikt/medlemskap-oppslag))
vurderer medlemskap automatisk for sykepenger, dagpenger og enslig mor eller
far. Vi bruker samme regel for status og lovvalg i MEDL, samme signal fra
Gosys og Joark og samme forsiktighet når MEDL har perioder både med og uten
medlemskap. Vi avviker på disse punktene:

| LovMe | Vi | Hvorfor |
|---|---|---|
| Dekningslisten for enslig mor eller far tar med `FTL_2-9_2_ld_jfr_1a` | Nei for kapittel 6 | Bygger på § 2-9 bokstav a, som ikke dekker kapittel 6 |
| Dekningslisten tar ikke med `FTL_2-9_b`, `FTL_2-9_c` og `FTL_2-9_3_ld_jfr_1b/1c` | Ja for kapittel 6 | Bygger på § 2-9 bokstav b eller c |
| Krever at unntaksperioden dekker en kontrollperiode på 12 måneder | Vurderer hver periode for seg | Vi har ingen kontrollperiode |
| Krever uendret arbeidsforhold | Ingen krav om arbeid | Grunnstønad krever ikke arbeid |
| Er laget for arbeidstakere | Gjelder alle, også barn og pensjonister | Søkerne våre er ofte ikke i arbeid |

### EF

EF (familie-ef-sak) har to vilkår: forutgående medlemskap og opphold i Norge.
Ingen av dem passer for oss, fordi kapittel 6 ikke har slike krav. Vi tar med
oss prinsippet om at systemet bare foreslår det som er klart, og at resten
vurderes av saksbehandler.

## Punkter til senere

For fag:

1. Kontroller tabellen «Trygdedekning for kapittel 6». Gjelder særlig
   `IHT_Avtale`, `Opphor`, `PENDEL`, `IKKEPENDEL` og de eldre kodene.
2. Hvorfor skal unntak etter avtale med USA, Canada eller Papua Ny-Guinea
   vurderes manuelt, og gjelder det flere avtaler? K9 og LovMe forklarer det
   ikke.
3. Hvordan skal § 2-13 første ledd (utenlandsk pensjon) vurderes, og kan vi
   finne det i noen kilde?
4. Hvordan skal § 2-16 (asylsøkere) og § 2-17 (fengsel og lignende) vurderes?
5. Hvem regnes som nordiske statsborgere? Gjelder det også Færøyene, Grønland
   og Åland?
6. Hva gjelder for statsløse og personer med ukjent statsborgerskap?
7. Hvilke typer opphold i PDL kan gi forslag om lovlig opphold (steg 5)?
8. Hvordan skal en åpen unntaksperiode i MEDL (uten sluttdato) vurderes?
9. Hva gjør vi når feltet «medlem» i MEDL ikke stemmer med trygdedekningen?

For teamet:

1. Regler for åpne saker i Gosys og Joark.
2. Lånekassen som kilde.
3. EØS-reglene.
4. Hva gjør vi når PDL har overlappende bostedsadresser eller personstatuser?

## Kilder

- Folketrygdloven
  [kapittel 2 (medlemskap)](https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_1-2),
  [kapittel 6 (grunnstønad og hjelpestønad)](https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_4-2),
  [kapittel 9 (sykdom i familien)](https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_4-5) og
  [kapittel 15 (enslig mor eller far)](https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_5-2)
- [navikt/k9-sak](https://github.com/navikt/k9-sak/tree/2e6390aa03a6e7d6e18106975e9e756372c89249/domenetjenester/medlem)
- [navikt/medlemskap-oppslag (LovMe)](https://github.com/navikt/medlemskap-oppslag)
- [navikt/familie-ef-sak](https://github.com/navikt/familie-ef-sak)

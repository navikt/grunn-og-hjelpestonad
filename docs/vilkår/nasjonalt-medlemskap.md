# Medlemskap etter nasjonale regler

Hvordan systemet lager et forslag til medlemskapsperioder fra
behandlingsgrunnlaget for medlemskap, for personer som ikke vurderes etter
EØS-reglene. Forslaget bygger på hvordan K9, EF og LovMe gjør det, og de er
beskrevet lenger ned. Begrepene er definert i [CONTEXT.md](../../CONTEXT.md), og
behandlingsgrunnlaget er beskrevet i
[behandlingsgrunnlag/medlemskap](../behandlingsgrunnlag/medlemskap/medlemskap.md).

Reglene og grunnlaget er beskrevet for fag og saksbehandlere i
[confluence/medlemskapsvilkaret.md](../../confluence/medlemskapsvilkaret.md).
Dette dokumentet beskriver det samme for teamet, med mer om K9 og om hvordan
det kan bygges. Ingenting av dette er i produksjon.

Kapittel 6 krever bare medlemskap (§§ 6-3 og 6-4), uten krav om forutgående
medlemskap eller opphold slik kapittel 9 og 15 har. Spørsmålet er derfor om
søker var medlem med trygdedekning for kapittel 6 i perioden. Yrkesskade
(§ 6-9) avgjøres i et annet vilkår, og da ser vi bort fra medlemskapsvilkåret.

## Steg

Vi går fra helt manuell vurdering til gradvis mer automatisering. Reglene
under er merket med steget de hører til.

| Steg | Systemet |
|---|---|
| 1 (MVP) | Henter og viser behandlingsgrunnlaget. Saksbehandler vurderer alle periodene. |
| 2 | Foreslår JA i klare tilfeller: bosatt nordisk statsborger uten noe i MEDL, eller gyldig trygdedekning for kap. 6 i MEDL |
| 3 | Foreslår NEI ut fra gyldige unntaksperioder i MEDL |
| 4 | Foreslår JA etter § 2-2 ut fra Aa-registeret og A-ordningen |
| 5 | Foreslår JA for tredjelandsborgere med lovlig opphold ut fra opphold i PDL |
| Senere | EØS, regler for åpne saker i Gosys og Joark, Lånekassen, § 2-13 første ledd, § 2-16, § 2-17 og unntak etter avtale med USA, Canada eller Papua Ny-Guinea |

Fra steg 2 til steg 5 gjelder funksjonen under.

## Funksjonen

Funksjonen tar inn behandlingsgrunnlaget og gir et forslag til
medlemskapsperioder for hele tidslinjen. Arbeidsforhold og inntekt blir input
fra steg 4. Funksjonen er ren: den henter ikke noe, den lagrer ikke noe, og
den vet ikke hvilken behandling den gjelder. Den som kaller den, kutter
tidslinjen til perioden som er aktuell, fra tidligste mulige
virkningstidspunkt (tre måneder før kravet, § 22-13 tredje ledd).

```kotlin
fun foreslåMedlemskapsperioder(
    unntaksperioder: List<MedlUnntaksperiode>,
    personopplysninger: PdlPersonopplysninger,
    arbeidsforhold: List<AaregArbeidsforhold>, // fra steg 4
    inntekter: List<AOrdningenInntekt>, // fra steg 4
): List<ForslagTilMedlemskapsperiode>

sealed interface ForslagTilMedlemskapsperiode {
    val fraOgMedDato: LocalDate?
    val tilOgMedDato: LocalDate?

    data class Foreslått(
        override val fraOgMedDato: LocalDate?,
        override val tilOgMedDato: LocalDate?,
        val vurdering: Vurdering,
        val regelverk: Regelverk,
        val begrunnelse: String,
    ) : ForslagTilMedlemskapsperiode

    data class MåVurderesManuelt(
        override val fraOgMedDato: LocalDate?,
        override val tilOgMedDato: LocalDate?,
        val grunner: Set<GrunnTilManuellVurdering>,
    ) : ForslagTilMedlemskapsperiode
}
```

- Periodene dekker hele tidslinjen uten hull og uten overlapp. `null` betyr
  åpen ende, som i vilkårstabellene.
- `Foreslått` har alltid `Regelverk.NASJONALE_REGLER` i denne omgang.
  `begrunnelse` sier hvilke opplysninger som ga forslaget, for eksempel
  «Bosatt i Norge og nordisk statsborger».
- `MåVurderesManuelt` har ingen vurdering. «Uavklart» er ikke en vurdering i
  domenet. En periode kan ha flere grunner samtidig.
- Like perioder ved siden av hverandre slås sammen.

Forslaget er at forslag til medlemskapsperioder ikke lagres, men beregnes når
saksbehandler ser på vilkåret. Saksbehandler godtar eller endrer det, og først da lagres
`VilkårMedlemskap`. Hentes grunnlaget på nytt etter at medlemskap er vurdert,
beholdes vurderingen. Avviker et nytt forslag fra det som er lagret, viser vi
at grunnlaget er endret.

## Fra opplysninger til tidslinjer

Hver opplysning blir en tidslinje for seg, og en ny periode begynner der én av
dem endrer seg. Det er samme idé som vurderingsdatoene i K9, men vi vurderer
hver periode i stedet for hver dato.

| Tidslinje | Kilde | Periode |
|---|---|---|
| Unntaksperioder | MEDL | `fraOgMed`–`tilOgMed` |
| Personstatus | PDL | Fra `gyldighetstidspunkt` til neste status begynner |
| Bostedsadresse | PDL | `gyldigFraOgMed`–`gyldigTilOgMed` |
| Statsborgerskap | PDL | `gyldigFraOgMed`–`gyldigTilOgMed`. Flere kan gjelde samtidig. |
| Opphold | PDL | `oppholdFra`–`oppholdTil`. Brukes fra steg 5. |
| Arbeidsforhold | Aa-registeret | Ansettelsesperioden, pluss en måned (§ 2-14 andre ledd). Brukes fra steg 4. |
| Inntekt | A-ordningen | Måneden inntekten gjelder. Brukes fra steg 4. |

Dødsfall i PDL avslutter tidslinjen. Perioder før den første
personstatusen får grunnen «mangler opplysninger».

## Rekkefølge

For hver periode går funksjonen gjennom reglene i denne rekkefølgen og stopper
ved første regel som avgjør:

1. [MEDL](#tolkning-av-medl)
2. [Bosted og statsborgerskap fra PDL](#tolkning-av-pdl)
3. [Arbeid i Norge etter § 2-2](#arbeid-i-norge-etter--2-2)

En gyldig unntaksperiode i MEDL går foran PDL, både for JA og for NEI. MEDL
registrerer nettopp unntakene fra medlemskap etter bosted, så uenighet mellom
MEDL og PDL er forventet. Det følger både K9 og LovMe.

LovMe er utgangspunktet, men LovMe er laget for sykepenger og andre ytelser
til arbeidstakere, og har andre unntak enn oss. LovMe krever at
unntaksperioden dekker en kontrollperiode på 12 måneder, og at
arbeidsforholdet er uendret. Det trenger ikke vi, fordi vi vurderer hver
periode for seg og ikke har krav om arbeid. Vi tar med at perioder med både
medlemskap og ikke medlemskap i MEDL samtidig vurderes manuelt.

## Tolkning av MEDL

| Unntaksperiode | Resultat |
|---|---|
| Status `GYLD` og lovvalg `ENDL` | Avgjør perioden, se tabellen under |
| Status `UAVK`, eller lovvalg `FORL` eller `UAVK` | Må vurderes manuelt |
| Status `AVST` | Tas ikke med, men vises for saksbehandler |
| Ingen unntaksperioder | PDL avgjør |

Regelen for status og lovvalg er den samme som i LovMe
(`brukerensMedlemskapsperioderIMedlForPeriode` i `Medlemskap.kt` i
`navikt/medlemskap-oppslag`).

| Gyldige unntaksperioder i perioden | Forslag | Steg |
|---|---|---|
| Trygdedekning for kap. 6 | JA | 2 |
| Trygdedekning som ikke omfatter kap. 6, også `Unntatt` | NEI | 3 |
| Både med og uten trygdedekning for kap. 6 samtidig | Må vurderes manuelt | 2 |
| Kilde Lånekassen | Må vurderes manuelt | 2 |
| Ingen dekningskode, ukjent kode eller kode som må vurderes manuelt | Må vurderes manuelt | 2 |
| Ikke medlem og lovvalgsland USA, Canada eller Papua Ny-Guinea | Må vurderes manuelt | 3 |

Vi ser på lovvalgslandet, ikke på statsborgerskapet som K9 gjør. Det er
avtalen som avgjør. LovMe gjør det samme for USA og Canada
(`harUSAellerCANunntakiKontrollperiode`). Hvorfor avtalene behandles særskilt,
må fag svare på.

### Trygdedekning for kap. 6

Tabellen er laget ut fra ordlyden i §§ 2-6 til 2-9 og skal kontrolleres av
fag. Den erstatter K9 sin gruppering, som vi ikke kan bruke: K9 regner
§ 2-9 bokstav a som medlem og bokstav b som ikke medlem, men for kap. 6 er
det omvendt.

| Dekningskode | Kap. 6 | Hvorfor |
|---|---|---|
| `Full` | Ja | Full trygdedekning |
| `FTL_2-7_3_ledd_a`, `FTL_2-7a_2_ledd_a` | Ja | Full trygdedekning ved frivillig medlemskap i Norge |
| `FTL_2-9_1_ledd_b`, `FTL_2-9_b` | Ja | § 2-9 bokstav b omfatter kap. 6 |
| `FTL_2-9_1_ledd_c`, `FTL_2-9_c`, `FTL_2-9_2_ld_jfr_1c`, `FTL_2-9_2_ld_3_ld_jfr_1c`, `FTL_2-9_3_ld_jfr_1c` | Ja | Bokstav c omfatter bokstav b |
| `FTL_2-9_3_ld_jfr_1b` | Ja | Bokstav b med særfordeler ved yrkesskade |
| `FTL_2-6` | Nei | Bare yrkesskade og dødsfall |
| `FTL_2-7_3_ledd_b`, `FTL_2-7a_2_ledd_b` | Nei | Bare kap. 5, 8, 9 og 14 |
| `Helsetjenester_sykepenger_sykdom_i_familie_svangerskap_fødsel_adopsjon` | Nei | Bare kap. 5, 8, 9 og 14 |
| `FTL_2-9_1_ledd_a`, `FTL_2-9_a`, `FTL_2-9_2_ld_jfr_1a` | Nei | Bokstav a omfatter ikke kap. 6 |
| `Unntatt` | Nei | Unntatt fra medlemskap |
| `IHT_Avtale`, `IHT_Avtale_Forord`, `Opphor`, `PENDEL`, `IKKEPENDEL`, `FTL_2-9_2_ledd`, `FTL_2-7_bok_a`, `FTL_2-7_bok_b`, `IT_DUMMY`, `IT_DUMMY_EOS` | Manuell | Avtale, forordning eller uklar kode |
| Alle andre koder | Manuell | Ukjent kode |

LovMe har en egen liste for enslig mor eller far (`dekningForEnsligForsorger`
i `Medlemskap.kt`). Den tar med `FTL_2-9_2_ld_jfr_1a`, som bygger på
bokstav a, og mangler `FTL_2-9_b`, `FTL_2-9_c` og `FTL_2-9_3_ld_jfr_1b/1c`.

## Tolkning av PDL

Gjelder perioder der MEDL ikke avgjør.

| Situasjon i perioden | Forslag | Steg |
|---|---|---|
| Ingen personstatus | Må vurderes manuelt | 2 |
| Personstatus er ikke bosatt (for eksempel utflyttet eller D-nummer), eller bostedsadressen er utenlandsk eller ukjent | [§ 2-2](#arbeid-i-norge-etter--2-2), ellers må vurderes manuelt | 2 |
| Bosatt, nordisk statsborger | JA | 2 |
| Bosatt, EØS-borger | Må vurderes manuelt (EØS) | 2 |
| Bosatt, tredjelandsborger | Må vurderes manuelt (lovlig opphold). Fra steg 5 kan `opphold` gi JA. | 2 og 5 |
| Bosatt, statsløs eller ukjent statsborgerskap | Må vurderes manuelt | 2 |

Har søker flere statsborgerskap samtidig, gjelder regionen med høyest rang:
Norden over EØS, EØS over tredjeland.

PDL gir aldri NEI. Søker kan være medlem selv om hen ikke er bosatt, for
eksempel etter § 2-2, ved fravær under 12 måneder (§ 2-1 fjerde ledd) eller
med et medlemskap som mangler i MEDL. Bosatt i Folkeregisteret er ikke det
samme som bosatt etter § 2-1 (seks mot 12 måneder). Det godtar vi i steg 2.

K9 er ikke konsekvent her. Vurderingsdatoene bruker regionen med høyest rang,
men regeltreet (`VurderLøpendeMedlemskap`) og aksjonspunktene
(`AvklaringFaktaMedlemskap`) bruker bare det første statsborgerskapet i
listen fra PDL. Listen er ikke sortert på region. En søker med statsborgerskap
i både USA og Sverige kan derfor få aksjonspunkt for lovlig opphold hvis USA
står først, selv om det svenske statsborgerskapet skulle gitt oppfylt vilkår.
Vi bruker alltid regionen med høyest rang.

## Arbeid i Norge etter § 2-2

Fra steg 4, for perioder der søker ikke er bosatt og MEDL ikke avgjør.

JA når alt dette gjelder i perioden:

- Et aktivt arbeidsforhold i Aa-registeret uten registrert utenlandsopphold.
- Inntekt i A-ordningen fra samme arbeidsgiver.
- Lovlig adgang til arbeid (§ 2-2 andre ledd): nordisk statsborger eller
  EØS-borger.

Medlemskapet varer en måned etter at arbeidsforholdet er slutt (§ 2-14 andre
ledd). Tredjelandsborgere, maritime arbeidsforhold, frilansere og selvstendig
næringsdrivende må vurderes manuelt. Regelen gir aldri NEI, fordi kildene ikke
kan vise at søker ikke er arbeidstaker.

## Grunner til manuell vurdering

| Grunn | Når | Tilsvarer i K9 |
|---|---|---|
| `MANGLER_OPPLYSNINGER` | Ingen personstatus i perioden | – |
| `MEDL_MÅ_AVKLARES` | Status `UAVK`, lovvalg `FORL` eller `UAVK`, Lånekassen, både med og uten trygdedekning samtidig, eller ukjent dekning | Avklar gyldig medlemskapsperiode |
| `UNNTATT_ETTER_TRYGDEAVTALE` | Ikke medlem i MEDL og lovvalgsland USA, Canada eller Papua Ny-Guinea | Avklar lovlig opphold |
| `VURDER_BOSATT` | Ikke bosatt, eller utenlandsk eller ukjent bostedsadresse, og § 2-2 avgjør ikke | Avklar om søker er bosatt |
| `VURDER_LOVLIG_OPPHOLD` | Bosatt tredjelandsborger, statsløs eller ukjent statsborgerskap | Avklar lovlig opphold |
| `EØS` | Bosatt, og høyeste region er EØS | EØS-grenen i regeltreet |
| `VURDER_ARBEID` | Arbeidstaker som er tredjelandsborger, har maritimt arbeidsforhold, er frilanser eller selvstendig næringsdrivende | – |

## Regler og kilder som ikke er med

Disse gir alltid manuell vurdering i MVP-en. I neste runde lager vi så enkle
regler som mulig, og senere eventuelt mer avanserte.

- **§ 2-13 første ledd.** Bosatt etter 1992 med utenlandsk pensjon: kap. 6
  dekkes bare i perioder med pensjonsgivende inntekt eller pensjon fra
  folketrygden.
- **§ 2-16 og § 2-17.** Asylsøkere, og varetekt, soning og lignende.
- **Åpne saker i Gosys og Joark** med tema `MED`, `UFM` eller `TRY`. LovMe
  gir «uavklart» når de finnes. Grunnlaget hentes, men regelen kommer senere.
- **EØS.** Eget dokument senere. Til da får EØS-borgere `EØS`.
- **Lånekassen** som egen kilde for studenter i utlandet (§ 2-5 bokstav h).
- **Opplysninger fra søker.** Saksbehandler leser dem. Reglene bruker dem
  ikke før søknaden er strukturert.

## Slik gjør EF

familie-ef-sak har to vilkår: forutgående medlemskap og opphold i Norge.
Opphold i Norge ligner mest på vårt vilkår. Behandlingsgrunnlaget er beskrevet
i [behandlingsgrunnlag/medlemskap](../behandlingsgrunnlag/medlemskap/medlemskap.md#slik-gjør-ef).

- Vilkåret vurderes for hele behandlingen, ikke per periode.
- Systemet vurderer bare de klare tilfellene, og bare som «ja»: norsk
  statsborger, bosatt, og søknaden sier at søker bor og oppholder seg i Norge.
- MEDL brukes ikke automatisk. Gyldige unntaksperioder vises for
  saksbehandler.
- Alt annet vurderer saksbehandler med faste svaralternativer.

Forslaget tar med seg at systemet bare foreslår det som er klart, og at resten
vurderes av saksbehandler. Den tar ikke med at vilkåret vurderes for hele
behandlingen. Kap. 6 har heller ikke krav om forutgående medlemskap eller
opphold, så EF sine regler kan ikke brukes direkte.

## Slik gjør LovMe

LovMe (`navikt/medlemskap-oppslag`) vurderer medlemskap automatisk for
sykepenger, dagpenger og enslig mor eller far, og svarer JA, NEI eller
uavklart for en kontrollperiode på 12 måneder.

- **MEDL** (`ReglerForMedl`): Bare perioder med status `GYLD` og lovvalg
  `ENDL` teller. Perioder både med og uten medlemskap gir uavklart. En periode
  med medlemskap må dekke hele kontrollperioden, arbeidsforholdet må være
  uendret, og dekningen må stå på listen for ytelsen. Unntak etter avtale med
  USA eller Canada gir uavklart.
- **Gosys og Joark**: Åpne oppgaver og journalposter med tema `MED`, `UFM`
  eller `TRY` gir uavklart.
- **Resultatet** (`Hovedregler.utledResultat`): NEI fra en regel vinner. Ellers
  vinner JA fra MEDL over uavklart fra de andre reglene.
- **Statsborgerskap**: Egne regelsett for norske statsborgere, EØS-borgere og
  andre, med UDI for lovlig opphold og arbeidsadgang.

Vi bruker samme regel for status og lovvalg, samme signal fra Gosys og Joark,
og samme forsiktighet når MEDL har perioder både med og uten medlemskap. Vi
har ikke kontrollperiode eller krav om arbeid, og vi har en egen liste over
trygdedekning for kap. 6.

## Slik gjør K9

K9 kaller unntaksperiodene for medlemskap fra MEDL for «medlemskapsperioder». Her bruker vi
våre egne begreper.

Kildene er k9-sak på commit
[`2e6390a`](https://github.com/navikt/k9-sak/tree/2e6390aa03a6e7d6e18106975e9e756372c89249):

- `domenetjenester/medlem`: henter og tolker unntaksperioder for medlemskap, og finner ut
  hva saksbehandler må avklare (`VurderMedlemskapTjeneste` og `Avklar*`).
- `domenetjenester/inngangsvilkar/.../medlemskap`: regeltreet
  (`Medlemskapsvilkår`, `FP_VK_2`) og grunnlaget for det
  (`VurderLøpendeMedlemskap`).

### Behandlingsgrunnlaget K9 bruker

| Opplysning | Kilde | Hvordan K9 bruker den |
|---|---|---|
| Unntaksperioder for medlemskap med status gyldig eller uavklart | MEDL | Dekningskoden oversettes til K9 sin egen type |
| Personstatus | PDL | Bosatt, utvandret, død eller annet |
| Statsborgerskap | PDL | Oversettes til region: Norden, EØS eller tredjeland |
| Adresser | PDL | Utenlandsk adresse gir behov for avklaring |
| Utenlandsopphold | Søknaden | Oppgitt opphold i utlandet gir behov for avklaring |
| Arbeidsforhold og pensjonsgivende inntekt | Aa-registeret og A-ordningen | Kan gi oppfylt vilkår for den som ikke er bosatt |

K9 ber bare MEDL om unntaksperioder for medlemskap med status `GYLD` og `UAVK` (`MedlemsunntakRestKlient` i k9-felles).

### Vurderingsdatoer

K9 vurderer ikke en sammenhengende tidslinje. Vilkåret vurderes på bestemte
datoer, og hver vurdering gjelder fram til neste dato.
`UtledVurderingsdatoerForMedlemskapTjeneste` finner datoene:

- Første dag i hver periode som vurderes.
- Datoer der personstatusen endres. Endringer til og fra død teller ikke.
- Datoer der adressetypen endres, for eksempel fra norsk til utenlandsk
  adresse.
- Datoer der regionen for statsborgerskapet endres. Har søker flere
  statsborgerskap samtidig, gjelder regionen med høyest rang, der Norden
  rangeres over EØS og EØS over tredjeland.
- Datoer der unntaksperiodene for medlemskap i MEDL er endret siden første henting i
  behandlingen.

### Hvordan K9 tolker dekningskodene

K9 oversetter dekningskoden fra MEDL til `MedlemskapDekningType` og deler
typene i grupper (`MedlemskapsperiodeKoder` og `MedlemskapDekningType`):

| Gruppe | Dekningskoder fra MEDL | Betyr |
|---|---|---|
| Pliktig eller frivillig medlem | `FTL_2-7_*`, `FTL_2-9_1_ledd_a`, `FTL_2-9_a`, `FTL_2-9_1_ledd_c`, `FTL_2-9_c`, `FTL_2-9_2_ld_jfr_1a`, `FTL_2-9_2_ld_jfr_1c`, `Full` | Oppfylt |
| Ikke medlem | `FTL_2-6`, `FTL_2-9_1_ledd_b`, `FTL_2-9_b` | Ikke oppfylt |
| Unntatt | `Unntatt` | Ikke oppfylt, med mindre søker er statsborger i USA eller Papua Ny-Guinea |
| Uavklart | `IHT_Avtale`, `IHT_Avtale_Forord`, `Opphor` | Saksbehandler må avklare |
| Ukjent | `FTL_2-9_2_ledd`, `IKKEPENDEL`, `PENDEL`, `IT_DUMMY`, `IT_DUMMY_EOS` | Blir ikke tatt med |

Vi bruker ikke denne grupperingen. Se [Trygdedekning for kap.
6](#trygdedekning-for-kap-6).

En unntaksperiode for medlemskap tas bare med når den har både fra- og til-dato og dekker
vurderingsdatoen. Unntaksperioder for medlemskap fra Lånekassen og med lovvalg «under
avklaring» tas heller ikke med. Unntaksperioder for medlemskap som er åpne i én ende,
kommer fra Lånekassen eller har lovvalg «under avklaring», må saksbehandler
avklare.

K9 bruker ikke feltet `medlem` fra MEDL i vurderingen. Det er dekningskoden
som avgjør.

### Regeltreet

Regeltreet `FP_VK_2` kjøres for hver vurderingsdato. Når saksbehandler ikke
har avklart noe, er standardsvaret at søker er bosatt og har lovlig opphold.
Hver node viser hvor K9 henter svaret fra.

```mermaid
flowchart TD
    ikkeMedlem{"Registrert som ikke medlem?<br/>MEDL, PDL, saksbehandler"}
    pliktig{"Pliktig eller frivillig medlem?<br/>MEDL, saksbehandler"}
    utvandret{"Personstatus utvandret?<br/>PDL"}
    ikkeBosatt{"Avklart som ikke bosatt?<br/>Saksbehandler"}
    arbeid1{"Arbeidsforhold med<br/>pensjonsgivende inntekt?<br/>Aa-registeret, A-ordningen"}
    arbeid2{"Arbeidsforhold med<br/>pensjonsgivende inntekt?<br/>Aa-registeret, A-ordningen"}
    nordisk{"Nordisk statsborger?<br/>PDL"}
    eøs{"EØS-borger?<br/>PDL, saksbehandler"}
    lovlig{"Avklart med lovlig opphold?<br/>Saksbehandler"}

    oppfylt(["Oppfylt"])
    ikkeOppfylt1(["Ikke oppfylt:<br/>registrert som ikke medlem"])
    ikkeOppfylt2(["Ikke oppfylt:<br/>utvandret"])
    ikkeOppfylt3(["Ikke oppfylt:<br/>ikke bosatt"])
    ikkeOppfylt4(["Ikke oppfylt:<br/>ikke lovlig opphold"])
    eøsDok(["Oppholdsrett:<br/>eget dokument"])

    ikkeMedlem -- Ja --> ikkeOppfylt1
    ikkeMedlem -- Nei --> pliktig
    pliktig -- Ja --> oppfylt
    pliktig -- Nei --> utvandret
    utvandret -- Ja --> arbeid1
    arbeid1 -- Ja --> oppfylt
    arbeid1 -- Nei --> ikkeOppfylt2
    utvandret -- Nei --> ikkeBosatt
    ikkeBosatt -- Ja --> arbeid2
    arbeid2 -- Ja --> oppfylt
    arbeid2 -- Nei --> ikkeOppfylt3
    ikkeBosatt -- Nei --> nordisk
    nordisk -- Ja --> oppfylt
    nordisk -- Nei --> eøs
    eøs -- Ja --> eøsDok
    eøs -- Nei --> lovlig
    lovlig -- Ja --> oppfylt
    lovlig -- Nei --> ikkeOppfylt4
```

- **Registrert som ikke medlem:** søker har en unntaksperiode for medlemskap i gruppen «ikke
  medlem», eller i gruppen «unntatt» uten å være statsborger i USA eller Papua
  Ny-Guinea. Saksbehandler kan også ha avklart at søker ikke er medlem.
- **Pliktig eller frivillig medlem:** søker har en unntaksperiode for medlemskap i gruppen
  «pliktig eller frivillig medlem», eller saksbehandler har avklart at søker er
  medlem.
- **Arbeidsforhold med pensjonsgivende inntekt:** søker har et arbeidsforhold
  som har startet og ikke er avsluttet på vurderingsdatoen, og har
  pensjonsgivende inntekt fra samme arbeidsgiver.
- **Personstatus:** personer med D-nummer regnes som bosatt i regeltreet.

### Når saksbehandler må avklare

K9 lager aksjonspunkter når opplysningene ikke er nok
(`VurderMedlemskapTjeneste`):

| Aksjonspunkt | Når |
|---|---|
| Avklar om søker er bosatt | Personstatusen er noe annet enn bosatt eller død, for eksempel D-nummer. Søker har oppgitt utenlandsopphold. Eller søker har utenlandsk adresse uten å være registrert som pliktig, frivillig eller ikke medlem i MEDL. |
| Avklar gyldig unntaksperiode for medlemskap | Søker har unntaksperioder for medlemskap, men ingen gyldig periode som pliktig eller frivillig medlem, og minst én periode er under avklaring, kommer fra Lånekassen, er åpen eller har en uavklart dekningskode. |
| Avklar lovlig opphold | Søker er tredjelandsborger, er ikke utvandret og har ingen gyldig unntaksperiode for medlemskap med kjent dekning. Eller søker er unntatt i MEDL, er statsborger i USA eller Papua Ny-Guinea og er ikke utvandret. |

Saksbehandler svarer per vurderingsdato. Svaret erstatter standardsvaret i
regeltreet.

K9 lager også en vurderingsdato der MEDL er endret siden første henting i
behandlingen. Det trenger vi trolig ikke hvis en ny henting erstatter
grunnlaget og forslaget beregnes på nytt.

## Åpne spørsmål

For fag:

- Stemmer [tabellen over trygdedekning for kap. 6](#trygdedekning-for-kap-6)?
  Gjelder særlig `IHT_Avtale`, `Opphor`, `PENDEL`, `IKKEPENDEL` og de eldre
  kodene.
- Hvorfor skal unntak etter avtale med USA, Canada eller Papua Ny-Guinea
  vurderes manuelt, og gjelder det flere avtaler? K9 og LovMe forklarer det
  ikke («Sært, men USA og Papua Ny-Guinea særbehandles» i K9).
- Hvordan skal § 2-13 første ledd, § 2-16 og § 2-17 vurderes?
- Hvem regnes som nordiske statsborgere? Gjelder det Færøyene, Grønland og
  Åland også?
- Hva gjelder for statsløse og personer med ukjent statsborgerskap?
- Hvilke typer `opphold` i PDL kan gi forslag om lovlig opphold (steg 5)?
- Skal en åpen unntaksperiode i MEDL gi manuell vurdering, som i K9? Med
  tidslinjer kan en åpen periode tolkes som løpende.
- Hva gjør vi når feltet `medlem` i MEDL ikke stemmer med trygdedekningen?

For teamet:

- Regler for åpne saker i Gosys og Joark.
- Lånekassen som kilde.
- EØS-reglene.
- Hva gjør vi når PDL har overlappende bostedsadresser eller personstatuser?

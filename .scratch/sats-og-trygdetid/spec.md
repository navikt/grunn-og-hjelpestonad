# Sats og trygdetid

Status: ready-for-agent

Se [ADR-0014](../../docs/adr/ADR-0014-tilkjent-ytelse-fra-ekstrautgifter-og-trygdetid.md),
[ADR-0006](../../docs/adr/ADR-0006-fem-vilkaar-med-inngangsvilkaar-som-foerste-steg.md),
[utledning av tilkjent ytelse](../../docs/tilkjent-ytelse/utledning-av-tilkjent-ytelse.md)
og begrepene i [CONTEXT.md](../../CONTEXT.md).

Bygger på PR 116 (ticket 13 i
[migrering](../migrering/issues/13-startmaaned-tilbake-paa-tilkjent-ytelse.md)),
som må være merget først.

## Problem

Saksbehandler kan vurdere medlemskap og diagnose, men kan ikke vurdere
nødvendige ekstrautgifter eller legge inn trygdetid. Løsningen kan derfor ikke
regne ut hva søker har rett til.

Tilkjent ytelse regnes i dag ut i vedtakssteget med logikk arvet fra
barnetilsyn. Beløpet er et heltall, og ingen regel for grunnstønad er med.
Vilkårssteget vurderer også institusjon, som ikke er ferdig modellert, og
lagrer perioder med rett som kan bli utdaterte.

## Løsning

Behandlingen får to steg før simulering:

1. **Inngangsvilkår.** Saksbehandler vurderer medlemskap og diagnose. Når
   saksbehandler trykker Neste, regner systemet ut periodene med oppfylte
   inngangsvilkår og avgjør om saken går videre, blir avslått eller opphører.
2. **Sats og trygdetid.** I periodene med oppfylte inngangsvilkår vurderer
   saksbehandler nødvendige ekstrautgifter, med sats 1–6 eller «nei», og legger
   inn trygdetid. Når saksbehandler trykker Neste, regner systemet ut og lagrer
   tilkjent ytelse per måned etter § 22-12.

Tilkjent ytelse er satsbeløpet × trygdetid / 40, uten avrunding. Saksbehandler
ser den øverst i steget for simulering. Institusjon tas ut av løsningen til det
er modellert sammen med ekstrautgiftene.

## Brukerhistorier

### Inngangsvilkår

1. Som saksbehandler vil jeg bare vurdere medlemskap og diagnose i første steg,
   slik at jeg ikke vurderer skjønnsvilkår før jeg vet at søker oppfyller
   inngangsvilkårene.
2. Som saksbehandler vil jeg ikke se institusjon i vilkårsvurderingen, slik at
   jeg ikke vurderer et vilkår løsningen ikke bruker.
3. Som saksbehandler vil jeg få en feilmelding per inngangsvilkår som ikke har
   noen vurdert periode når jeg trykker Neste, slik at jeg vet hva som mangler.
4. Som saksbehandler vil jeg se periodene med oppfylte inngangsvilkår etter at
   jeg har trykket Neste, slik at jeg vet hvilke tidsrom jeg skal vurdere i neste
   steg.
5. Som saksbehandler vil jeg gå rett til brev når en førstegangsbehandling ikke
   har noen perioder med oppfylte inngangsvilkår, slik at jeg kan sende avslag
   uten å fylle ut flere steg.
6. Som saksbehandler vil jeg at en revurdering uten perioder med oppfylte
   inngangsvilkår går til steg 2 med beskjed om at saken opphører, slik at
   utbetalingen blir stoppet mot oppdrag.
7. Som saksbehandler vil jeg at endringshistorikken viser «Vurdering av
   inngangsvilkår fullført», slik at jeg ser når steget ble fullført.
8. Som saksbehandler vil jeg at periodene med oppfylte inngangsvilkår alltid
   regnes ut fra vilkårene slik de er nå, slik at jeg aldri ser utdaterte
   perioder.

### Nødvendige ekstrautgifter

9. Som saksbehandler vil jeg vurdere nødvendige ekstrautgifter per periode med
   ja eller nei, slik at jeg kan gi rett i noen perioder og avslag i andre.
10. Som saksbehandler vil jeg velge sats 1–6 når jeg vurderer ekstrautgiftene
    til ja, slik at satsen følger vurderingen.
11. Som saksbehandler vil jeg ikke kunne velge sats når jeg vurderer
    ekstrautgiftene til nei, slik at et avslag etter § 6-3 tredje ledd ikke får
    en sats.
12. Som saksbehandler vil jeg skrive begrunnelse for vurderingen, slik at
    utgiftstype og eventuell fast sats for diagnosen er dokumentert.
13. Som saksbehandler vil jeg at et tidsrom uten vurdering av ekstrautgifter
    regnes som «nei», på samme måte som inngangsvilkårene.
14. Som saksbehandler vil jeg få en feilmelding hvis det finnes perioder med
    oppfylte inngangsvilkår, men ingen vurdert ekstrautgiftsperiode, slik at jeg
    ikke gir avslag ved en feil.
15. Som saksbehandler vil jeg kunne endre, splitte og slette perioder på samme
    måte som for inngangsvilkårene, slik at alle vilkår oppfører seg likt.

### Trygdetid

16. Som saksbehandler vil jeg legge inn trygdetid per periode som en teller fra
    0 til 40, slik at tilkjent ytelse blir redusert etter § 6-6.
17. Som saksbehandler vil jeg oppgi om trygdetiden kommer fra alderspensjon,
    uføretrygd eller ingen av dem, slik at kilden kan stå i brevet.
18. Som saksbehandler vil jeg at kilden «ingen» alltid gir 40/40, slik at jeg
    ikke kan redusere beløpet uten grunnlag.
19. Som saksbehandler vil jeg ikke svare ja eller nei på trygdetid, fordi søker
    kan ha rett uansett trygdetid.
20. Som saksbehandler vil jeg få en feilmelding hvis et tidsrom med ekstrautgifter
    vurdert til ja mangler trygdetid, slik at systemet aldri gjetter brøken.
21. Som saksbehandler vil jeg slippe å legge inn trygdetid der ekstrautgiftene er
    vurdert til nei.

### Periodene i steg 2

22. Som saksbehandler vil jeg se periodene med oppfylte inngangsvilkår som en
    tidslinje øverst i steg 2.
23. Som saksbehandler vil jeg se hvilke perioder for ekstrautgifter og trygdetid
    som ligger utenfor periodene med oppfylte inngangsvilkår, slik at jeg kan
    rydde etter at jeg har endret et inngangsvilkår.
24. Som saksbehandler vil jeg at perioder utenfor ikke gir feil og ikke påvirker
    beregningen, slik at jeg ikke blir stoppet av perioder som ikke betyr noe.

### Tilkjent ytelse

25. Som saksbehandler vil jeg at systemet regner ut tilkjent ytelse når jeg
    trykker Neste i steg 2, slik at jeg ser beløpet før simulering.
26. Som saksbehandler vil jeg at tilkjent ytelse følger satsbeløpet som gjaldt i
    hver måned, slik at satsendringer fra Stortinget blir med.
27. Som stønadsmottaker vil jeg få utbetalt fra måneden etter at jeg fyller
    vilkårene, etter § 22-12 første ledd.
28. Som stønadsmottaker vil jeg at utbetalingen stopper ved utgangen av måneden
    retten faller bort, etter § 22-12 sjette ledd.
29. Som stønadsmottaker vil jeg få høyere sats eller høyere trygdetidsbrøk fra
    samme måned som endringen, etter § 22-12 fjerde ledd.
30. Som Nav vil jeg at lavere sats eller lavere trygdetidsbrøk gjelder fra
    måneden etter endringen, etter § 22-12 femte ledd.
31. Som Nav vil jeg at hver endring av sats og trygdetid vurderes for seg etter
    § 22-12, slik at en økning og en reduksjon i samme måned får hver sin
    virkningsmåned.
32. Som Nav vil jeg at tilkjent ytelse lagres eksakt med desimaler, slik at
    oppdrag eier avrundingen etter § 22-11 og ADR-0011.
33. Som saksbehandler vil jeg at en måned med trygdetid 0/40 regnes som en måned
    med rett, selv om den gir 0 kr og ingen andel.
34. Som saksbehandler vil jeg se tilkjent ytelse per periode øverst i steget for
    simulering, med satsbeløp, brøk og beløp, merket som tilkjent ytelse og ikke
    utbetalingsbeløp.
35. Som saksbehandler vil jeg at gjentatte trykk på Neste gir samme tilkjent
    ytelse, slik at det er trygt å gå fram og tilbake.

### Utdatert beregning

36. Som Nav vil jeg at tilkjent ytelse slettes når et vilkår eller trygdetiden
    lagres eller slettes, slik at en utdatert beregning aldri går videre.
37. Som saksbehandler vil jeg få beskjed om å fullføre steg 2 hvis jeg prøver å
    simulere eller innvilge uten tilkjent ytelse.

### Satsbeløp

38. Som Nav vil jeg at satsbeløpene fra rundskrivet til § 6-3 ligger i kode
    fra 1.1.1997, slik at revurderinger og migreringer tilbake i tid virker.
39. Som Nav vil jeg at systemet kaster en feil hvis det ber om et satsbeløp for
    en dato utenfor listen, i stedet for å bruke nærmeste beløp.
40. Som utvikler vil jeg at en test sjekker at periodene for hver sats ikke
    overlapper og ikke har hull, slik at en feil i en ny sats blir oppdaget før
    deploy.
41. Som utvikler vil jeg legge inn nye satser i en PR med review, slik at
    endringen er sporbar.

### Vedtak

42. Som saksbehandler vil jeg fortsatt velge resultatet i vedtaket.
43. Som saksbehandler i en førstegangsbehandling vil jeg få feil hvis jeg velger
    innvilget uten noen måned med rett, eller avslått med minst én måned med
    rett.
44. Som saksbehandler vil jeg slippe felter fra barnetilsyn i vedtaket.

### Ny behandling

45. Som saksbehandler vil jeg at en ny behandling kopierer perioder vurdert til
    ja for medlemskap, diagnose og ekstrautgifter fra forrige iverksatte
    behandling, som i barnetrygd.
46. Som saksbehandler vil jeg at alle trygdetidsperiodene blir kopiert, fordi de
    ikke har noen vurdering.
47. Som saksbehandler vil jeg kunne starte en revurdering etter et avslag uten
    kopierte vilkår, uten at systemet stopper meg.

### Personvern

48. Som Nav vil jeg at diagnose ikke havner i endringshistorikk, logger eller
    responsene fra de nye endepunktene, etter GDPR artikkel 9.

## Beslutninger om implementasjon

### Institusjon fjernes

- Vilkåret fjernes helt: tabellen droppes i en ny migrering, og verdien i
  vilkårstypen, servicen, kontrolleren, valideringen, kopieringen og
  skjermbildet fjernes.
- ADR-0006 har aksjonspunktet om at institusjon må tilbake før prod.

### Steg 1: inngangsvilkår

- Tabellen for perioder med rett og alle typene rundt den fjernes.
- Periodene med oppfylte inngangsvilkår regnes ut av en ren funksjon fra
  vilkårsradene for medlemskap og diagnose, med dagens regler for § 6-9 og
  flere samtidige diagnoser.
- `POST /api/behandling/{behandlingId}/inngangsvilkar/fullfor` erstatter
  `/vilkar/fullfor`. Endepunktet sjekker at behandlingen kan redigeres og at
  kallet kommer fra ansvarlig saksbehandler, validerer R1 for medlemskap og
  diagnose, og lagrer ingenting.
- Endepunktet skriver «Vurdering av inngangsvilkår fullført» i
  endringshistorikken med en ny endringstype. Detaljene inneholder bare antall
  perioder og datoer.
- Responsen inneholder periodene og utfallet. Utfallet er én av tre verdier:
  videre til sats og trygdetid, avslag eller opphør.
- En ren funksjon avgjør utfallet. Er det perioder, går saken videre. Er det
  ingen perioder og ingen tidligere iverksatt behandling, blir det avslag. Er
  det ingen perioder i en revurdering, blir det opphør.
- Valideringsfeil har samme format som i dag, med én feil per vilkårstype og
  egen exception handler på kontrolleren.

### Nytt vilkår: nødvendige ekstrautgifter

- Egen tabell med fellesfeltene til vilkårene (id, behandling, vurdering,
  begrunnelse, fra og med, til og med, sporbarhet) og `sats` (heltall 1–6, kan
  være tom).
- En CHECK-constraint håndhever at `sats` er satt ved `JA` og tom ved `NEI`.
  En CHECK-constraint håndhever også datorekkefølgen.
- Vilkåret bruker `VilkårPeriodeService`: redigerbarhet, ansvarlig
  saksbehandler, forkorting ved overlapp og endringshistorikk.
- Endepunktene ligger på `/api/behandling/{behandlingId}/vilkar/ekstrautgifter`,
  med hent, lagre og slett som for de andre vilkårene.
- Ny verdi i vilkårstypen for nødvendige ekstrautgifter.

### Fellesdel for vilkårsperioder og trygdetid

- Prefactoring uten endret oppførsel. Fellesdelen av vilkårsperiodene skilles
  ut under et navn uten «Vilkår», for eksempel `PeriodeMedBegrunnelse`. Den har
  id, behandling, begrunnelse, datoer, sporbarhet, tidslinjenøkkel, kopiering,
  forkorting ved overlapp, redigerbarhet, ansvarlig saksbehandler og
  endringshistorikk.
- `VilkårPeriode`, `VilkårPeriodeRequest` og `VilkårPeriodeService` bygger på
  fellesdelen og legger til vurderingen og «er oppfylt».
- Servicen for fellesdelen tar inn hvilke endringstyper den skriver. Vilkårene
  bruker fortsatt `VILKÅR_VURDERING_*`.
- Fellesdelen bygger detaljene i endringshistorikken av faste felter, så
  diagnose aldri kan havne der.

### Trygdetid

- Trygdetid er ikke et vilkår. Den har ingen `JA`/`NEI`, kan ikke gi avslag og
  reduserer bare tilkjent ytelse etter § 6-6. Den bygger på fellesdelen, men
  ikke på noe som er spesielt for vilkår.
- Egen tabell, `trygdetid` og ikke `vilkar_trygdetid`, med id, behandling,
  begrunnelse, fra og med, til og med, sporbarhet, `teller` (heltall 0–40) og
  `kilde` (`ALDERSPENSJON`, `UFØRETRYGD`, `INGEN`). Raden har ingen vurdering.
- CHECK-constraints: `teller` er mellom 0 og 40, `kilde = INGEN` gir
  `teller = 40`, og datoene står i riktig rekkefølge.
- Servicen bygger på servicen for fellesdelen, ikke på `VilkårPeriodeService`.
- Egne endringstyper for opprettet, oppdatert og slettet trygdetid, ikke
  `VILKÅR_VURDERING_*`. Detaljene har type og periode, ingen vurdering.
- Endepunktene ligger på `/api/behandling/{behandlingId}/trygdetid`, med hent,
  lagre og slett.
- `VilkårType` får ingen verdi for trygdetid.

### Satsbeløp

- En Kotlin-liste i et `object` med én rad per sats og gyldighetsperiode:
  sats, månedsbeløp i hele kroner (`Int`), gyldig fra og med, gyldig til og med.
- Hele historikken fra rundskrivet til § 6-3 fra 1.1.1997, med månedsbeløpet.
  Ikke årsbeløpet delt på 12.
- Satsene fra 2024 legges inn med gyldighet fra 1.1.2024. Fag skal bekrefte
  datoen, se ADR-0011.
- Oppslag for en dato utenfor listen kaster en feil.

### Utledning av tilkjent ytelse

- Én ren funksjon uten Spring, etter ADR-0008. Den tar inn vilkårsradene for
  medlemskap, diagnose og ekstrautgifter, trygdetidsradene og listen over
  satsbeløp. Den returnerer andeler per måned, eller kaster en valideringsfeil
  med listen over feil: én per vilkårstype og én for trygdetid, med egen nøkkel.
- Den erstatter beregningen av andeler fra barnetilsyn som PR 116 la i en egen
  funksjon.
- Validering i funksjonen: er det perioder med oppfylte inngangsvilkår, må minst
  én ekstrautgiftsperiode være vurdert, og trygdetiden må dekke alle tidsrom der
  ekstrautgiftene er `JA` innenfor periodene med oppfylte inngangsvilkår.
- Rett i et tidsrom betyr inngangsvilkårene oppfylt og ekstrautgifter `JA`.
  Hull er «nei». Perioder for ekstrautgifter og trygdetid utenfor periodene med
  oppfylte inngangsvilkår tas ikke med.
- § 22-12 brukes per endring:
  - Retten gjelder fra måneden etter at den oppstår.
  - Utbetalingen stopper ved utgangen av måneden retten faller bort.
  - En høyere sats eller brøk gjelder fra samme måned.
  - En lavere sats eller brøk gjelder fra måneden etter.

  Endringer i satsbeløpet fra Stortinget gjelder fra datoen i listen. KDoc
  merker tolkningen som ubekreftet av fag, med henvisning til ledd.
- Beløpet per måned er satsbeløpet × teller / 40 som `BigDecimal`, eksakt med
  tre desimaler. Måneder med 0 kr gir ingen andel. Måneder med likt beløp slås
  sammen til én andel.

### Steg 2: sats og trygdetid

- `POST /api/behandling/{behandlingId}/sats-og-trygdetid/fullfor` sjekker
  redigerbarhet og ansvarlig saksbehandler, henter vilkårene og trygdetiden,
  kaller utledningen og lagrer tilkjent ytelse med tjenesten for beregning av
  tilkjent ytelse fra PR 116. Startmåned regnes ut som PR 116 gjør.
- Lagringen sletter og setter inn på nytt i samme transaksjon.
- Ny endringstype i endringshistorikken for fullført sats og trygdetid, med
  antall andeler og datoer, uten diagnose.
- Valideringsfeil returneres med samme format som i steg 1.
- I en revurdering med opphør og ingen perioder lagrer steget tilkjent ytelse
  uten andeler, slik at oppdrag opphører.

### Tilkjent ytelse og oppdrag

- Beløpskolonnen på andelene endres fra heltall til `NUMERIC` i en migrering som
  kommer etter `V48` fra PR 116.
- Domenemodellen for andelene bruker `BigDecimal`.
- Simulering og oppdrag avrunder med `HALF_UP` når de leser beløpet, etter
  § 22-11 og ADR-0011.
- Vedtakssteget regner ikke lenger ut tilkjent ytelse. Kallet flyttes til
  steg 2.

### Sletting når vilkår eller trygdetid endres

- Servicen for fellesdelen sletter tilkjent ytelse for behandlingen i samme
  transaksjon når en periode lagres eller slettes. Slettingen ligger i
  fellesdelen, ikke i `VilkårPeriodeService`, så den gjelder medlemskap,
  diagnose, ekstrautgifter og trygdetid.
- Simulering og vedtak om innvilgelse stopper med en funksjonell feil om å
  fullføre steg 2 hvis tilkjent ytelse mangler. Et vedtak om avslag krever ikke
  tilkjent ytelse.

### Vedtak

- Beløpsperiodene fra barnetilsyn, med utgifter og barn, fjernes fra vedtaket
  og fra API-et.
- I en førstegangsbehandling krever `INNVILGET` minst én måned med rett i
  tilkjent ytelse, og `AVSLÅTT` krever at det ikke finnes noen. Revurderinger
  sjekkes ikke ennå.

### Kopiering til ny behandling

- Medlemskap, diagnose og ekstrautgifter kopierer bare rader med `JA`.
- Trygdetid er ikke et vilkår og kopieres for seg, med alle rader.
- Feilen som kastes når ingenting blir kopiert, fjernes.

### Frontend

- Steg 1 viser medlemskap og diagnose, kaller det nye endepunktet for
  fullføring og navigerer etter utfallet: steg 2, brev ved avslag, eller steg 2
  med beskjed om opphør.
- Steg 2 viser tidslinjen med perioder med oppfylte inngangsvilkår, listen over
  ekstrautgifter og listen over trygdetid. Listene er bygget som komponentene
  for inngangsvilkår, og perioder utenfor er markert.
- Ekstrautgifter: vurdering ja/nei, sats 1–6 bare ved ja, datoer og begrunnelse.
- Trygdetid: teller 0–40, kilde og datoer. Kilden «ingen» låser telleren på 40.
  Skjermbildet kaller ikke trygdetid et vilkår.
- Neste i steg 2 kaller endepunktet for fullføring og viser valideringsfeilene i
  en `ErrorSummary`, som i steg 1, også feilen for trygdetid.
- Steget for simulering viser tilkjent ytelse øverst per periode med satsbeløp,
  brøk og tilkjent ytelse, merket som tilkjent ytelse.
- API-klienten genereres på nytt etter at backend er merget, etter ADR-0002.
- Komponentene for institusjon og tidslinjen for perioder med rett slettes.

## Beslutninger om testing

En god test sjekker oppførsel gjennom en offentlig testflate. Den beskriver
regelen, ikke rekkefølgen av interne kall. Reglene testes som rene funksjoner
uten Spring og uten mocks, og Spring-skallet testes tynt.

- **Utledningen av tilkjent ytelse** er hovedtestflaten og dekker:
  - perioder med oppfylte inngangsvilkår, inkludert § 6-9 og flere diagnoser
  - at hull i ekstrautgiftene betyr «nei»
  - valideringen av trygdetid og R1 for steg 2
  - at perioder utenfor ikke tas med
  - de fire reglene i § 22-12, hver for seg og kombinert i samme måned
  - satsendring fra Stortinget midt i en periode
  - eksakte `BigDecimal`-beløp
  - at 0/40 ikke gir noen andel
  - at like måneder slås sammen

  Eksempelet i dokumentet om utledning av tilkjent ytelse blir en test.
  Forbilder: `PeriodeMedRettUtlederTest` og `MedlemskapOgDiagnoseTidslinjeTest`.
- **Utfallet av inngangsvilkårene** (videre, avslag eller opphør) testes som ren
  funksjon i samme stil.
- **Listen over satsbeløp:** ingen overlapp, ingen hull per sats, første dato er
  1.1.1997, oppslag utenfor listen kaster feil, og stikkprøver mot rundskrivet.
- **Servicene for ekstrautgifter og trygdetid:** lagring, overlapp,
  endringshistorikk og at tilkjent ytelse slettes ved lagring og sletting.
  Forbilde: `VilkårMedlemskapServiceTest`.
- **Kopiering:** bare `JA` kopieres, alle trygdetidsrader kopieres, og ingen feil
  når ingenting blir kopiert. Forbilde: `KopierVilkårServiceTest`.
- **Repository:** CHECK-constraints for sats og vurdering, teller og kilde, og
  `NUMERIC` med desimaler på andelene. Forbilder: `VilkårRepositoryTest` og
  `TilkjentYtelseRepositoryTest`.
- **Servicene for de to stegene:** én mockk-test per steg som sjekker
  redigerbarhet, ansvarlig saksbehandler, rekkefølgen hent → utled → slett →
  lagre, og endringshistorikk uten diagnose. Forbilde:
  `VilkårVurderingStegServiceTest`.
- **Vedtak:** sjekken av resultatet mot tilkjent ytelse i en
  førstegangsbehandling, og at avslag ikke krever tilkjent ytelse. Forbilde:
  `VedtakServiceTest`.
- **Tilgangskontroll:** de nye endepunktene blir med i registeret som sjekker at
  alle endepunkter har tilgangskontroll.
- **Frontend:** `npx tsc` passerer. Det kommer ingen nye tester.

## Utenfor omfang

- Institusjon etter § 6-8. Kommer tilbake sammen med ekstrautgiftene før prod.
- Utgiftstyper og fast sats ut fra diagnose. Saksbehandler skriver det i
  begrunnelsen.
- Innhenting av trygdetid fra Pesys. Når den kommer, må teamet avgjøre om den
  hentede trygdetiden blir faktagrunnlag som saksbehandler kan overstyre, eller
  om den erstatter radene saksbehandler legger inn.
- Automatisk utregning av vedtaksresultatet og sjekk av resultatet i
  revurderinger.
- Automatisk behandling når Stortinget endrer satsene, for løpende saker.
- Egen side for tilkjent ytelse.
- Endringer i migreringen. Kravene om § 22-12 står i ticket 16 og 23 i
  [migrering](../migrering/).
- Hjelpestønad.
- Hva vedtaksbrevet skal si om sats, trygdetid og avslag etter § 6-3 tredje ledd.

## Andre merknader

- PR 116 må være merget før arbeidet starter. Det nye arbeidet endrer de samme
  tjenestene for tilkjent ytelse og vedtak, og migreringene starter etter `V48`.
- Løsningen kjører bare i `dev-gcp` uten produksjonsdata. Å droppe tabellene
  for institusjon og perioder med rett mister ingen data av betydning.
- Åpne spørsmål til fag står i ADR-0011 og ADR-0014: om § 22-12 skal leses
  bokstavelig, om trygdetid 0/40 kan forekomme, hvordan 50 øre rundes og datoen
  for satsene fra 2024. Implementasjonen bruker tolkningene i spec-en til fag
  har svart.
- Diagnose er helseopplysning etter GDPR artikkel 9. Ingen nye responser,
  endringshistorikk eller logger får inneholde diagnose.

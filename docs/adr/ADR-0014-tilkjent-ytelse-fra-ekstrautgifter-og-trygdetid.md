# ADR-0014: Steg 2 regner ut tilkjent ytelse fra ekstrautgifter og trygdetid

**Dato:** 2026-10-30
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad
**Bygger på:** [ADR-0006](ADR-0006-fem-vilkaar-med-inngangsvilkaar-som-foerste-steg.md),
[ADR-0008](ADR-0008-deterministisk-logikk-utenfor-spring.md),
[ADR-0011](ADR-0011-avrunding-og-utbetalingsgrunnlag-hoerer-til-oppdrag.md),
[ADR-0013](ADR-0013-migrering-fra-infotrygd-som-i-ef.md)

## Kontekst

ADR-0006 bestemte at steget etter inngangsvilkårene vurderer nødvendige
ekstrautgifter og trygdetid, og at trygdetid ikke er et vilkår. Denne ADR-en
bestemmer hvordan ekstrautgiftene og trygdetiden blir til tilkjent ytelse.
ADR-0011 krever at tilkjent ytelse beholder desimalene.

To paragrafer i folketrygdloven styrer utregningen. § 22-12 sier hvilken måned
utbetalingen starter, endres og stopper. § 22-11 sier at terminbeløp avrundes
til nærmeste hele krone. Reglene er oppsummert i
[utledning av tilkjent ytelse](../tilkjent-ytelse/utledning-av-tilkjent-ytelse.md).

## Beslutning

### Tilkjent ytelse regnes ut og lagres når steg 2 fullføres

`POST /api/behandling/{behandlingId}/sats-og-trygdetid/fullfor` validerer steget,
regner ut tilkjent ytelse og lagrer den. Utregningen er en ren funksjon etter
ADR-0008. Inn går vilkårsradene, trygdetidsradene og listen over satsbeløp, og
ut går andeler per måned. Servicen sletter tilkjent ytelse for behandlingen og
lagrer den på nytt i samme transaksjon. Startmåned settes etter ADR-0013.

Ekstrautgiftene ligger på `/vilkar/ekstrautgifter`. Trygdetid er ikke et vilkår
og ligger på `/trygdetid`.

Tilkjent ytelse for en måned er satsbeløpet × trygdetid / 40, lagret som
`BigDecimal` uten avrunding. Satsbeløpet er et helt kronebeløp, så resultatet har
aldri mer enn tre desimaler. `andel_tilkjent_ytelse.belop` er `NUMERIC`. En
måned med 0 kr gir ingen andel, men regnes som en måned med rett i vedtaket og i
brevet.

### Hull og perioder utenfor

- Et tidsrom uten vurdering av ekstrautgifter betyr «nei», som for
  inngangsvilkårene.
- Trygdetiden må dekke alle tidsrom der ekstrautgiftene er `JA`. Et hull gir
  valideringsfeil. Trygdetid har ingen «nei», så et hull har ingen lovlig
  tolkning.
- Er det minst én periode med oppfylte inngangsvilkår, må minst én
  ekstrautgiftsperiode være vurdert.
- Perioder for ekstrautgifter og trygdetid som ligger utenfor periodene med
  oppfylte inngangsvilkår, tar beregningen ikke med. Frontend markerer dem.
- Valideringsfeilen har én feil per vilkårstype og én for trygdetid. Feilen for
  trygdetid har en egen nøkkel, siden `VilkårType` ikke har noen verdi for
  trygdetid.

### § 22-12 ligger i én ren funksjon

Funksjonen følger alle fire reglene i § 22-12, med henvisning til ledd:

- Retten gjelder fra måneden etter at vilkårene er oppfylt.
- Utbetalingen stanses ved utgangen av måneden retten faller bort.
- En økning gjelder fra samme måned.
- En reduksjon gjelder fra måneden etter.

Regelen om økning og reduksjon gjelder hver endring av sats og trygdetid for
seg. Vi leser loven bokstavelig, også når retten oppstår den 1. i en måned.
Regelen er merket som ubekreftet i KDoc til fag har svart.

### Satsbeløpene ligger i kode

Satsbeløpene ligger i en Kotlin-liste i et `object`, som `SatsService` i
`familie-ba-sak`. Hver rad har sats, månedsbeløp, `gyldigFraOgMed` og
`gyldigTilOgMed`. Listen har med hele historikken fra 1.1.1997. Ber systemet om et
satsbeløp for en dato utenfor listen, kaster det en feil i stedet for å bruke
nærmeste beløp. En test sjekker at periodene ikke overlapper og ikke har hull.

Vi bruker månedsbeløpet fra
[rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/v1-06-03), ikke
årsbeløpet delt på 12. Unntaket er sats 2 i 2012, der månedsbeløpet i
rundskrivet ikke stemmer med årsbeløpet. Der bruker vi årsbeløpet delt på 12,
som Infotrygd har i alle utbetalingene, se ADR-0011.

### Tilkjent ytelse slettes når et vilkår eller trygdetiden endres

Servicen for fellesdelen av vilkårsperiodene sletter tilkjent ytelse for
behandlingen i samme transaksjon som en periode lagres eller slettes. Slettingen
ligger i fellesdelen, ikke i `VilkårPeriodeService`, så den gjelder både
vilkårene og trygdetiden. Et vedtak om innvilgelse og simuleringen stopper med
en melding om å fullføre steg 2 hvis tilkjent ytelse mangler.

### Avslag eller opphør når ingen perioder har oppfylte inngangsvilkår

Backend avgjør utfallet og sender det med i svaret fra
`POST /api/behandling/{behandlingId}/inngangsvilkar/fullfor`:

| Situasjon | Utfall | Neste steg |
|---|---|---|
| Ingen tidligere iverksatt behandling | Avslag | Brev. Steg 2 og simulering hoppes over |
| Revurdering | Opphør | Steg 2 viser at saken opphører. Neste regner ut tilkjent ytelse, slik at oppdrag kan opphøre |

### Vedtaket sjekkes mot tilkjent ytelse

Saksbehandler velger fortsatt resultatet. I en førstegangsbehandling krever
`INNVILGET` minst én måned med rett, og `AVSLÅTT` krever at det ikke finnes noen.
For revurderinger må sjekken sammenligne med forrige behandling. Den bygger vi
sammen med revurdering og opphør.

### Kopiering til ny behandling

En ny behandling kopierer `JA`-rader for medlemskap, diagnose og ekstrautgifter.
Det er samme regel som i `familie-ba-sak`, se
[vilkår.md](../vilkår/vilkår.md#kopiering-til-ny-behandling). Trygdetid er ikke
et vilkår og kopieres for seg, med alle rader. En revurdering etter et avslag
kan starte uten vilkår.

## Alternativer vurdert

### Regne ut tilkjent ytelse i vedtakssteget

Forkastet. Saksbehandler skal se beregningen før simulering, og vedtaket eier
ikke opplysningene beregningen bygger på.

### Regne ut tilkjent ytelse ved hvert oppslag

Forkastet. Simulering og oppdrag trenger et lagret resultat, og vi vil kunne se
i ettertid hva saksbehandler fullførte.

### Satsbeløp i database eller i en fil under `resources`

Forkastet. Satsene endres høyst én gang i året. En endring i kode går gjennom
review, er sporbar i git og testes som annen kode. Kostnaden er at en ny sats
krever en deploy.

### Årsbeløp delt på 12

Forkastet. Delingen gir desimaler som aldri slutter, og månedsbeløpet er det
Infotrygd har utbetalt. Kontrollen mot Infotrygd i ADR-0011 sammenligner
månedsbeløp.

### Hull i ekstrautgiftene som valideringsfeil

Forkastet. Det ville skilt seg fra inngangsvilkårene og fra barnetrygd, der en
periode uten vurdering betyr at vilkåret ikke er oppfylt.

### Hull i trygdetiden som 40/40

Forkastet. Glemmer saksbehandler å legge inn alderspensjon eller uføretrygd,
betaler vi for mye.

## Konsekvenser

- Simulering og oppdrag leser `BigDecimal` og avrunder med `HALF_UP` etter
  ADR-0011.
- Tilkjent ytelse vises på simuleringssiden i første omgang, merket som tilkjent
  ytelse og ikke som utbetalingsbeløp. Den kan få en egen side senere.
- Når Stortinget endrer satsene, må listen oppdateres og deployes før de nye
  satsene gjelder. Løpende saker får ikke de nye beløpene uten en ny behandling.
- Vilkårene i en migrering må starte i måneden før startmåned. Ellers mister den
  første revurderingen startmåned eller flytter den bakover. Se
  [migrering og § 22-12](../tilkjent-ytelse/utledning-av-tilkjent-ytelse.md#migrering-og--22-12)
  og ticket 16 og 23 i `.scratch/migrering/issues/`.

## Aksjonspunkter

- [ ] Fag bekrefter at § 22-12 skal leses bokstavelig, også når retten oppstår
      den 1. i en måned.
- [ ] Fag bekrefter om trygdetid 0/40 kan forekomme.
- [ ] Teamet bestemmer hvordan løpende saker får nye satsbeløp når Stortinget
      endrer satsene, for eksempel med en automatisk behandling for satsendring
      som i barnetrygd.
- [ ] Teamet bygger sjekken av vedtaksresultatet for revurderinger sammen med
      revurdering og opphør.

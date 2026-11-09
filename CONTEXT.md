# Grunnstønad

Saksbehandling av grunnstønad etter folketrygdloven kapittel 6: fra
faktagrunnlag, via vilkår, til tilkjent ytelse. Alt periodiseres.

## Faktagrunnlag

**Faktagrunnlag**:
Opplysninger fra søker, registre eller fagpersoner som saksbehandler vurderer vilkårene ut fra. Kan komme før eller underveis i behandlingen.
_Avoid_: Behandlingsgrunnlag, grunnlagsdata, grunnlag (alene), saksopplysninger

**Legeerklæring**:
Erklæring fra lege eller spesialist om søkers diagnose. Kommer med søknaden, ved innhenting eller ved ettersending.

**Ettersending**:
Dokumenter som mangler i søknaden, og som saksbehandler ber søker om å sende inn underveis i behandlingen.

**Innhenting**:
At Nav henter opplysninger fra andre enn søker, enten fra registre eller fra lege, spesialist eller medisinsk ekspert.
_Avoid_: Ettersending (for opplysninger som ikke kommer fra søker)

**Unntaksperiode for medlemskap**:
En periode i medlemskapsregisteret der søker er medlem eller ikke medlem av folketrygden på et annet grunnlag enn bosted i Norge. At søker ikke har noen unntaksperioder for medlemskap, betyr ikke at søker ikke er medlem.

**Trygdedekning**:
Hvilke kapitler i folketrygdloven en unntaksperiode for medlemskap gir rett til ytelser etter. Bare trygdedekning som omfatter kapittel 6, gir medlemskap for grunnstønad.
_Avoid_: Dekningsgrad

**Medlemskapsperiode**:
En periode der saksbehandler har vurdert om søker er medlem av folketrygden, etter nasjonale regler eller EØS-reglene. Medlemskapsperiodene er vurderingen av vilkåret medlemskap og dekker hele tidslinjen.

**Forslag til medlemskapsperioder**:
En tidslinje som systemet utleder fra faktagrunnlaget for medlemskap. Hver periode har enten en foreslått vurdering, eller en grunn til at saksbehandler må vurdere den manuelt. Det blir til medlemskapsperioder først når saksbehandler godtar det.
_Avoid_: Automatisk vurdering, maskinell vurdering, aksjonspunkt

## Vilkår og trygdetid

**Vilkår**:
De fire forholdene som vurderes ja eller nei per periode i en behandling: medlemskap, diagnose, nødvendige ekstrautgifter og institusjon. Fag regner også trygdetid som et vilkår, men det gjør ikke løsningen. Institusjon er tatt ut av løsningen inntil videre, se ADR-0006.

**Inngangsvilkår**:
Vilkårene uten skjønn, medlemskap og diagnose, som vurderes først. De andre vilkårene og trygdetiden vurderes bare i perioder der begge er oppfylt.

**Periode med oppfylte inngangsvilkår**:
En periode der både medlemskap og diagnose er oppfylt.

**Nødvendige ekstrautgifter**:
Vilkåret som avgjør om søker har rett i en periode med oppfylte inngangsvilkår, og i så fall hvilken sats. Er ekstrautgiftene under laveste sats, er vilkåret ikke oppfylt, jf. § 6-3 tredje ledd.

**Hjemmelbokstav**:
Bokstaven i folketrygdloven § 6-3 første ledd som sier hva en nødvendig ekstrautgift går til: a) drift av tekniske hjelpemidler, b) transport, herunder drift av egen bil, c) hold av førerhund, f) fordyret kosthold, g) slitasje på klær og sengetøy og h) hold av servicehund. Bokstav d (telefon) og e (proteser, støttebandasje o.l.) gir ikke grunnstønad i nye tilfeller fra 1. januar 2020.
_Avoid_: Formål (alene), utgiftstype

**Standardsats**:
En sats som enkelte diagnoser gir uten individuell prøving av ekstrautgiftene, fordi personer med diagnosen regnes som en ensartet gruppe med like høye ekstrautgifter. Et eksempel er fordyret kosthold ved cøliaki. Diagnosene, aldersgrensene og satsene står i [rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/r06-00). En standardsats legges sammen med andre godkjente ekstrautgifter når satsen fastsettes.
_Avoid_: Fast sats, direkte plassert sats

**Trygdetid**:
Søkers trygdetid per periode, som en teller fra 0 til 40 og kilden alderspensjon, uføretrygd eller ingen. Ikke et vilkår: den har ingen ja/nei-vurdering, kan ikke gi avslag og reduserer bare tilkjent ytelse etter § 6-6. Har samme livsløp som vilkårene, med periode, begrunnelse og endringshistorikk.
_Avoid_: Trygdetidsvilkår, vilkåret trygdetid

**Trygdetidsbrøk**:
Brøken x/40 fra søkers alderspensjon eller uføretrygd. Er 40/40 når søker ikke har noen av dem. Kan ikke skjønnsvurderes.

## Behandling

**Migrering**:
At en løpende sak flyttes fra Infotrygd til ny løsning, slik at Infotrygd slutter å utbetale og ny løsning overtar utbetalingen fra inneværende måned. Systemet gjør det med ett klikk fra saksbehandler, uten vedtaksbrev og uten beslutter, som i EF. Endrer ikke hva stønadsmottaker får utbetalt.
_Avoid_: Konvertering, overføring

**Migreringsbehandling**:
Den første behandlingen i en migrert sak, med årsak `MIGRERING`. Vilkårene er satt til oppfylt av systemet og er ikke vurdert av noen.

**Migrert sak**:
En sak der den første behandlingen er en migreringsbehandling.

**Startmåned**:
Den første måneden ny løsning eier utbetalingen for en sak. Er den tidligste av første andel og forrige behandlings startmåned. En vanlig revurdering kan flytte den bakover og ta over måneder Infotrygd har utbetalt for. Tilsvarer `startdato` i EF.
_Avoid_: Migreringsdato, startdato, migrert fra og med

## Utfall

**Avslag**:
Vedtak om at søker ikke har rett i noen av månedene det er søkt om.
_Avoid_: Opphør (når søker ikke har hatt en løpende ytelse)

**Opphør**:
Vedtak om at en løpende rett til grunnstønad faller bort fra en gitt måned, slik at utbetalingen må stoppes fra den måneden.
_Avoid_: Avslag (når søker har hatt en løpende ytelse)

**Sats**:
Ett av seks nivåer for grunnstønad, sats 1 til 6, bestemt av de nødvendige ekstrautgiftene i en periode. Ekstrautgifter under laveste sats gir ikke rett.
_Avoid_: Sats om kronebeløpet (det er satsbeløpet)

**Satsbeløp**:
Månedsbeløpet Stortinget har fastsatt for en sats i en gitt periode. Satsbeløpet for samme sats endres over tid, vanligvis fra 1. januar.
_Avoid_: Årsbeløp delt på 12

**Tilkjent ytelse**:
Beløpet søker har rett til per måned: satsbeløpet redusert med trygdetidsbrøken, uten avrunding.
_Avoid_: Utbetalt beløp (det er beløpet etter avrunding)

## Utbetaling

**Utbetalingsbeløp**:
Tilkjent ytelse avrundet til nærmeste hele krone, jf. § 22-11. Det er beløpet som faktisk utbetales.
_Avoid_: Tilkjent ytelse, sats

**Utbetalingsandel**:
En periode med samme utbetalingsbeløp, slik den sendes til Oppdrag.
_Avoid_: Andel tilkjent ytelse, utbetalingsperiode

**Infotrygd-beløp**:
Månedsbeløpet for grunnstønad som Infotrygd har vedtatt for en måned. Er det samme som Oppdrag utbetaler, til ny løsning endrer det.
_Avoid_: Utbetalt beløp, beløp fra Infotrygd

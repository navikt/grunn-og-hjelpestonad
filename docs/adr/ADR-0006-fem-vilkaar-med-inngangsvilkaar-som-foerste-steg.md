# ADR-0006: Fem vilkår, med inngangsvilkår som første steg

**Dato:** 2026-09-29
**Status:** Foreslått
**Beslutningstakere:** Teamet som forvalter grunn- og hjelpestønad
**Erstatter delvis:** [ADR-0003](ADR-0003-vilkaarsmodell-for-grunnstoenad.md),
[ADR-0004](ADR-0004-vilkaarssteg-utleder-perioder-med-rett.md)

## Kontekst

ADR-0003 innførte tre vilkår: medlemskap, diagnose og institusjon. Nødvendige
ekstrautgifter og trygdetid ble lagt til satssteget. ADR-0004 lot vilkårssteget
utlede `PeriodeMedRett` fra alle de tre vilkårene samlet.

Etter en samtale med fag er bildet et annet:

- Fag regner fem vilkår: medlemskap, diagnose, nødvendige ekstrautgifter,
  institusjon og trygdetid.
- Medlemskap og diagnose vurderes uten skjønn. Er ett av dem ikke oppfylt,
  vurderer ikke saksbehandler de andre vilkårene.
- Opplysninger kan komme inn underveis i behandlingen, ved ettersending fra
  søker eller ved innhenting fra lege, spesialist eller medisinsk ekspert.

Modellen er tegnet i [docs/vilkår/vilkår.md](../vilkår/vilkår.md), og begrepene
er definert i [CONTEXT.md](../../CONTEXT.md).

## Beslutning

### Fem vilkår

| Vilkår | Hjemmel | Gruppe |
|---|---|---|
| Medlemskap | ftrl. kap. 2, jf. § 6-3 første ledd, og § 6-1 a | Inngangsvilkår |
| Diagnose | § 6-2, § 6-9 | Inngangsvilkår |
| Institusjon | § 6-8 | Vilkår |
| Nødvendige ekstrautgifter | § 6-3 | Vilkår |
| Trygdetid | § 6-6 | Vilkår |

Vi bruker «vilkår» om alle fem, og «inngangsvilkår» om medlemskap og diagnose.

### Inngangsvilkårene er første steg

Medlemskap og diagnose vurderes i det første steget. De andre vilkårene
vurderes bare i perioder med oppfylte inngangsvilkår. Er det ingen slike
perioder, blir det avslag uten flere vurderinger.

§ 6-9 hører til inngangsvilkårene: er diagnosen en yrkesskade, lempes
medlemskapskravet.

### Alt periodiseres

Faktagrunnlaget, hvert vilkår og tilkjent ytelse periodiseres. Tilkjent
ytelse er tidslinjen av sats × trygdetidsbrøk i periodene der søker har rett.

### Institusjon slår av retten, med ekstrautgifter som unntak

Opphold på institusjon gir ikke rett i perioden. Unntaket er når søker har
ekstrautgifter ut over det institusjonen dekker. Da fastsettes satsen bare ut
fra disse utgiftene.

### `PeriodeMedRett` fjernes

Begrepet `PeriodeMedRett` fra ADR-0004 fjernes. Det som går videre fra første
steg, er perioder med oppfylte inngangsvilkår. Om retten foreligger, avgjøres
først når institusjon og ekstrautgifter er vurdert.

## Hva som erstattes

Fra **ADR-0003**:

- Tabellen med tre vilkår. Den erstattes av de fem vilkårene over.
- «Nødvendige ekstrautgifter er ikke et vilkår i denne modellen».
- «Ekstrautgifter og sats flyttes ut av vilkårssteget».
- Avgrensningen som legger reduksjon ved manglende trygdetid til satsene.

Fra **ADR-0004**:

- Vilkårssteget med tre vilkår. Det første steget vurderer nå bare
  inngangsvilkårene.
- `PeriodeMedRett` og tabellen `periode_med_rett`.

Det som ikke er nevnt her, gjelder fortsatt. Det gjelder blant annet egne
tabeller per vilkår, diagnose som helseopplysning, regelen om rene funksjoner og
reglene for yrkesskade i utledningen.

## Alternativer vurdert

### Beholde modellen fra ADR-0003 og ADR-0004

Tre vilkår i vilkårssteget, og ekstrautgifter og trygdetid i satssteget.
Forkastet fordi modellen ikke stemmer med hvordan fag beskriver vilkårene.
Den lar heller ikke saksbehandler stoppe når et inngangsvilkår ikke er oppfylt.

## Åpne spørsmål

1. **Trygdetid: vilkår eller reduksjon?** Trygdetid kan aldri gi avslag, bare
   redusere beløpet. Juridisk kan det derfor være mer presist å kalle det en
   reduksjon. Enn så lenge kaller vi trygdetid et vilkår, fordi fag gjør det.
2. **Trenger perioder med oppfylte inngangsvilkår en egen tabell?** Eller skal
   periodene utledes fra vilkårsradene hver gang de leses?
3. **Hvilke steg kommer etter inngangsvilkårene?** Det er ikke bestemt om
   institusjon, ekstrautgifter og trygdetid vurderes i ett eller flere steg,
   eller i hvilken rekkefølge.

## Konsekvenser

- Vilkårssteget i backend og frontend, som i dag vurderer institusjon sammen
  med inngangsvilkårene, må endres.
- `periode_med_rett`, `PeriodeMedRettUtleder` og `PerioderMedRettTidslinje` i
  steget «Sats og trygdetid» må erstattes når de neste stegene er bestemt.
- Kravet om minst én vurdert periode per vilkår (R1 i ADR-0004) gjelder i første
  steg bare medlemskap og diagnose.

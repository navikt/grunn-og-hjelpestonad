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

Periodene med oppfylte inngangsvilkår lagres ikke. En ren funksjon regner dem ut
fra vilkårsradene når de trengs. Tabellen `periode_med_rett` og
`PeriodeMedRett*` fjernes. Første steg fullføres med
`POST /api/behandling/{behandlingId}/inngangsvilkar/fullfor`. Endepunktet
validerer, lagrer ingenting og skriver «Vurdering av inngangsvilkår fullført» i
endringshistorikken.

### Steg 2 er nødvendige ekstrautgifter og trygdetid

Steget etter inngangsvilkårene heter «Sats og trygdetid» og vurderer to vilkår:

- **Nødvendige ekstrautgifter** (`VilkårEkstrautgifter`) har `vurdering`
  (`JA`/`NEI`) og `sats` (1–6). Satsen er påkrevd ved `JA` og tom ved `NEI`.
  `NEI` gir ingen rett i perioden, med § 6-3 tredje ledd som hjemmel.
- **Trygdetid** har en teller fra 0 til 40 og en kilde, men ingen `JA`/`NEI`.
  Søker kan ha rett uansett trygdetid.

Når steget fullføres, regner saken ut og lagrer tilkjent ytelse. Beslutningen
står i [ADR-0014](ADR-0014-tilkjent-ytelse-fra-ekstrautgifter-og-trygdetid.md),
og reglene i
[utledning av tilkjent ytelse](../tilkjent-ytelse/utledning-av-tilkjent-ytelse.md).

### Institusjon tas ut av løsningen inntil videre

Institusjon er fortsatt et vilkår etter § 6-8, men vurderingen henger sammen med
ekstrautgiftene: søker kan ha ekstrautgifter som institusjonen ikke dekker. Det
krever skjønn, og modellen er ikke ferdig. Vi fjerner derfor alt for
institusjon: tabellen `vilkar_institusjon`, `VilkårType`-verdien, koden og
skjermbildet. Verdiene i `Oppholdstype` og `Unntakshjemmel` var heller aldri
sjekket med fag, se ADR-0003. Koden finnes i git-historikken. Når vilkåret
kommer tilbake, designer vi det sammen med ekstrautgiftene.

Uten institusjonsvilkåret kan løsningen innvilge grunnstønad til noen som bor på
institusjon. Løsningen kan derfor ikke tas i bruk i prod før vilkåret er på
plass.

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

## Avklarte spørsmål

| Spørsmål | Avklaring |
|---|---|
| Trenger perioder med oppfylte inngangsvilkår en egen tabell? | Nei. En ren funksjon regner dem ut fra vilkårsradene |
| Hvilke steg kommer etter inngangsvilkårene? | Ett steg, «Sats og trygdetid», med nødvendige ekstrautgifter og trygdetid. Institusjon venter |

## Aksjonspunkter

- [ ] Institusjon må tilbake i løsningen før prod, designet sammen med
      nødvendige ekstrautgifter.

## Konsekvenser

- Vilkårssteget i backend og frontend, som i dag vurderer institusjon sammen
  med inngangsvilkårene, må endres.
- `periode_med_rett`, `PeriodeMedRettUtleder` og `PerioderMedRettTidslinje`
  fjernes. `vilkar_institusjon` droppes i en ny migrering.
- `POST /vilkar/fullfor` erstattes av `POST /inngangsvilkar/fullfor`.
- Kravet om minst én vurdert periode per vilkår (R1 i ADR-0004) gjelder i første
  steg bare medlemskap og diagnose.

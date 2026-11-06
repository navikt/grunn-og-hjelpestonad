# 15: Avklar modellen for nødvendige ekstrautgifter

**What to build:** En beslutning om hvordan vilkåret nødvendige ekstrautgifter modelleres: hva saksbehandler registrerer, hva systemet regner ut og hva som lagres. Svaret avgjør om koden for ticket 07 beholdes, og hva utledningen i ticket 09 får inn. Teamet og fag må avgjøre det sammen. Én utvikler eller en agent kan ikke avgjøre det alene.

**Blocked by:** None (can start immediately). Spørsmålet om migrering trenger svaret fra ticket 18.

**Status:** needs-info

Spec: [spec.md](../spec.md). Bakgrunn: ADR-0006, ADR-0014 og ADR-0008. Variantene er vist i [forslag-ekstrautgifter.html](../forslag-ekstrautgifter.html). Åpne fila i en nettleser.

- [ ] Teamet og fag har valgt variant.
- [ ] Hvert spørsmål under har et svar under `## Answer`.
- [ ] En ny ADR erstatter punktet om ekstrautgifter i ADR-0006 og merknaden i ADR-0014.
- [ ] Spec-en og ticket 07, 09, 10 og 11 er oppdatert. Migrering/16 er sjekket, fordi den setter vilkårene til oppfylt og henter sats fra Infotrygd.
- [ ] `CONTEXT.md` er oppdatert: «Nødvendige ekstrautgifter», og «Utgiftspost» hvis C eller D blir valgt.

## Avklart

1. **Utgiftene legges sammen før satsen fastsettes.** Rundskrivet til § 6-3 tredje ledd: «Dersom det foreligger ekstrautgifter til flere formål […] skal utgiftene legges sammen før satsen fastsettes.» En standardsats legges sammen med andre godkjente ekstrautgifter.
2. **Satsen regnes ikke ut på nytt når Stortinget endrer satsbeløpene.** Søker beholder satsen og får det nye beløpet for den. Satsen endres bare når vilkårene vurderes på nytt, eller når en standardsats endres med alderen, for eksempel når standardsatsen for cøliaki går fra sats 2 til sats 1. Hvordan aldersendringen skjer, er et spørsmål i ticket 16. En saksbehandler har bekreftet dette.
3. **En satsendring i vurderingsperioden kan i teorien gi lavere sats.** Vurderingsperioden går tre måneder bak søknaden. Går den over en satsendring, sammenlignes summen med satsbeløpene som gjaldt i hver måned, og nedsettelsen følger § 22-12 femte ledd. I praksis setter saksbehandler ofte samme sats for hele perioden, fordi beløpene er skjønnsmessige. Fra saksbehandleren:
   > I praksis vil dette svært sjelden skje, siden 1) differansen ved en slik satsjustering normalt er ubetydelig, og 2) vi har så stor grad av skjønnsmessige vurderinger på størrelsen på ekstrautgifter, samt at det ikke er et absolutt krav at søkeren skal dokumentere krone for krone, at vi mest sannsynlig i dette tilfellet ville ha vurdert denne til sats 2 hele veien.
4. **Løsningen må kunne migrere saker fra Infotrygd.** Infotrygd har én hjemmelbokstav, én eller to diagnosekoder og én sats per periode, og Infotrygd-beløpet, det som utbetales hver måned. Infotrygd har ikke beløpene for selve ekstrautgiftene. Migreringen tar med satsen fra siste periode i Infotrygd.
5. **En migreringsbehandling tar med det Infotrygd har, nøyaktig som det er.** Den vurderer ikke vilkårene på nytt, og saksbehandler kan ikke legge til, endre eller slette noe. Søker får samme sats og samme beløp som i Infotrygd. I en revurdering kan saksbehandler endre og slette det som kom fra Infotrygd, og satsen kan endres fra perioden saksbehandler vurderer.
6. **Trygdetid brukes etter at satsen er satt, aldri på ekstrautgiftene.** Derfor er det satsen fra Infotrygd som føres inn, ikke Infotrygd-beløpet. Infotrygd-beløpet er satsbeløpet ganget med trygdetiden.
7. **Oppsummeringen av vedtaket viser beløpet per måned** (satsbeløp ganget med trygdetid) for hver periode. For en migrert sak viser den også Infotrygd-beløpet, slik at saksbehandler ser om beløpet endres.
8. **Hjemmelbokstaven er bokstaven i § 6-3 første ledd**, se «Hjemmelbokstav» i `CONTEXT.md`.

## Variantene

| | A. Sats direkte | B. Sats med kobling | C. Utgiftsposter | D. Utgiftsposter og vedtatt sats |
|---|---|---|---|---|
| Saksbehandler registrerer | Ja/nei og sats per periode | Som A, pluss hjemmelbokstaver og diagnoser per periode | Hver godkjent utgift med bokstav, diagnoser, beløp og periode | Som C, og bekrefter eller overstyrer satsen systemet foreslår |
| Systemet regner ut | Ingenting | Ingenting | Sum og sats per periode | Forslag til sats per periode |
| Lagres | Vurdering og sats | Vurdering, sats, bokstaver og diagnoser | Utgiftene. Satsen utledes og lagres ikke | Utgiftene og den vedtatte satsen |
| Migrert sak fra Infotrygd | Passer: én sats per periode | Passer: én bokstav og diagnosene per periode | Passer i migreringen når utgiften fra Infotrygd har satsen som grunnlag. Ved revurdering regnes også tidligere perioder ut på nytt | Passer: satsen fra Infotrygd blir vedtatt sats og endres bare fra perioden som revurderes |
| Satsen står seg når satsbeløpene endres | Ja | Ja | Nei, den utledes på nytt | Ja |
| Arbeid | Lite: koden finnes i en stash, se ticket 07 | Lite | Stort | Stort, men tabellen fra A kan bli den vedtatte satsen |

C kan migrere saker når utgiften fra Infotrygd har satsen som grunnlag, men oppfyller ikke punkt 2 og 5 under «Avklart». C lagrer ingen sats. Nye satsbeløp eller en endret utgift kan derfor gi en annen sats også i perioder ingen har vurdert på nytt, for eksempel når saksbehandler erstatter posten fra Infotrygd i en revurdering. Med en lagret sats blir C til D.

## Forslag fra Halvor (ikke avgjort)

- Variant D. Handoffen valgte C før det var avklart at satsen ikke skal regnes ut på nytt når satsbeløpene endres, og at saker skal migreres.
- Høyst én utgiftspost per hjemmelbokstav per periode. To transportutgifter blir ett samlet beløp på bokstav b.
- En utgiftspost kan ha flere diagnoser.
- En migrert utgiftspost har satsen fra Infotrygd som grunnlag, slik en standardsats har. Infotrygd-beløpet vises i oppsummeringen av vedtaket. Se spørsmål 13.

## Spørsmål

Hvert spørsmål har et forslag, hvem som avgjør og hvilke tickets svaret påvirker.

### 1. Hvilken variant?

A, B, C eller D. Forslag: D.
**Avgjør:** teamet og fag. **Påvirker:** 07, 09, 10, 11 og spec-en.

### 2. Hva er en utgiftspost?

Gjelder C og D. Forslag: høyst én post per hjemmelbokstav per periode. Overlappende poster med samme bokstav forkortes, slik diagnosevilkåret gjør med diagnosekoden.
- Må begrunnelsen vise hvordan beløpet er satt sammen når flere utgifter ligger i samme post? Forslag: ja, i fritekst.
- Skal databasen håndheve regelen med en constraint, eller holder tidslinjen i servicen, som for diagnose? Forslag: tidslinjen.

**Avgjør:** fag (én post per bokstav) og teamet (constraint). **Påvirker:** 07.

### 3. Koblingen til diagnose

- Kobles posten til diagnosekoden eller til en bestemt diagnoseperiode? Koden overlever kopiering og deling av perioder. Det gjør ikke en rad-id. Forslag: diagnosekoden.
- Må minst én diagnose være valgt? Forslag: ja.
- Hva skjer når en valgt diagnose ikke er oppfylt i hele perioden til posten: feil, markering som for perioder utenfor inngangsvilkårene, eller ingenting? Forslag: markering.
- Egen koblingstabell eller liste på raden? Forslag: egen tabell.

**Avgjør:** fag (må hver utgift ha en diagnose?) og teamet (resten). **Påvirker:** 07.

### 4. Når blir vilkåret ja eller nei?

I C og D setter ikke saksbehandler ja eller nei. Det følger av summen.
- Utledes det fortløpende eller når steg 2 fullføres? Forslag: fortløpende i skjermbildet og lagret ved fullføring.
- Hvordan vet systemet at alle utgiftene er registrert? Trenger saksbehandler en egen bekreftelse? Forslag: fullføringen av steg 2 er bekreftelsen.
- Gjelder brukerhistorie 13 og 14 fortsatt (hull betyr nei, og feil når ingen ekstrautgifter er vurdert)? Forslag: ja.

**Avgjør:** teamet. **Påvirker:** 09, 10.

### 5. Avslag og avviste utgifter

- Hvor dokumenteres en utgift søker har krevd, men som ikke er nødvendig? Kan en post ha status «ikke godkjent» med begrunnelse? Forslag: ja.
- Hvordan skiller vedtaksbrevet mellom avslag fordi summen er under laveste sats (§ 6-3 tredje ledd) og avslag fordi utgiften ikke er nødvendig?
- Trenger brevet utgiftene, hjemmelbokstavene og summen, eller bare satsen?

**Avgjør:** fag. **Påvirker:** 07, brev.

### 6. Forslag og vedtatt sats

Gjelder D.
- Lagres den vedtatte satsen per periode eller per behandling? Forslag: per periode.
- Må saksbehandler begrunne en overstyring? Forslag: ja.
- Når skal en satsendring i vurderingsperioden dele perioden, og når holder det at saksbehandler setter én sats for hele perioden? Forslag: systemet foreslår delingen, og saksbehandler kan slå periodene sammen med begrunnelse.
- Hva kopieres til en revurdering: postene, den vedtatte satsen eller begge? Forslag: begge.

**Avgjør:** fag (overstyring og deling) og teamet (lagring og kopiering). **Påvirker:** 07, 09, 10.

### 7. Beløpet

- Lagres godkjent netto ekstrautgift, eller brutto og fradrag for vanlige utgifter (SIFO-referansebudsjettet, som rundskrivet bruker)? Forslag: netto.
- Registreres beløpet per måned eller per år? Satsbeløpene er månedsbeløp. Rundskrivet sammenligner årlige utgifter med årsbeløpet til satsen.

**Avgjør:** fag. **Påvirker:** 07.

### 8. Periodisering og § 22-12

Hver post har sin egen periode, så satsen blir en tidslinje. Hva går inn i utledningen av tilkjent ytelse: den vedtatte satsen per periode (D) eller postene (C)?
**Avgjør:** teamet. **Påvirker:** 09.

### 9. Transport etter 70 år

§ 6-3 fjerde ledd. Er det en regel per post med bokstav b?
**Avgjør:** fag. **Påvirker:** 07, 09.

### 10. Familien vurdert samlet

TRR-2009-1939. Er det innenfor eller utenfor det vi bygger nå? Forslag: utenfor.
**Avgjør:** fag.

### 11. Personvern

Koblingen mellom post og diagnose er en helseopplysning (GDPR artikkel 9). Endringshistorikken bygges i dag bare av faste felter. Hva skal stå der for en post? Forslag: bokstav, beløp og periode, men ikke diagnosekoden.
**Avgjør:** teamet. **Påvirker:** 07.

### 12. Kopiering til ny behandling

Postene har ikke ja eller nei. Kopieres alle, som trygdetid, eller bare godkjente? Forslag: bare godkjente, og den vedtatte satsen.
**Avgjør:** teamet. **Påvirker:** 07, ADR-0014.

### 13. Migrering og revurdering av en migrert sak

Infotrygd har én hjemmelbokstav, én eller to diagnosekoder, én sats og Infotrygd-beløpet per periode. Migreringen vurderer ikke vilkårene, så søker må få samme sats og beløp (punkt 5 under «Avklart»).
- Hva oppretter migreringen? Avklart for C og D: én post med hjemmelbokstaven og diagnosene, med satsen fra Infotrygd som grunnlag, slik en standardsats har. Posten teller med årsbeløpet til satsen, så summen gir samme sats. Infotrygd-beløpet vises i oppsummeringen av vedtaket, men teller ikke som ekstrautgift (punkt 6 under «Avklart»). I D blir satsen også vedtatt sats.
- Avklart: i en revurdering kan saksbehandler endre eller slette posten fra Infotrygd, som alle andre utgifter (punkt 5 under «Avklart»).
- Hva ser saksbehandler når en migrert sak revurderes?
- Hva gjør vi med hjemmelbokstav d og e fra før 2020, se ticket 18?

**Avgjør:** teamet og fag. **Påvirker:** migrering/16, 18.

## Konsekvenser for koden

Koden for variant A er fjernet fra branchen `noedvendige-ekstrautgifter` og ligger i en lokal stash, se ticket 07. Den har tabellen `vilkar_ekstrautgifter` (V52) med `vurdering` og `sats`, servicen, endepunktene, kopieringen av JA-rader og listen i steg 2. V52 ble aldri deployet. Velger vi A, kan koden hentes tilbake. Velger vi D, kan tabellen bli den vedtatte satsen per periode, med postene i en ny tabell.

## Comments

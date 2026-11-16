# 21: Avklar fra hvilken måned en ny trygdetidsbrøk gjelder

**What to build:** En beslutning fra fag om § 22-12 fjerde og femte ledd gjelder når trygdetidsbrøken endres, eller om brøken skal følge virkningstidspunktet for alderspensjonen eller uføretrygden. Svaret avgjør om utledningen av tilkjent ytelse fra ticket 09 må endres.

**Blocked by:** None (can start immediately)

**Status:** needs-info

Spec: [spec.md](../spec.md). Se også [utledning av tilkjent ytelse](../../../docs/tilkjent-ytelse/utledning-av-tilkjent-ytelse.md) og [ADR-0014](../../../docs/adr/ADR-0014-tilkjent-ytelse-fra-ekstrautgifter-og-trygdetid.md).

- [ ] Spørsmålene under har svar under `## Answer`.
- [ ] Dokumentet om utledning av tilkjent ytelse og ADR-0014 er oppdatert med svaret.
- [ ] Får svaret følger for koden, er `forskyvTilHeleMåneder` i `TilkjentYtelseUtleder` endret for trygdetiden, og testene under er oppdatert.

## Slik er det nå

Ticket 09 behandler en endring i trygdetidsbrøken som en endring etter § 22-12, på samme måte som en endring av sats:

- En høyere brøk gjelder fra samme måned (fjerde ledd).
- En lavere brøk gjelder fra måneden etter (femte ledd), også når den gjelder fra den 1.

Brøken kommer fra alderspensjonen eller uføretrygden, jf. § 6-6. Disse ytelsene starter normalt den 1. i en måned. Starter alderspensjonen 1. september med trygdetid 30/40, får søker grunnstønad med 40/40 i september og 30/40 fra oktober. Søker får altså én måned med full brøk etter at pensjonen har startet.

Fagdokumentet har samme regel i eksempelet sitt: alderspensjon med 30/40 fra 10. september gir 30/40 fra oktober.

Uansett svar må brøken flyttes til hele måneder, fordi tilkjent ytelse er per måned. Spørsmålet er hvilken måned den nye brøken gjelder fra.

## Spørsmål

1. Gjelder § 22-12 fjerde og femte ledd når trygdetidsbrøken endres? Eller skal brøken i grunnstønaden følge virkningstidspunktet for alderspensjonen eller uføretrygden? Forslag: § 22-12 gjelder, som i dag.
2. Alderspensjon med lavere brøk starter 1. september. Gjelder den lavere brøken i grunnstønaden fra september eller fra oktober? Forslag: oktober, etter femte ledd.
3. Gjelder svaret også når brøken går ned midt i en måned, som 10. september i eksempelet i fagdokumentet?
4. Når brøken går opp, for eksempel fordi uføretrygden faller bort og brøken går tilbake til 40/40, gjelder den nye brøken fra samme måned? Forslag: ja, etter fjerde ledd.
5. Hvordan gjorde Infotrygd det? Gir ny løsning en annen brøk enn Infotrygd for samme måned, får den første revurderingen etter en migrering et annet beløp, uten at noe i saken er endret.
6. Skal en endring i trygdetiden mellom to perioder med oppfylte vilkår telle som en endring etter § 22-12? Se eksempelet under. Teamet mener at trygdetiden ikke skal kuttes etter vilkårene, altså at mars får 40/40.

## Eksempel: trygdetiden endres mellom to perioder med oppfylte vilkår

- Vilkårene er oppfylt til 14. mars, og igjen fra 20. mars.
- Trygdetiden går fra 30/40 til 40/40 den 17. mars, i hullet.

Mars er siste måned med utbetaling for den første perioden. Den andre perioden gir utbetaling fra april.

| Trygdetiden | Mars | Fra april |
|---|---|---|
| Kuttes etter vilkårene | 30/40 | 40/40 |
| Kuttes ikke etter vilkårene, slik koden gjør nå | 40/40 | 40/40 |

Kuttes trygdetiden etter vilkårene, gjelder brøken bare der vilkårene er oppfylt. Den andre perioden starter da med en ny brøk, og økningen 17. mars teller ikke. Mars får brøken den første perioden hadde hele tiden.

Kuttes den ikke, er økningen 17. mars en økning etter fjerde ledd og gjelder fra mars.

Ticket 09 sa først at trygdetid utenfor periodene med oppfylte inngangsvilkår ikke skal påvirke resultatet. Teamet har valgt at trygdetiden ikke kuttes, og ticket 09, spec-en og ADR-0014 er oppdatert. `TilkjentYtelseUtleder` flytter `trygdetidTidslinje` direkte. Svarer fag at trygdetiden skal kuttes, må vi tilbake til å kombinere trygdetiden med satsen før den flyttes.

## Tester som avhenger av svaret

I `TilkjentYtelseUtlederTest`:

- `eksempelet i dokumentet om utledning av tilkjent ytelse`
- `femte ledd - lavere trygdetidsbrøk gjelder fra måneden etter, også når den gjelder fra den 1`
- `fjerde ledd - høyere trygdetidsbrøk gjelder fra samme måned`
- `økning av sats og reduksjon av trygdetidsbrøk i samme måned vurderes hver for seg`
- `en måned med 0 kr gir ingen andel`
- `løpende rett med trygdetid 0 av 40 gir siste andel med sluttdato`
- `økning av trygdetidsbrøken mellom to perioder med oppfylte vilkår gjelder fra samme måned` (spørsmål 6)

## Comments

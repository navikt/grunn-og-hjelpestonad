# 16: Avklar standardsatser

**What to build:** En beslutning om hvordan en standardsats registreres, og om systemet skal foreslå den. Fag må svare. Se «Standardsats» i `CONTEXT.md`.

**Blocked by:** None (can start immediately). Svaret bygges inn i modellen fra ticket 15.

**Status:** needs-info

Spec: [spec.md](../spec.md). Kilde: [rundskrivet til § 6-3](https://lovdata.no/nav/rundskriv/r06-00), avsnittene om fordyret kosthold og klesslitasje.

- [ ] Spørsmålene under har svar under `## Answer`.
- [ ] Svaret er tatt inn i ticket 15 eller i ADR-en som følger av den.

## Det rundskrivet sier

- Enkelte diagnoser gir «uten individuell prøving rett til en direkte plassert sats». Personene regnes som en ensartet gruppe med like høye ekstrautgifter.
- Standardsatsen legges sammen med andre konkrete godkjente ekstrautgifter når den samlede satsen fastsettes.
- Cøliaki, bokstav f: sats 1 fra 1 til 4 år, sats 2 fra 5 til 30 år og sats 1 fra 31 år. «Stønaden reduseres fra samme måned som personen fyller 31 år.» Gjelder glutenfri diett alene. Tilleggsallergier vurderes konkret.
- Hyperkinetiske forstyrrelser (ADHD med hyperaktivitet), bokstav g: sats 1 til barn under 18 år.
- Avsnittet om cøliaki er endret i 2026 (merket 6/26 og 7/26). Listen over diagnoser må sjekkes med fag før vi bygger noe.

## Spørsmål

1. Er en standardsats en utgiftspost med sats i stedet for beløp? Telles den med satsbeløpet når den legges sammen med andre utgifter? Forslag: ja til begge.
2. Skal systemet foreslå standardsatsen ut fra diagnosekode og alder, eller velger saksbehandler den alltid selv? Forslag: systemet foreslår, saksbehandler bekrefter.
3. Skal systemet dele perioden ved aldersgrensene (cøliaki ved 31 år, ADHD ved 18 år)? Forslag: ja.
4. Rundskrivet sier at cøliakisatsen reduseres fra samme måned som personen fyller 31 år. § 22-12 femte ledd sier at en reduksjon gjelder fra måneden etter. Hvilken regel gjelder?
5. Hvilke diagnoser har standardsats i dag, og hvor holder vi listen oppdatert?
6. Hvordan endres satsen når søker når en aldersgrense? Satsen endres ellers bare når vilkårene vurderes på nytt, men en standardsats endres med alderen. Forslag A: vedtaket inneholder allerede perioden etter aldersgrensen, så det trengs ingen ny behandling. Forslag B: systemet oppretter en automatisk behandling når søker når aldersgrensen, som barnetrygd gjør ved 6 og 18 år. A er enklere, men da må vedtaksbrevet si fra om endringen på forhånd.

## Comments

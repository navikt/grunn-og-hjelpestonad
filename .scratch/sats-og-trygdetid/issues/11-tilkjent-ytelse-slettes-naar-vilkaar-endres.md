# 11: Tilkjent ytelse slettes når et vilkår eller trygdetiden endres

**What to build:** En utdatert beregning kan ikke gå videre. Når saksbehandler lagrer eller sletter en periode for et vilkår eller for trygdetid, slettes tilkjent ytelse for behandlingen. Simulering og innvilgelse stopper med beskjed om å fullføre steg 2. Se ADR-0014 under «Tilkjent ytelse slettes når et vilkår endres».

**Blocked by:** 10

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Servicen for fellesdelen fra ticket 02 sletter tilkjent ytelse for behandlingen i samme transaksjon når en periode lagres eller slettes. Slettingen ligger i fellesdelen, ikke i `VilkårPeriodeService`, så den gjelder medlemskap, diagnose, ekstrautgifter og trygdetid.
- [ ] Kopiering til ny behandling sletter ikke tilkjent ytelse i forrige behandling.
- [ ] Simulering uten tilkjent ytelse gir en funksjonell feil om å fullføre steget «Sats og trygdetid».
- [ ] Et vedtak om innvilgelse uten tilkjent ytelse gir samme feil. Et vedtak om avslag krever ikke tilkjent ytelse.
- [ ] Frontend viser feilen og lenker til steg 2.
- [ ] Servicetestene for vilkårene og for trygdetid sjekker at tilkjent ytelse slettes ved lagring og sletting.

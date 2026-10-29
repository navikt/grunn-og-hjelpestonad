# 18: `vedtaksmelding` og sjekk av opphør i Infotrygd

**What to build:** Når en migrering iverksettes, får Infotrygd beskjed om å slutte å utbetale fra startmåned. En task sjekker etterpå at Infotrygd har sluttet, som `SjekkMigrertStatusIInfotrygdTask` i EF, og varsler hvis ikke.

**Blocked by:** 16

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Iverksettingen av en migrering oppretter `SendVedtakTilInfotrygdFeedTask` med `datoStartNyGrunnstønad` lik startmåned.
- [ ] En ny task sjekker at siste periode i Infotrygd slutter senest måneden før startmåned. Den kjører 16 minutter etter iverksettingen og prøver på nytt ved feil, som i EF.
- [ ] Etter siste forsøk går tasken til manuell oppfølging, og det finnes et varsel på det.
- [ ] Logger bruker behandlings-ID. Detaljer om perioder i Infotrygd og fnr går bare til team logs.

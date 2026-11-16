# 10: Steg 2 fullføres og lagrer tilkjent ytelse

**What to build:** Når saksbehandler trykker Neste i steg 2, regner systemet ut tilkjent ytelse med funksjonen fra ticket 09 og lagrer den. Valideringsfeil vises i `ErrorSummary`. Vedtakssteget regner ikke lenger ut tilkjent ytelse. Se ADR-0014 under «Tilkjent ytelse regnes ut og lagres når steg 2 fullføres».

**Blocked by:** 03, 09, PR 116 ([ticket 13 i migrering](../../migrering/issues/13-startmaaned-tilbake-paa-tilkjent-ytelse.md))

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] `POST /api/behandling/{behandlingId}/sats-og-trygdetid/fullfor` sjekker redigerbarhet og ansvarlig saksbehandler, henter vilkårene og trygdetiden, kaller utledningen og lagrer tilkjent ytelse.
- [ ] Lagringen bruker tjenesten for beregning av tilkjent ytelse fra PR 116, med startmåned regnet ut som der. Utregningen fra barnetilsyn er erstattet av funksjonen fra ticket 09.
- [ ] Servicen sletter og lagrer tilkjent ytelse på nytt i samme transaksjon, så gjentatte trykk gir samme resultat.
- [ ] Når det ikke er rett i noen måned, hopper steg 1 over steg 2, se ADR-0014. I en revurdering må tilkjent ytelse uten andeler likevel lagres, slik at oppdrag stopper utbetalingen. Avgjør om fullføringen av steg 1 eller simuleringen lagrer den.
- [ ] Vedtakssteget regner ikke ut tilkjent ytelse.
- [ ] Valideringsfeil returneres med 400 i samme format som i steg 1, med lokal exception handler på kontrolleren.
- [ ] Fullføringen sjekker på nytt at hver diagnose i ekstrautgiftene er vurdert til `JA` i hele perioden. Steg 1 kan endres etter at ekstrautgiftene er lagret, og kopieringen tar ikke med diagnoser vurdert til `NEI`. Bruk regelen i `EkstrautgifterRegler`, se ADR-0016.
- [ ] Endringshistorikken får en ny endringstype for fullført sats og trygdetid, med antall andeler og datoer, uten diagnose.
- [ ] Neste i steg 2 kaller endepunktet, viser feilene i `ErrorSummary`, også feilen for trygdetid som ikke har en vilkårstype, og går videre til simulering ved suksess. Knappen er deaktivert mens kallet pågår.
- [ ] Testen av steg-servicen sjekker tilgang, rekkefølgen hent → utled → slett → lagre, og endringshistorikk.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

# 08: Trygdetid

**What to build:** Saksbehandler kan legge inn trygdetid per periode i steg 2: en teller fra 0 til 40 og kilden alderspensjon, uføretrygd eller ingen. Trygdetid har ingen ja/nei-vurdering. Perioder utenfor periodene med oppfylte inngangsvilkår er markert. Se ADR-0006 under «Steg 2 er nødvendige ekstrautgifter og trygdetid».

**Blocked by:** 02, 05, 06

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En ny Flyway-migrering oppretter tabellen med fellesfeltene uten vurdering, `teller` og `kilde`. CHECK-constraints krever at `teller` er 0–40, at kilden `INGEN` gir `teller = 40`, og at datoene står i riktig rekkefølge. Tabellen har `ON DELETE CASCADE` fra behandling og indeks på behandling.
- [ ] Vilkårstypen har en ny verdi for trygdetid.
- [ ] Servicen bruker den felles livsløpsdelen fra ticket 02, uten «er oppfylt». Valideringen avviser teller utenfor 0–40 og `INGEN` med en annen teller enn 40 med 400.
- [ ] Endepunktene ligger på `/api/behandling/{behandlingId}/vilkar/trygdetid`, med hent, lagre og slett, og krever rollen saksbehandler.
- [ ] Kopieringen til ny behandling tar med alle trygdetidsrader.
- [ ] Steg 2 har en liste over trygdetidsperioder. Kilden «ingen» låser telleren på 40.
- [ ] Perioder utenfor periodene med oppfylte inngangsvilkår er markert i listen.
- [ ] Testene dekker servicen, repositoryet med CHECK-constraints og kopieringen.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

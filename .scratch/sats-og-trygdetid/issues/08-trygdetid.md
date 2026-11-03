# 08: Trygdetid

**What to build:** Saksbehandler kan legge inn trygdetid per periode i steg 2: en teller fra 0 til 40 og kilden alderspensjon, uføretrygd eller ingen. Perioder utenfor periodene med oppfylte inngangsvilkår er markert. Se ADR-0006 under «Steg 2 er nødvendige ekstrautgifter og trygdetid».

Trygdetid er ikke et vilkår. Den har ingen ja/nei-vurdering, kan ikke gi avslag og reduserer bare tilkjent ytelse etter § 6-6. Til trygdetiden hentes fra andre systemer, legger saksbehandler den inn for hånd med begrunnelse. Trygdetid bruker derfor fellesdelen fra ticket 02, men ingenting som er spesielt for vilkår.

Henting av trygdetid er utenfor omfang. Når den kommer, må teamet avgjøre om den hentede trygdetiden blir faktagrunnlag som saksbehandler kan overstyre, eller om den erstatter radene saksbehandler legger inn.

**Blocked by:** 02, 05, 06

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En ny Flyway-migrering oppretter tabellen `trygdetid`, ikke `vilkar_trygdetid`, med fellesfeltene uten vurdering, `teller` og `kilde`. CHECK-constraints krever at `teller` er 0–40, at kilden `INGEN` gir `teller = 40`, og at datoene står i riktig rekkefølge. Tabellen har `ON DELETE CASCADE` fra behandling og indeks på behandling.
- [ ] `VilkårType` får ingen verdi for trygdetid.
- [ ] Servicen bygger på servicen for fellesdelen fra ticket 02, ikke på `VilkårPeriodeService`. Valideringen avviser teller utenfor 0–40 og `INGEN` med en annen teller enn 40 med 400.
- [ ] Endringshistorikken får egne endringstyper for trygdetid som er opprettet, oppdatert og slettet, ikke `VILKÅR_VURDERING_*`. Detaljene har type og periode, ingen vurdering.
- [ ] Endepunktene ligger på `/api/behandling/{behandlingId}/trygdetid`, med hent, lagre og slett, og krever rollen saksbehandler.
- [ ] Kopieringen til ny behandling tar med alle trygdetidsrader. Trygdetid kopieres for seg, ikke som et vilkår.
- [ ] Steg 2 har en liste over trygdetidsperioder. Kilden «ingen» låser telleren på 40. Skjermbildet kaller ikke trygdetid et vilkår.
- [ ] Perioder utenfor periodene med oppfylte inngangsvilkår er markert i listen.
- [ ] Testene dekker servicen, repositoryet med CHECK-constraints og kopieringen.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

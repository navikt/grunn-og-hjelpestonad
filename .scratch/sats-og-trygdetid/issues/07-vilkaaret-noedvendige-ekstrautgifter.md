# 07: Vilkåret nødvendige ekstrautgifter

**What to build:** Saksbehandler kan registrere nødvendige ekstrautgifter i steg 2 etter modellen teamet og fag velger i ticket 15. Perioder som ligger utenfor periodene med oppfylte inngangsvilkår, er markert. Kriteriene som avhenger av modellen, skrives inn når ticket 15 er avgjort. Se ADR-0006 under «Steg 2 er nødvendige ekstrautgifter og trygdetid».

**Blocked by:** 05, 06, 15

**Status:** needs-info

Spec: [spec.md](../spec.md). Forslagene til modell: [forslag-ekstrautgifter.html](../forslag-ekstrautgifter.html).

## Gjelder uansett modell

- [ ] En ny Flyway-migrering oppretter tabellene modellen trenger. Tabellene har `ON DELETE CASCADE` fra behandling, indeks på behandling og en CHECK-constraint på datorekkefølgen.
- [ ] Servicen bygger på den felles servicen for perioder med begrunnelse fra ticket 02, med overlapp, redigerbarhet, ansvarlig saksbehandler og endringshistorikk. Hva endringshistorikken viser, følger svaret på spørsmål 11 i ticket 15.
- [ ] Endepunktene krever rollen saksbehandler.
- [ ] I en revurdering av en migrert sak kan saksbehandler endre og slette det som kom fra Infotrygd (ticket 15, punkt 5 under «Avklart»).
- [ ] Kopieringen til ny behandling følger svaret på spørsmål 12 i ticket 15.
- [ ] Steg 2 har en liste bygget som komponentene i steg 1.
- [ ] Perioder utenfor periodene med oppfylte inngangsvilkår er markert i listen. Ticket 08 trenger den samme markeringen. Ticketen som bygges først, lager den slik at begge kan bruke den.
- [ ] Testene dekker servicen, repositoryet med CHECK-constraints og kopieringen.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

## Avhenger av modellen

Fylles ut når ticket 15 er avgjort: hva saksbehandler registrerer, hvilke tabeller og felter som trengs, hvordan satsen blir til, og hva steg 2 sender videre til utledningen i ticket 09.

## Comments

**2026-11-06:** Satt på vent til modellen er avklart i ticket 15.

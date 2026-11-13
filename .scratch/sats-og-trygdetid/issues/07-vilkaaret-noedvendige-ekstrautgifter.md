# 07: Vilkåret nødvendige ekstrautgifter

**What to build:** Saksbehandler kan registrere nødvendige ekstrautgifter i steg 2 etter modellen teamet og fag velger i ticket 15. Perioder som ligger utenfor periodene med oppfylte inngangsvilkår, er markert. Kriteriene som avhenger av modellen, skrives inn når ticket 15 er avgjort. Se ADR-0006 under «Steg 2 er nødvendige ekstrautgifter og trygdetid».

**Blocked by:** 05, 06, 15

**Status:** ready-for-human

Spec: [spec.md](../spec.md). Forslagene til modell: [forslag-ekstrautgifter.html](../forslag-ekstrautgifter.html). Modellen: [ADR-0016](../../../docs/adr/ADR-0016-ekstrautgifter-som-sats-med-kobling.md).

## Gjelder uansett modell

- [x] En ny Flyway-migrering oppretter tabellene modellen trenger. Tabellene har `ON DELETE CASCADE` fra behandling, indeks på behandling og en CHECK-constraint på datorekkefølgen.
- [x] Servicen bygger på den felles servicen for perioder med begrunnelse fra ticket 02, med overlapp, redigerbarhet, ansvarlig saksbehandler og endringshistorikk. Hva endringshistorikken viser, følger svaret på spørsmål 11 i ticket 15.
- [x] Endepunktene krever rollen saksbehandler.
- [x] I en revurdering av en migrert sak kan saksbehandler endre og slette det som kom fra Infotrygd (ticket 15, punkt 5 under «Avklart»).
- [x] Kopieringen til ny behandling følger svaret på spørsmål 12 i ticket 15.
- [x] Steg 2 har en liste bygget som komponentene i steg 1.
- [x] Perioder utenfor periodene med oppfylte inngangsvilkår er markert i listen. Ticket 08 trenger den samme markeringen. Ticketen som bygges først, lager den slik at begge kan bruke den.
- [x] Testene dekker reglene og servicen, inkludert kopiering med mockede repository.
- [x] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

## Avhenger av modellen

Modell B, se ADR-0016.

- [x] Saksbehandler registrerer vurdering, sats og en liste med ekstrautgifter per periode. En ekstrautgift har én hjemmelbokstav og én eller flere diagnoser vurdert til ja i steg 1. Høyst én ekstrautgift per bokstav.
- [x] Tabellene er `vilkar_ekstrautgifter` (vurdering, sats, begrunnelse, datoer) og `vilkar_ekstrautgift_diagnose` (én rad per bokstav og diagnose). CHECK-constraints krever fra og med-dato og sats ved `JA`, og ingen sats ellers. Ved `NEI` er fra og med-dato valgfri.
- [x] Saksbehandler velger satsen. Systemet regner ikke ut noe.
- [x] Reglene ved lagring ligger i `EkstrautgifterRegler`: sats og minst én ekstrautgift ved `JA`, minst én diagnose per ekstrautgift, ingen bokstav to ganger, bokstav d og e bare når perioden allerede har dem, og hver diagnose vurdert til `JA` i hele perioden.
- [x] Endringshistorikken viser sats når perioden har en sats, ellers vurderingen, samt perioden. Hjemmelbokstaver, diagnosekoder og begrunnelse tas ikke med.
- [x] Bare perioder med `JA` kopieres.
- [x] Steg 2 sender vurdering og sats per periode til utledningen i ticket 09. Bokstaver og diagnoser påvirker ikke beløpet.
- [x] Flyten er testet i nettleseren.

## Comments

**2026-11-06:** Satt på vent til modellen er avklart i ticket 15.

**2026-11-09:** Bygget etter modell B. På dette tidspunktet gjensto test av flyten i nettleseren. Den lokale databasen måtte nullstilles først, fordi den hadde en annen V52 fra variant A. Se sjekklisten over for oppdatert teststatus.

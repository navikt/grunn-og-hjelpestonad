# 05: Steg 1 regner ut periodene og velger neste steg

**What to build:** Når saksbehandler trykker Neste i steg 1, regner systemet ut periodene med oppfylte inngangsvilkår uten å lagre dem, og avgjør om saken går til steg 2 eller rett til simulering. Steg 2 viser periodene som tidslinje. Se ADR-0006 og ADR-0014 under «Ingen rett i noen måned hopper over steg 2».

**Blocked by:** 01

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [x] Tabellen for perioder med rett droppes i en ny Flyway-migrering, og typene, endepunktet og frontend-koden rundt den fjernes.
- [x] En ren funksjon regner ut periodene med oppfylte inngangsvilkår fra medlemskap og diagnose, med dagens regler for § 6-9 og flere diagnoser. De eksisterende testene for utledningen flyttes til denne funksjonen.
- [x] En ren funksjon avgjør neste steg: perioder gir sats og trygdetid, og ingen rett i noen måned gir simulering. Hvert tilfelle har en test.
- [x] `POST /api/behandling/{behandlingId}/inngangsvilkar/fullfor` erstatter `/vilkar/fullfor`. Endepunktet sjekker redigerbarhet og ansvarlig saksbehandler, validerer R1 for medlemskap og diagnose, lagrer ingenting og returnerer periodene og neste steg.
- [x] Endringshistorikken får en ny endringstype med teksten «Vurdering av inngangsvilkår fullført». Detaljene har antall perioder og datoer, aldri diagnose.
- [x] Et nytt GET-endepunkt returnerer de utregnede periodene, slik at steg 2 kan vise dem uten å fullføre steg 1 på nytt.
- [x] Frontend går til steg 2 når det finnes perioder, og rett til simulering når det ikke er rett i noen måned.
- [x] Steg 2 viser periodene som tidslinje fra det nye endepunktet.
- [x] Testen av steg-servicen sjekker tilgang, rekkefølge og endringshistorikk.
- [x] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

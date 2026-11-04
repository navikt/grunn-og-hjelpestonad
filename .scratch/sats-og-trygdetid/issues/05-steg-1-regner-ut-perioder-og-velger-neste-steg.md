# 05: Steg 1 regner ut periodene og velger neste steg

**What to build:** Når saksbehandler trykker Neste i steg 1, regner systemet ut periodene med oppfylte inngangsvilkår uten å lagre dem, og avgjør om saken går til steg 2, blir avslått eller opphører. Steg 2 viser periodene som tidslinje. Se ADR-0006 og ADR-0014 under «Avslag eller opphør».

**Blocked by:** 01

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Tabellen for perioder med rett droppes i en ny Flyway-migrering, og typene, endepunktet og frontend-koden rundt den fjernes.
- [ ] En ren funksjon regner ut periodene med oppfylte inngangsvilkår fra medlemskap og diagnose, med dagens regler for § 6-9 og flere diagnoser. De eksisterende testene for utledningen flyttes til denne funksjonen.
- [ ] En ren funksjon avgjør utfallet: perioder gir «videre», ingen perioder og ingen tidligere iverksatt behandling gir «avslag», og ingen perioder i en revurdering gir «opphør». Hvert tilfelle har en test.
- [ ] `POST /api/behandling/{behandlingId}/inngangsvilkar/fullfor` erstatter `/vilkar/fullfor`. Endepunktet sjekker redigerbarhet og ansvarlig saksbehandler, validerer R1 for medlemskap og diagnose, lagrer ingenting og returnerer periodene og utfallet.
- [ ] Endringshistorikken får en ny endringstype med teksten «Vurdering av inngangsvilkår fullført». Detaljene har antall perioder og datoer, aldri diagnose.
- [ ] Et nytt GET-endepunkt returnerer de utregnede periodene, slik at steg 2 kan vise dem uten å fullføre steg 1 på nytt.
- [ ] Frontend går til steg 2 ved «videre», til brev ved «avslag», og til steg 2 med teksten «Ingen perioder med oppfylte inngangsvilkår. Saken opphører.» ved «opphør».
- [ ] Steg 2 viser periodene som tidslinje fra det nye endepunktet.
- [ ] Testen av steg-servicen sjekker tilgang, rekkefølge og endringshistorikk.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

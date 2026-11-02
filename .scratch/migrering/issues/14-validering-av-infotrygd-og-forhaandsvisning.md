# 14: Validering av Infotrygd og forhåndsvisning

**What to build:** Saksbehandler kan se om en person kan migreres. Svaret er enten perioden og beløpene ny løsning vil utbetale, eller årsaken til at personen ikke kan migreres. Reglene følger `InfotrygdPeriodeValideringService` i EF.

**Blocked by:** 03, 27

**Status:** ready-for-human

🔴 Rød sone: valideringsreglene og valget av første måned. Utvikleren skriver de rene funksjonene og testene. Endepunktet er grønn sone.

Spec: [spec.md](../spec.md). Reglene står under `## Answer` i [ticket 03](03-avklar-perioder-og-utbetalt-belop-fra-infotrygd.md).

- [ ] Rene funksjoner får de gjeldende periodene fra ticket 27 og inneværende måned, og returnerer perioden som skal migreres eller en årsak fra en lukket enum.
- [ ] Migreringen stoppes for hver årsak i listen i ticket 03, blant annet ved åpen sak i Infotrygd (utenom klage og anke), mer enn én gjeldende periode fremover i tid, fom som ikke er første dag i måneden, tom som ikke er siste dag i måneden, beløp 0 og siste periode før datogrensen 2026-01-01.
- [ ] Like perioder som henger sammen, slås sammen før antallet aktive perioder telles.
- [ ] Første måned er den seneste av inneværende måned og periodens fom. Uten periode fremover i tid migreres siste måned av siste periode med beløp.
- [ ] Migreringen stoppes hvis fagsaken har en behandling som ikke er henlagt eller avslått.
- [ ] `GET /api/migrering/{fagsakPersonId}` sjekker tilgang med `TilgangService.validerTilgangTilFagsakPerson` før Infotrygd leses, og auditlogges som lesing.
- [ ] Svaret har `kanMigreres`, årsaken som tekst til saksbehandler, perioden og beløpene per periode etter avrunding.
- [ ] Ingen fnr i logger, feilmeldinger eller svaret ut over det saksbehandler allerede har tilgang til.
- [ ] Rene tester dekker hver regel, med tilfellene fra `InfotrygdPeriodeValideringServiceTest` i EF.
- [ ] Endepunktet ligger bak feature toggle for migrering.

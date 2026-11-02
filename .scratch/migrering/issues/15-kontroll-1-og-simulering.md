# 15: Kontroll 1 og sjekk av simuleringen

**What to build:** To rene funksjoner som stopper en migrering som ville endret utbetalingen. Kontroll 1 sammenligner utbetalingsbeløp per måned med Infotrygd-beløpet. Sjekken av simuleringen krever 0 kr i feilutbetaling og etterbetaling.

**Blocked by:** 03, 27

**Status:** ready-for-human

🔴 Rød sone: begge funksjonene. De er sperrene som erstatter beslutter i migreringen (ADR-0013).

Spec: [spec.md](../spec.md)

- [ ] Kontroll 1 får utbetalingsbeløp per måned fra beregningen og Infotrygd-beløpet per måned, og returnerer avvikene per måned med begge beløpene.
- [ ] Kontroll 1 sammenligner beløp etter avrundingsfunksjonen i ADR-0011, aldri tilkjent ytelse med desimaler. Avrundingsfunksjonen lages som en ren funksjon hvis den ikke finnes.
- [ ] Kontroll 1 gjelder bare måneder der Infotrygd har utbetalt.
- [ ] Sjekken av simuleringen får summene for feilutbetaling og etterbetaling, og returnerer et avvik hvis en av dem ikke er 0.
- [ ] Ingen av funksjonene har en parameter for overstyring.
- [ ] Rene tester dekker: ingen avvik, avvik i én måned, avvik i flere måneder, øreavrunding som gir likt beløp, feilutbetaling, etterbetaling og begge.

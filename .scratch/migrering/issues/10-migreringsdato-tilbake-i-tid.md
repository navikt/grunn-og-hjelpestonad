# 10: Migreringsdato tilbake i tid

**What to build:** Saksbehandler kan velge en migreringsdato som Infotrygd allerede har utbetalt for, i den første migreringen. Kontroll 1 og 3 gjelder som for andre migreringer.

**Blocked by:** 05, 07, og ekstern blokkering: Økonomi har bekreftet om OS motregner mellom Infotrygd og ny løsning (aksjonspunkt i ADR-0011)

**Status:** wontfix

Spec: [spec.md](../spec.md)

- [ ] Begrensningen fra ticket 02 om at datoen må være neste måned eller senere, er fjernet.
- [ ] Kontroll 1 sammenligner også månedene Infotrygd har utbetalt for fra migreringsdatoen.
- [ ] Simuleringen viser 0 kr for en migrering tilbake i tid med like beløp, verifisert i dev.
- [ ] Hvis Økonomi svarer at OS ikke motregner, settes denne ticketen til `wontfix` med begrunnelse, og ADR-0011 oppdateres.

## Comments

**2026-10-28:** Lukket. Migreringen gjøres nå som i EF, se ADR-0013 og ny spec. Erstattet av ticket 13–25.

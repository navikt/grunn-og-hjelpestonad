# 11: «Endre migreringsdato»-behandling

**What to build:** Saksbehandler kan flytte migreringsdatoen bakover i en migrert sak. Månedene vi tar over, hentes fra Infotrygd. Fra den gamle datoen og fremover kopieres vilkår og perioder uendret fra forrige behandling og låses. Behandlingen kan ikke sendes videre hvis utbetalingen endres fra den gamle datoen og fremover.

**Blocked by:** 05, 06, 07, 08, og ekstern blokkering: Økonomi har bekreftet om OS motregner mellom Infotrygd og ny løsning (aksjonspunkt i ADR-0011)

**Status:** wontfix

Spec: [spec.md](../spec.md)

- [ ] Saksbehandler kan opprette en behandling med årsak `ENDRE_MIGRERINGSDATO` bare i en sak som har gjeldende migreringsdato.
- [ ] En ren funksjon avviser ny migreringsdato som er lik eller senere enn gjeldende, også i samme måned.
- [ ] Vilkår og perioder fra gammel migreringsdato og fremover kopieres fra forrige iverksatte behandling med kilde forrige behandling, og er låst i skjermbildet.
- [ ] Vilkår og perioder for månedene fra ny til gammel migreringsdato hentes fra Infotrygd som i ticket 05 og 06.
- [ ] Kontroll 1 gjelder månedene fra ny migreringsdato til måneden før gammel.
- [ ] Kontroll 2 er en ren funksjon som sammenligner utbetalingsbeløp per måned med forrige iverksatte behandling, fra gammel migreringsdato og fremover. Den kjøres når vedtak og beregning fullføres, og ved sending til beslutter.
- [ ] En ren funksjon utleder `opphørAlleKjederFra`: ny migreringsdato for `ENDRE_MIGRERINGSDATO`, ellers ingen. Den er klar til bruk når oppdrag-delen kaller generatoren.
- [ ] Rene tester dekker bakover-regelen, kontroll 2 med og uten avvik, og utledningen av opphøret for hver årsak.

## Comments

**2026-10-28:** Lukket. Migreringen gjøres nå som i EF, se ADR-0013 og ny spec. Erstattet av ticket 13–25.

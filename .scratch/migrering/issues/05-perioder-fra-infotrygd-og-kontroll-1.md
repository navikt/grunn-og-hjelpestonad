# 05: Perioder fra Infotrygd og kontroll 1

**What to build:** En migreringsbehandling får perioder med sats og trygdetidsbrøk fra Infotrygd, fra migreringsdatoen og fremover, og beregner tilkjent ytelse. Saksbehandler kan ikke fullføre vedtak og beregning hvis utbetalingsbeløpet i en måned avviker fra det Infotrygd utbetalte. Feilen viser måned og begge beløpene.

**Blocked by:** 02, 03

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Periodene hentes fra `/stonader` etter reglene i svaret på ticket 03.
- [ ] Periodene fra Infotrygd er merket med kilde Infotrygd og kan ikke endres i skjermbildet.
- [ ] Kontroll 1 er en ren funksjon som får utbetalingsbeløp per måned fra beregningen og utbetalt beløp per måned fra Infotrygd, og returnerer avvikene per måned.
- [ ] Kontrollen sammenligner utbetalingsbeløp etter avrunding, aldri tilkjent ytelse med desimaler. Avrundingsfunksjonen fra ADR-0011 lages som en ren funksjon hvis den ikke finnes.
- [ ] Kontrollen gjelder bare måneder der Infotrygd har utbetalt. Måneder etter siste utbetaling i Infotrygd sammenlignes ikke.
- [ ] Vedtak og beregning kan ikke fullføres med avvik. Feilen er funksjonell og viser måned og begge beløpene.
- [ ] Rene tester dekker: ingen avvik, avvik i én måned, avvik i flere måneder og øreavrunding som gir likt beløp.
- [ ] Revurderinger og andre behandlinger påvirkes ikke.

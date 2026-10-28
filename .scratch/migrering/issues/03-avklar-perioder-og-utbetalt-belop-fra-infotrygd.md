# 03: Avklar perioder og utbetalt beløp fra Infotrygd

**What to build:** En beslutning om hvordan en migreringsbehandling får perioder og utbetalt beløp per måned fra Infotrygd. Ticket 05 bygger på svaret. Dette krever kunnskap om Infotrygd og fagsiden, og kan ikke avgjøres av en agent alene.

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

Spec: [spec.md](../spec.md). Bakgrunn: ADR-0005 (risiko og aksjonspunkter) og README for `apps/infotrygd`.

- [ ] Det er bestemt hvilke delytelser som gir perioder for grunnstønad, og hvordan overlappende delytelser og flere endringskoder per vedtak tolkes.
- [ ] Det er bestemt hvordan utbetalt beløp per måned regnes ut, som kontroll 1 sammenligner mot.
- [ ] Det er bekreftet om regelen om `oppdrag_id` fra EF gjelder for grunnstønad.
- [ ] Det er bestemt hvilken registrering i `t_gh` som gir sats og trygdetidsbrøk for en periode.
- [ ] Svaret er skrevet under `## Answer` i denne fila, og README for `apps/infotrygd` er oppdatert hvis svaret endrer det som står der.

## Comments

**2026-10-28:** Gjelder fortsatt etter ADR-0013. Ticket 14 og 15 bygger på svaret i stedet for ticket 05. Valideringen i ticket 14 trenger i tillegg «summerte» perioder som i EF: én periode per sammenhengende tidsrom med månedsbeløp, fom og tom. `GjeldendeInfotrygdPerioder.utled` (ADR-0012, arv og gjeldende perioder) gir allerede hvilket vedtak som gjelder når, og er trolig utgangspunktet. Avklar også om grunnstønad har noe som tilsvarer åpen sak i `InfotrygdService.hentSaker` i EF.

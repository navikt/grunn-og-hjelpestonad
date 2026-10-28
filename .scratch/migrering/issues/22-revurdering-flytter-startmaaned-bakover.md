# 22: Revurdering flytter startmåned bakover

**What to build:** En vanlig revurdering i en migrert sak kan gi andeler før startmåned og ta over måneder Infotrygd har utbetalt for, som i EF. Oppdrag opphører alle kjeder fra ny startmåned.

**Blocked by:** 13, 21, oppdrag-delen av ADR-0011, Økonomi (motregning i OS)

**Status:** ready-for-human

🔴 Rød sone: utledningen av `opphørAlleKjederFra`. Den avgjør hvilke måneder OS opphører.

Spec: [spec.md](../spec.md)

- [ ] Sjekk hvordan ef-iverksett bruker `startdato` mot generatoren, og skriv svaret som kommentar her før funksjonen lages.
- [ ] En ren funksjon utleder `opphørAlleKjederFra`: ny startmåned når en revurdering i en migrert sak flytter startmåned bakover, ellers `null`.
- [ ] En sak er migrert når den første behandlingen har årsak `MIGRERING`.
- [ ] I en sak som ikke er migrert, sender generatoren bare den nye andelen, som i ADR-0011.
- [ ] Revurderingen sender `vedtaksmelding` med `datoStartNyGrunnstønad` lik ny startmåned. I dag bruker `SendVedtakTilInfotrygdFeedTask` første ordinære periode i vedtaket. Sjekk at det gir startmåned, eller bytt til startmåned fra tilkjent ytelse.
- [ ] Revurdering bakover i en migrert sak ligger bak en egen feature toggle til Økonomi har svart.
- [ ] Rene tester dekker: sak som ikke er migrert, migrert sak uten flytting, migrert sak med flytting og flytting i samme måned.

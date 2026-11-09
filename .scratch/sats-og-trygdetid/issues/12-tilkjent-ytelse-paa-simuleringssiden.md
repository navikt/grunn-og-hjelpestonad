# 12: Tilkjent ytelse på simuleringssiden

**What to build:** Saksbehandler ser tilkjent ytelse øverst på simuleringssiden før simuleringen. Beløpet er merket som tilkjent ytelse og ikke som utbetalingsbeløp, etter ADR-0011. Siden kan senere bli en egen side.

**Blocked by:** 10

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] Et GET-endepunkt returnerer tilkjent ytelse for behandlingen per andel: fra og med, til og med, sats, satsbeløp, trygdetidsbrøk og beløp med desimaler.
- [ ] Simuleringssiden viser andelene i en tabell øverst, med overskrift og kolonner som sier at beløpet er tilkjent ytelse.
- [ ] Beløp med desimaler vises med to desimaler i norsk format.
- [ ] Mangler tilkjent ytelse, viser siden beskjed om å fullføre steg 2.
- [ ] Tabellen bruker Aksel-komponenter og har riktig overskriftsnivå og tabelloverskrifter.
- [ ] Registeret som sjekker tilgangskontroll på endepunkter, er oppdatert, og API-klienten er generert på nytt.

# 03: Tilkjent ytelse lagres med desimaler

**What to build:** Prefactoring uten endrede beløp. Tilkjent ytelse skal lagres eksakt med desimaler, og oppdrag eier avrundingen etter § 22-11 og ADR-0011. Beløpet på andelene går fra heltall til desimaltall, og alle som leser beløpet for simulering eller oppdrag, avrunder til hele kroner.

**Blocked by:** PR 116 ([ticket 13 i migrering](../../migrering/issues/13-startmaaned-tilbake-paa-tilkjent-ytelse.md))

**Status:** ready-for-agent

Spec: [spec.md](../spec.md)

- [ ] En ny Flyway-migrering etter `V48` endrer beløpskolonnen på andelene til `NUMERIC`. Eksisterende rader beholder verdien.
- [ ] Domenemodellen for andelene bruker `BigDecimal`.
- [ ] Simulering og oppdrag avrunder med `HALF_UP` til hele kroner i én ren funksjon når de leser beløpet.
- [ ] En test viser at 286,50 blir 287 og 286,49 blir 286.
- [ ] En repository-test lagrer og leser et beløp med tre desimaler uten tap.
- [ ] Beregningen gir samme beløp som før, og de eksisterende testene er grønne.

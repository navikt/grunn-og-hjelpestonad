# 03: Tilkjent ytelse lagres med desimaler

**What to build:** Prefactoring uten endrede beløp. Tilkjent ytelse skal lagres eksakt med desimaler, og koden i oppdrags-mappen eier avrundingen etter § 22-11 og ADR-0011. Beløpet på andelene går fra heltall til desimaltall, og alle som leser beløpet for simulering eller oppdrag, avrunder til hele kroner.

**Blocked by:** PR 116 ([ticket 13 i migrering](../../migrering/issues/13-startmaaned-tilbake-paa-tilkjent-ytelse.md))

**Status:** resolved

Spec: [spec.md](../spec.md)

- [x] En ny Flyway-migrering etter `V48` endrer beløpskolonnen på andelene til `NUMERIC`. Eksisterende rader beholder verdien.
- [x] Domenemodellen for andelene bruker `BigDecimal`.
- [x] Simulering og oppdrag avrunder med `HALF_UP` til hele kroner i én ren funksjon når de leser beløpet. Funksjonen er `tilUtbetalingsbeløp` i pakken `oppdrag`.
- [x] En test viser at 286,50 blir 287 og 286,49 blir 286.
- [x] En repository-test lagrer og leser et beløp med tre desimaler uten tap.
- [x] Beregningen gir samme beløp som før, og de eksisterende testene er grønne.

## Comments

Rekkefølgen på merge mot andre åpne PR-er:

- PR 126 (`fjern-institusjon`) bruker også `V49`. PR-en som merges sist, må gi migreringen et nytt nummer.
- PR 122 og 123 leser `andel.beløp` som `Int`. Etter rebase må de bruke `tilUtbetalingsbeløp(andel.beløp)`. Filteret `it.beløp != 0` må sjekke det avrundede beløpet, ellers går en andel på 0,40 kr til oppdrag som 0 kr.
- PR 123 sletter `iverksett/utbetaling/SimuleringService.kt`, som denne endringen også endrer. Ved konflikt godtar vi slettingen.

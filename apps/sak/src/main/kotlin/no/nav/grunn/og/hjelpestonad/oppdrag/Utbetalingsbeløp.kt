package no.nav.grunn.og.hjelpestonad.oppdrag

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Oppdrag eier avrundingen til hele kroner. § 22-11 sier nærmeste hele krone, men ikke hva som skjer med nøyaktig 50 øre.
 * Vi runder opp med HALF_UP til fag har bestemt det (ADR-0011).
 */
fun tilUtbetalingsbeløp(tilkjentYtelse: BigDecimal): Int = tilkjentYtelse.setScale(0, RoundingMode.HALF_UP).intValueExact()

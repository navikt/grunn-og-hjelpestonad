package no.nav.grunn.og.hjelpestonad.oppdrag

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")
private val ÅPNER: LocalTime = LocalTime.of(6, 0)
private val STENGER: LocalTime = LocalTime.of(21, 0)

/**
 * Første tidspunkt minst [minimumForsinkelse] etter [nå] som ligger på en hverdag mellom kl. 06 og 21 norsk tid, når OS
 * behandler køen. Samme regel som `utledNesteTriggerTidIHverdagerForTask` i familie-ba-sak, men uten helligdager: på en
 * helligdag poller vi som på en vanlig hverdag.
 */
fun nesteTidspunktIÅpningstid(
    nå: Instant,
    minimumForsinkelse: Duration,
): Instant {
    val tidligst = nå.plus(minimumForsinkelse).atZone(OSLO)
    val dato = tidligst.toLocalDate()
    val tid = tidligst.toLocalTime()

    return when {
        dato.erHelg() -> dato.nesteHverdag().åpner()
        tid.isBefore(ÅPNER) -> dato.åpner()
        !tid.isAfter(STENGER) -> tidligst
        else -> dato.nesteHverdag().åpner()
    }.toInstant()
}

private fun LocalDate.erHelg() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

private fun LocalDate.nesteHverdag(): LocalDate = generateSequence(plusDays(1)) { it.plusDays(1) }.first { !it.erHelg() }

private fun LocalDate.åpner(): ZonedDateTime = atTime(ÅPNER).atZone(OSLO)

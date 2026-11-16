package no.nav.grunn.og.hjelpestonad.oppdrag

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

private val NORSK_TIDSSONE: ZoneId = ZoneId.of("Europe/Oslo")
private val OS_ÅPNER: LocalTime = LocalTime.of(6, 0)
private val OS_STENGER: LocalTime = LocalTime.of(21, 0)

/**
 * Første tidspunkt minst [minimumForsinkelse] etter [nå] som ligger på en hverdag mellom kl. 06 og 21 norsk tid, når OS
 * behandler køen. Samme regel som `utledNesteTriggerTidIHverdagerForTask` i familie-ba-sak, men uten helligdager: på en
 * helligdag poller vi som på en vanlig hverdag.
 */
fun nesteTidspunktIÅpningstid(
    nå: Instant,
    minimumForsinkelse: Duration,
): Instant {
    val tidligsteTidspunkt = nå.plus(minimumForsinkelse).atZone(NORSK_TIDSSONE)
    val tidligsteDato = tidligsteTidspunkt.toLocalDate()
    val tidligsteKlokkeslett = tidligsteTidspunkt.toLocalTime()

    val erHelg = tidligsteDato.erHelg()
    val erFørÅpningstid = tidligsteKlokkeslett < OS_ÅPNER
    val erEtterStengetid = tidligsteKlokkeslett > OS_STENGER

    return when {
        erHelg || erEtterStengetid -> tidligsteDato.nesteHverdag().tidspunktOSÅpner()
        erFørÅpningstid -> tidligsteDato.tidspunktOSÅpner()
        else -> tidligsteTidspunkt
    }.toInstant()
}

private fun LocalDate.erHelg() = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

private fun LocalDate.nesteHverdag(): LocalDate = generateSequence(plusDays(1)) { it.plusDays(1) }.first { !it.erHelg() }

private fun LocalDate.tidspunktOSÅpner(): ZonedDateTime = atTime(OS_ÅPNER).atZone(NORSK_TIDSSONE)

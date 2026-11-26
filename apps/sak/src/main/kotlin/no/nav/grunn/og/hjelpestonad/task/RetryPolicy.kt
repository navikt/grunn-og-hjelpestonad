package no.nav.grunn.og.hjelpestonad.task

import efterlatte.prosessering.RetryPolicy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

fun retryMedVentetider(ventetider: List<Duration>): RetryPolicy =
    RetryPolicy(maxAntallFeil = ventetider.size + 1) { antallFeil ->
        ventetider[antallFeil - 1]
    }

fun lagRetryPolicy(
    startVentetid: Duration = 30.seconds,
    maksVentetid: Duration = 1.hours,
    maksSamletVentetid: Duration = 12.hours,
): RetryPolicy {
    require(startVentetid > Duration.ZERO)
    require(maksVentetid >= startVentetid)
    require(maksSamletVentetid >= Duration.ZERO && maksSamletVentetid.isFinite())

    val ventetider =
        buildList {
            var ventetid = startVentetid
            var samletVentetid = Duration.ZERO
            while (samletVentetid + ventetid <= maksSamletVentetid) {
                add(ventetid)
                samletVentetid += ventetid
                ventetid = (ventetid * 2).coerceAtMost(maksVentetid)
            }
        }
    return retryMedVentetider(ventetider)
}

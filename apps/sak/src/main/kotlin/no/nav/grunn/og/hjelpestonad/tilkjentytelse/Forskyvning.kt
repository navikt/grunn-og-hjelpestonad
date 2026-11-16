package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate
import java.time.YearMonth

/** Hvordan verdien endrer seg fra én periode til den som starter dagen etter. Som `Graderingsforskjell` i ks-sak. */
internal enum class Endring {
    ØKNING,
    REDUKSJON,
}

/**
 * Flytter periodene til hele måneder etter § 22-12, som forskyvningen av vilkår i ks-sak. Hver periode sammenlignes med
 * perioden rett før og rett etter:
 * - Ingen periode rett før: verdien gjelder fra måneden etter (første ledd).
 * - Økning: den nye verdien gjelder fra samme måned (fjerde ledd).
 * - Reduksjon: den nye verdien gjelder fra måneden etter (femte ledd).
 * - Ingen periode rett etter: verdien gjelder ut måneden (sjette ledd).
 *
 * `null` i fra og med eller til og med forblir `null`. Perioder som blir tomme, tas ut. Flere endringer i samme måned
 * kan gi perioder som overlapper i den måneden. Da gjelder den høyeste verdien, fordi en økning skal gjelde fra samme
 * måned og en reduksjon først fra måneden etter.
 */
internal fun <T : Comparable<T>> Tidslinje<T>.forskyvTilHeleMåneder(): Tidslinje<T> {
    val perioder = slåSammenLikePerioder().tilPerioderIkkeNull()
    return perioder
        .mapIndexed { indeks, periode ->
            val forrige = perioder.getOrNull(indeks - 1)?.takeIf { it.slutterDagenFør(periode) }
            val neste = perioder.getOrNull(indeks + 1)?.takeIf { periode.slutterDagenFør(it) }
            Periode(
                verdi = periode.verdi,
                fom = forskyvFom(periode.fom, endring(periodeFør = forrige?.verdi, periodeEtter = periode.verdi)),
                tom = forskyvTom(periode.tom, endring(periodeFør = periode.verdi, periodeEtter = neste?.verdi)),
            )
        }.filterNot { it.erTom() }
        .map { it.tilTidslinje() }
        .kombiner { verdier -> verdier.maxOrNull() }
}

/** `null` når en av verdiene mangler, altså når det ikke finnes en periode rett før eller rett etter. */
private fun <T : Comparable<T>> endring(
    periodeFør: T?,
    periodeEtter: T?,
): Endring? =
    when {
        periodeFør == null || periodeEtter == null -> null
        periodeEtter > periodeFør -> Endring.ØKNING
        else -> Endring.REDUKSJON
    }

private fun forskyvFom(
    fom: LocalDate?,
    endringFraForrige: Endring?,
): LocalDate? =
    fom?.let {
        when (endringFraForrige) {
            Endring.ØKNING -> YearMonth.from(it).atDay(1)
            Endring.REDUKSJON, null -> YearMonth.from(it).plusMonths(1).atDay(1)
        }
    }

/** Neste periode starter dagen etter [tom], så endringen skjer den dagen. */
private fun forskyvTom(
    tom: LocalDate?,
    endringTilNeste: Endring?,
): LocalDate? =
    tom?.let {
        when (endringTilNeste) {
            Endring.ØKNING -> YearMonth.from(it.plusDays(1)).minusMonths(1).atEndOfMonth()
            Endring.REDUKSJON -> YearMonth.from(it.plusDays(1)).atEndOfMonth()
            null -> YearMonth.from(it).atEndOfMonth()
        }
    }

private fun <T> Periode<T>.slutterDagenFør(neste: Periode<T>): Boolean = tom?.plusDays(1) == neste.fom

private fun <T> Periode<T>.erTom(): Boolean {
    val fom = fom ?: return false
    val tom = tom ?: return false
    return fom.isAfter(tom)
}

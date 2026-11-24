package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object SimuleringsperiodeSammenslåer {
    fun slåSammenPerioderFørNesteUtbetaling(
        simuleringsmåneder: List<Simuleringsmåned>,
        fomNesteUtbetaling: LocalDate?,
    ): List<Simuleringsperiode> {
        val månederTilOgMedNesteUtbetaling =
            simuleringsmåneder.filter { fomNesteUtbetaling == null || it.fom <= fomNesteUtbetaling }
        check(månederTilOgMedNesteUtbetaling.map { YearMonth.from(it.fom) }.distinct().size == månederTilOgMedNesteUtbetaling.size) {
            "Flere simuleringsmåneder i samme kalendermåned"
        }

        val månederFørNesteUtbetaling =
            månederTilOgMedNesteUtbetaling.filter { fomNesteUtbetaling == null || it.fom < fomNesteUtbetaling }
        val sammenslåttePerioder = månederFørNesteUtbetaling.tilSamenslåtteSimuleringsperioder()

        val nesteSimuleringsmåned =
            månederTilOgMedNesteUtbetaling.singleOrNull { fomNesteUtbetaling != null && it.fom == fomNesteUtbetaling }
        val nestePeriode =
            nesteSimuleringsmåned?.let { nesteMåned ->
                Simuleringsperiode(
                    fom = nesteMåned.fom,
                    tom = nesteMåned.tom,
                    antallMåneder = 1,
                    erNesteUtbetaling = true,
                    beløpPerMåned = nesteMåned.beløp,
                    sumForPerioden = nesteMåned.beløp,
                )
            }
        return sammenslåttePerioder + listOfNotNull(nestePeriode)
    }

    private fun List<Simuleringsmåned>.tilSamenslåtteSimuleringsperioder(): List<Simuleringsperiode> {
        val månedsperioder =
            map { simuleringsmåned ->
                Periode(verdi = simuleringsmåned.beløp, fom = simuleringsmåned.fom, tom = simuleringsmåned.tom)
            }

        return månedsperioder
            .tilTidslinje()
            .slåSammenLikePerioder { forrigeSimuleringsbeløp, nesteSimuleringsbeløp ->
                forrigeSimuleringsbeløp != null &&
                    nesteSimuleringsbeløp != null &&
                    forrigeSimuleringsbeløp.erLik(nesteSimuleringsbeløp)
            }.tilPerioderIkkeNull()
            .map { periode -> tilSammenslåttPeriode(periode) }
    }

    private fun tilSammenslåttPeriode(periode: Periode<Simuleringsbeløp>): Simuleringsperiode {
        val fom = checkNotNull(periode.fom) { "Perioden fra tidslinjen mangler fom" }
        val tom = checkNotNull(periode.tom) { "Perioden fra tidslinjen mangler tom" }
        val beløpPerMåned = checkNotNull(periode.verdi) { "Perioden fra tidslinjen mangler beløp" }

        val antallMåneder = ChronoUnit.MONTHS.between(YearMonth.from(fom), YearMonth.from(tom)).toInt() + 1
        return Simuleringsperiode(
            fom = fom,
            tom = tom,
            antallMåneder = antallMåneder,
            erNesteUtbetaling = false,
            beløpPerMåned = beløpPerMåned,
            sumForPerioden = beløpPerMåned * antallMåneder,
        )
    }
}

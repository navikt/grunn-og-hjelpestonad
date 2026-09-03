package no.nav.gjenlevende.bs.sak.vedtak

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

data class MaxbeløpBarnetilsynSats(
    val periode: Månedsperiode,
    val maxbeløp: Map<Int, Int>,
)

object BeregningUtils {
    // https://lovdata.no/nav/rundskriv/v7-15-00
    private val eldreBarnetilsynsatser: List<MaxbeløpBarnetilsynSats> =
        listOf(
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2025, 1), YearMonth.of(2025, 12)),
                maxbeløp = mapOf(1 to 4790, 2 to 6248, 3 to 7081),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 12)),
                maxbeløp = mapOf(1 to 4650, 2 to 6066, 3 to 6875),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2023, 7), YearMonth.of(2023, 12)),
                maxbeløp = mapOf(1 to 4480, 2 to 5844, 3 to 6623),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2023, 1), YearMonth.of(2023, 6)),
                maxbeløp = mapOf(1 to 4369, 2 to 5700, 3 to 6460),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 12)),
                maxbeløp = mapOf(1 to 4250, 2 to 5545, 3 to 6284),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 12)),
                maxbeløp = mapOf(1 to 4195, 2 to 5474, 3 to 6203),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 12)),
                maxbeløp = mapOf(1 to 4053, 2 to 5289, 3 to 5993),
            ),
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 12)),
                maxbeløp = mapOf(1 to 3977, 2 to 5190, 3 to 5881),
            ),
            // Det samme beløpet for 2016-2018
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2016, 1), YearMonth.of(2018, 12)),
                maxbeløp = mapOf(1 to 3888, 2 to 5074, 3 to 5749),
            ),
        )
    val satserForBarnetilsyn: List<MaxbeløpBarnetilsynSats> =
        listOf(
            MaxbeløpBarnetilsynSats(
                Månedsperiode(YearMonth.of(2026, 1), YearMonth.of(9999, 12)),
                maxbeløp = mapOf(1 to 4895, 2 to 6385, 3 to 7237),
            ),
        ) +
            eldreBarnetilsynsatser

    fun kalkulerUtbetalingsbeløp(
        periodeutgift: BigDecimal,
    ) = maxOf(BigDecimal.ZERO, (periodeutgift).multiply(0.64.toBigDecimal()))

    fun beregnPeriodeBeløp(
        periodeutgift: BigDecimal,
        antallBarn: Int,
        årMåned: YearMonth,
    ): BigDecimal {
        val utbetalingsbeløp = kalkulerUtbetalingsbeløp(periodeutgift)
        val maxSatsBeløp = satserForBarnetilsyn.hentSatsFor(antallBarn, årMåned).toBigDecimal()
        val minBeløp = minOf(utbetalingsbeløp, maxSatsBeløp)

        return maxOf(BigDecimal.ZERO, minBeløp)
    }

    fun beregnBarnetilsynperiode(barnetilsynBeregninger: List<BarnetilsynBeregning>): List<BeløpsperioderDto> {
        val splittetBarnetilsynpBeregninger = delOppPerÅr(barnetilsynBeregninger)
        val beløpsperioderDtoListe = tilBeløpsPerioderDto(splittetBarnetilsynpBeregninger)

        return sammenslåSammenhengendeBeløpsperioder(beløpsperioderDtoListe)
    }

    private fun delOppPerÅr(barnetilsynBeregninger: List<BarnetilsynBeregning>): List<BarnetilsynBeregning> =
        barnetilsynBeregninger.flatMap { periode ->

            val periodeÅr = (periode.datoFra.year..periode.datoTil.year)
            periodeÅr.map { year ->
                val fra = maxOf(periode.datoFra, YearMonth.of(year, 1))
                val til = minOf(periode.datoTil, YearMonth.of(year, 12))

                periode.copy(datoFra = fra, datoTil = til)
            }
        }

    private fun tilBeløpsPerioderDto(barnetilsynperioder: List<BarnetilsynBeregning>): List<BeløpsperioderDto> {
        val perioder = mutableListOf<BeløpsperioderDto>()

        barnetilsynperioder.forEach { barnetilsynperiode ->
            perioder.add(
                BeløpsperioderDto(
                    datoFra = barnetilsynperiode.datoFra,
                    datoTil = barnetilsynperiode.datoTil,
                    utgifter = barnetilsynperiode.utgifter,
                    antallBarn = barnetilsynperiode.barn.size,
                    beløp = beregnPeriodeBeløp(barnetilsynperiode.utgifter, barnetilsynperiode.barn.size, barnetilsynperiode.datoFra).roundUp().toInt(),
                    periodetype = barnetilsynperiode.periodetype,
                ),
            )
        }
        return perioder
    }

    private fun sammenslåSammenhengendeBeløpsperioder(beløpsperioder: List<BeløpsperioderDto>): List<BeløpsperioderDto> {
        if (beløpsperioder.isEmpty()) return emptyList()

        val sorterteBeløpsperioder = beløpsperioder.sortedBy { it.datoFra }
        val sammenslåttBeløpsperioder = mutableListOf<BeløpsperioderDto>()

        var gjeldendePeriode = sorterteBeløpsperioder.first()

        for (i in 1 until sorterteBeløpsperioder.size) {
            val nestePeriode = sorterteBeløpsperioder[i]

            if (kanSammenslå(gjeldendePeriode, nestePeriode)) {
                gjeldendePeriode = gjeldendePeriode.copy(datoTil = nestePeriode.datoTil)
            } else {
                sammenslåttBeløpsperioder.add(gjeldendePeriode)
                gjeldendePeriode = nestePeriode
            }
        }

        sammenslåttBeløpsperioder.add(gjeldendePeriode)

        return sammenslåttBeløpsperioder
    }

    private fun kanSammenslå(
        gjeldendePeriode: BeløpsperioderDto,
        nestePeriode: BeløpsperioderDto,
    ): Boolean {
        val erSammenhengende = gjeldendePeriode.datoTil.plusMonths(1) == nestePeriode.datoFra

        val likeVerdier =
            gjeldendePeriode.utgifter == nestePeriode.utgifter &&
                gjeldendePeriode.antallBarn == nestePeriode.antallBarn &&
                gjeldendePeriode.beløp == nestePeriode.beløp &&
                gjeldendePeriode.periodetype == nestePeriode.periodetype

        return erSammenhengende && likeVerdier
    }
}

fun List<MaxbeløpBarnetilsynSats>.hentSatsFor(
    antallBarn: Int,
    årMåned: YearMonth,
): Int {
    if (antallBarn == 0) {
        return 0
    }
    val maxbeløpBarnetilsynSats =
        this.singleOrNull {
            it.periode.inneholder(årMåned)
        } ?: error("Kunne ikke finne barnetilsyn sats for dato: $årMåned ")

    return maxbeløpBarnetilsynSats.maxbeløp[minOf(antallBarn, 3)]
        ?: error { "Kunne ikke finne barnetilsyn sats for antallBarn: $antallBarn periode: $årMåned " }
}

fun BigDecimal.roundUp(): BigDecimal = this.setScale(0, RoundingMode.UP)

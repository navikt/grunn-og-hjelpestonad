package no.nav.grunn.og.hjelpestonad.oppdrag.simulering

import no.nav.grunn.og.hjelpestonad.oppdrag.FagOmrådeKode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test

/** Ren test uten Spring, jf. ADR-0008. */
class SimuleringsperiodeSammenslåerTest {
    @Test
    fun `påfølgende måneder med like beløp blir én periode med summen for hele perioden`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752), måned("2026-02", nyttBeløp = 752), måned("2026-03", nyttBeløp = 752)),
                fomNesteUtbetaling = null,
            )

        val simuleringsperiode = simuleringsperioder.single()
        assertThat(simuleringsperiode.fom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(simuleringsperiode.tom).isEqualTo(LocalDate.of(2026, 3, 31))
        assertThat(simuleringsperiode.antallMåneder).isEqualTo(3)
        assertThat(simuleringsperiode.beløpPerMåned.nyttBeløp).isEqualByComparingTo("752")
        assertThat(simuleringsperiode.sumForPerioden.nyttBeløp).isEqualByComparingTo("2256")
        assertThat(simuleringsperiode.sumForPerioden.resultat).isEqualByComparingTo("2256")
    }

    @Test
    fun `et endret beløp starter en ny periode`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752), måned("2026-02", nyttBeløp = 752), måned("2026-03", nyttBeløp = 1_146)),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(2, 1)
    }

    @Test
    fun `et endret beløp i et annet felt enn nytt beløp starter også en ny periode`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752, etterbetaling = 752), måned("2026-02", nyttBeløp = 752, etterbetaling = 0)),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder).hasSize(2)
    }

    @Test
    fun `like beløp på hver side av et hull blir to perioder, og hullet står ikke i resultatet`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752), måned("2026-03", nyttBeløp = 752)),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))
        assertThat(simuleringsperioder.map { it.tom }).containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31))
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(1, 1)
    }

    @Test
    fun `et hull over flere måneder gir ingen periode for hullet`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752), måned("2026-05", nyttBeløp = 752)),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1))
        assertThat(simuleringsperioder.map { it.tom }).containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 5, 31))
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(1, 1)
    }

    @Test
    fun `tilstøtende like måneder slås sammen på hver side av et hull`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(
                    måned("2026-01", nyttBeløp = 752),
                    måned("2026-02", nyttBeløp = 752),
                    måned("2026-04", nyttBeløp = 752),
                    måned("2026-05", nyttBeløp = 752),
                ),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1))
        assertThat(simuleringsperioder.map { it.tom }).containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 5, 31))
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(2, 2)
        assertThat(simuleringsperioder.map { it.sumForPerioden.nyttBeløp.toInt() }).containsExactly(1_504, 1_504)
    }

    @Test
    fun `måneder etter neste utbetaling blir ikke med, heller ikke etter et hull`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(
                    måned("2026-01", nyttBeløp = 752),
                    måned("2026-03", nyttBeløp = 752),
                    måned("2026-05", nyttBeløp = 752),
                    måned("2026-06", nyttBeløp = 752),
                ),
                fomNesteUtbetaling = LocalDate.of(2026, 3, 1),
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1))
        assertThat(simuleringsperioder.map { it.tom }).containsExactly(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31))
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(1, 1)
        assertThat(simuleringsperioder.map { it.erNesteUtbetaling }).containsExactly(false, true)
    }

    @Test
    fun `en måned med reell nullsum blir egen periode og forsvinner ikke`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(
                    måned("2026-01", nyttBeløp = 752),
                    måned("2026-02", nyttBeløp = 0),
                    måned("2026-03", nyttBeløp = 752),
                ),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1))
        val nullsum = simuleringsperioder[1]
        assertThat(nullsum.tom).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(nullsum.antallMåneder).isEqualTo(1)
        assertThat(nullsum.sumForPerioden.erLik(Simuleringsbeløp.NULL)).isTrue()
    }

    @Test
    fun `neste utbetaling står alene selv om beløpet er likt månedene før, og senere måneder blir ikke med`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(
                    måned("2026-10", nyttBeløp = 752),
                    måned("2026-11", nyttBeløp = 752),
                    måned("2026-12", nyttBeløp = 752),
                    måned("2027-01", nyttBeløp = 752),
                    måned("2027-02", nyttBeløp = 752),
                ),
                fomNesteUtbetaling = LocalDate.of(2026, 12, 1),
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 1))
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(2, 1)
        assertThat(simuleringsperioder.map { it.erNesteUtbetaling }).containsExactly(false, true)
    }

    @Test
    fun `neste utbetaling som første måned står alene, og senere måneder med likt beløp blir ikke med`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752), måned("2026-02", nyttBeløp = 752), måned("2026-03", nyttBeløp = 752)),
                fomNesteUtbetaling = LocalDate.of(2026, 1, 1),
            )

        val simuleringsperiode = simuleringsperioder.single()
        assertThat(simuleringsperiode.fom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(simuleringsperiode.tom).isEqualTo(LocalDate.of(2026, 1, 31))
        assertThat(simuleringsperiode.antallMåneder).isEqualTo(1)
        assertThat(simuleringsperiode.erNesteUtbetaling).isTrue()
    }

    @Test
    fun `delvis måned beholder sin opprinnelige dato`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(
                    måned("2026-01", nyttBeløp = 752).copy(fom = LocalDate.of(2026, 1, 15)),
                    måned("2026-02", nyttBeløp = 752),
                    måned("2026-03", nyttBeløp = 752).copy(fom = LocalDate.of(2026, 3, 10)),
                ),
                fomNesteUtbetaling = LocalDate.of(2026, 3, 10),
            )

        assertThat(simuleringsperioder.map { it.fom }).containsExactly(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 10))
        assertThat(simuleringsperioder.map { it.tom }).containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31))
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(2, 1)
        assertThat(simuleringsperioder.map { it.erNesteUtbetaling }).containsExactly(false, true)
    }

    @Test
    fun `beløp med ulik skala er like`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-01", nyttBeløp = 752), måned("2026-02", nyttBeløp = 752).let { it.copy(beløp = it.beløp.copy(nyttBeløp = BigDecimal("752.00"))) }),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder).hasSize(1)
    }

    @Test
    fun `summen av periodene er lik summen av månedene, og ingen måned forsvinner`() {
        val simuleringsmåneder =
            listOf(
                måned("2025-11", nyttBeløp = 500),
                måned("2026-01", nyttBeløp = 752, tidligereUtbetalt = 752),
                måned("2026-02", nyttBeløp = 752, tidligereUtbetalt = 752),
                måned("2026-03", nyttBeløp = 1_146, tidligereUtbetalt = 752, feilutbetaling = 0, etterbetaling = 394),
                måned("2026-04", nyttBeløp = 1_146, tidligereUtbetalt = 752, feilutbetaling = 0, etterbetaling = 394),
                måned("2026-05", nyttBeløp = 0, tidligereUtbetalt = 752, feilutbetaling = 752),
                måned("2026-06", nyttBeløp = 1_146),
            )

        val simuleringsperioder = SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(simuleringsmåneder, fomNesteUtbetaling = LocalDate.of(2026, 6, 1))

        val sumAvPeriodene = simuleringsperioder.fold(Simuleringsbeløp.NULL) { sum, periode -> sum + periode.sumForPerioden }
        val sumAvMånedene = simuleringsmåneder.fold(Simuleringsbeløp.NULL) { sum, måned -> sum + måned.beløp }
        assertThat(sumAvPeriodene.erLik(sumAvMånedene)).isTrue()
        assertThat(simuleringsperioder.sumOf { it.antallMåneder }).isEqualTo(simuleringsmåneder.size)
        assertThat(simuleringsperioder.map { it.fom }).containsExactly(
            LocalDate.of(2025, 11, 1),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 6, 1),
        )
        assertThat(simuleringsperioder.map { it.tom }).containsExactly(
            LocalDate.of(2025, 11, 30),
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 5, 31),
            LocalDate.of(2026, 6, 30),
        )
        assertThat(simuleringsperioder.map { it.antallMåneder }).containsExactly(1, 2, 2, 1, 1)
        assertThat(simuleringsperioder.last().erNesteUtbetaling).isTrue()
    }

    @Test
    fun `månedene sorteres før de slås sammen`() {
        val simuleringsperioder =
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(
                listOf(måned("2026-02", nyttBeløp = 752), måned("2026-01", nyttBeløp = 752)),
                fomNesteUtbetaling = null,
            )

        assertThat(simuleringsperioder.single().fom).isEqualTo(LocalDate.of(2026, 1, 1))
    }

    @Test
    fun `to ulike fom i samme kalendermåned, der den ene er før neste utbetaling, kaster IllegalStateException`() {
        val simuleringsmåneder =
            listOf(
                måned("2026-03", nyttBeløp = 752).copy(fom = LocalDate.of(2026, 3, 1)),
                måned("2026-03", nyttBeløp = 752).copy(fom = LocalDate.of(2026, 3, 10)),
            )

        assertThatThrownBy {
            SimuleringsperiodeSammenslåer.slåSammenPerioderFørNesteUtbetaling(simuleringsmåneder, fomNesteUtbetaling = LocalDate.of(2026, 3, 10))
        }.isInstanceOf(IllegalStateException::class.java)
    }

    private fun måned(
        måned: String,
        nyttBeløp: Int,
        tidligereUtbetalt: Int = 0,
        feilutbetaling: Int = 0,
        etterbetaling: Int = nyttBeløp - tidligereUtbetalt,
    ): Simuleringsmåned {
        val yearMonth = YearMonth.parse(måned)
        val resultat = if (feilutbetaling > 0) -feilutbetaling else nyttBeløp - tidligereUtbetalt
        return Simuleringsmåned(
            fom = yearMonth.atDay(1),
            tom = yearMonth.atEndOfMonth(),
            forfallsdato = yearMonth.atDay(20),
            beløp =
                Simuleringsbeløp(
                    nyttBeløp = BigDecimal.valueOf(nyttBeløp.toLong()),
                    tidligereUtbetalt = BigDecimal.valueOf(tidligereUtbetalt.toLong()),
                    resultat = BigDecimal.valueOf(resultat.toLong()),
                    feilutbetaling = BigDecimal.valueOf(feilutbetaling.toLong()),
                    etterbetaling = BigDecimal.valueOf(etterbetaling.toLong()),
                    manuellPostering = BigDecimal.ZERO,
                ),
            posteringer =
                listOf(
                    Simuleringspostering(
                        fagområdekode = FagOmrådeKode.GRUNNSTØNAD_INFOTRYGD.name,
                        mottakertype = Mottakertype.BRUKER,
                        fom = yearMonth.atDay(1),
                        tom = yearMonth.atEndOfMonth(),
                        forfallsdato = yearMonth.atDay(20),
                        posteringstype = Posteringstype.YTELSE,
                        betalingstype = Betalingstype.DEBIT,
                        klassekode = "GHGSORMG",
                        beløp = BigDecimal.valueOf(nyttBeløp.toLong()),
                        erFeilkonto = false,
                        utenInntrekk = false,
                    ),
                ),
        )
    }
}

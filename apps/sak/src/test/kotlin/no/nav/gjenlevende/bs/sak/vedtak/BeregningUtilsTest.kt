package no.nav.gjenlevende.bs.sak.vedtak

import no.nav.gjenlevende.bs.sak.vedtak.BeregningUtils.satserForBarnetilsyn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

class BeregningUtilsTest {
    @Test
    fun `Sjekk at man bruker beregnet dersom mindre enn maks sats, at makssats blir brukt dersom større og null dersom beregnet er mindre enn 0`() {
        val utgift = BigDecimal(1000)
        val barnetilsynPeriodeBelop = BeregningUtils.beregnPeriodeBeløp(utgift, 2, YearMonth.of(2024, 2))

        val utgift2 = BigDecimal(9500)
        val barnetilsynPeriodeBelop2 = BeregningUtils.beregnPeriodeBeløp(utgift2, 2, YearMonth.of(2024, 2))

        val utgift3 = BigDecimal(-200)
        val barnetilsynPeriodeBelop3 = BeregningUtils.beregnPeriodeBeløp(utgift3, 2, YearMonth.of(2024, 2))

        assertThat(barnetilsynPeriodeBelop).isEqualTo(BigDecimal("640.00"))

        val maxBeløp = BigDecimal(6066)
        assertThat(barnetilsynPeriodeBelop2).isEqualTo(maxBeløp)

        assertThat(barnetilsynPeriodeBelop3).isEqualTo(BigDecimal("0"))
    }

    @Test
    fun `Sjekk at det er lagt inn ny makssats for dette året`() {
        val åretsMakssats = satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = YearMonth.now())
        val fjoråretsMakssats = satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = YearMonth.now().minusYears(1))
        assertThat(åretsMakssats).isNotEqualTo(fjoråretsMakssats)
    }

    @Test
    fun `hente riktig sats for barn for år 2025`() {
        val januar2025 = YearMonth.of(2025, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = januar2025)).isEqualTo(7081)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = januar2025)).isEqualTo(7081)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = januar2025)).isEqualTo(6248)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = januar2025)).isEqualTo(4790)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = januar2025)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2024`() {
        val januar2024 = YearMonth.of(2024, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = januar2024)).isEqualTo(6875)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = januar2024)).isEqualTo(6875)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = januar2024)).isEqualTo(6066)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = januar2024)).isEqualTo(4650)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = januar2024)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2023`() {
        val juli2023 = YearMonth.of(2023, 7)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = juli2023)).isEqualTo(6623)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = juli2023)).isEqualTo(6623)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = juli2023)).isEqualTo(5844)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = juli2023)).isEqualTo(4480)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = juli2023)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2023 første halvdel`() {
        val januar2023 = YearMonth.of(2023, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = januar2023)).isEqualTo(6460)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = januar2023)).isEqualTo(6460)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = januar2023)).isEqualTo(5700)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = januar2023)).isEqualTo(4369)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = januar2023)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2022`() {
        val år2022 = YearMonth.of(2022, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2022)).isEqualTo(6284)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2022)).isEqualTo(6284)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2022)).isEqualTo(5545)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2022)).isEqualTo(4250)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2022)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2021`() {
        val år2021 = YearMonth.of(2021, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2021)).isEqualTo(6203)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2021)).isEqualTo(6203)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2021)).isEqualTo(5474)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2021)).isEqualTo(4195)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2021)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2020`() {
        val år2020 = YearMonth.of(2020, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2020)).isEqualTo(5993)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2020)).isEqualTo(5993)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2020)).isEqualTo(5289)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2020)).isEqualTo(4053)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2020)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2019`() {
        val år2019 = YearMonth.of(2019, 1)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år2019)).isEqualTo(5881)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år2019)).isEqualTo(5881)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år2019)).isEqualTo(5190)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år2019)).isEqualTo(3977)
        assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år2019)).isEqualTo(0)
    }

    @Test
    fun `hente riktig sats for barn for år 2016, 2017, 2018`() {
        listOf(2018, 2017, 2016).forEach {
            val år = YearMonth.of(it, 1)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 4, årMåned = år)).isEqualTo(5749)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 3, årMåned = år)).isEqualTo(5749)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 2, årMåned = år)).isEqualTo(5074)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 1, årMåned = år)).isEqualTo(3888)
            assertThat(satserForBarnetilsyn.hentSatsFor(antallBarn = 0, årMåned = år)).isEqualTo(0)
        }
    }

    @Test
    fun `skal sammenslå beløpsperioder med samme utgifter, barn og beløp som er sammenhengende`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 1),
                    datoTil = YearMonth.of(2025, 3),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 4),
                    datoTil = YearMonth.of(2025, 6),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(1)
        assertThat(resultat[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
        assertThat(resultat[0].datoTil).isEqualTo(YearMonth.of(2025, 6))
        assertThat(resultat[0].utgifter).isEqualTo(BigDecimal(1000))
        assertThat(resultat[0].antallBarn).isEqualTo(1)
    }

    @Test
    fun `skal ikke sammenslå beløpsperioder med forskjellige utgifter`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 1),
                    datoTil = YearMonth.of(2025, 3),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 4),
                    datoTil = YearMonth.of(2025, 6),
                    utgifter = BigDecimal(2000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(2)
        assertThat(resultat[0].utgifter).isEqualTo(BigDecimal(1000))
        assertThat(resultat[1].utgifter).isEqualTo(BigDecimal(2000))
    }

    @Test
    fun `skal ikke sammenslå beløpsperioder med forskjellig antall barn`() {
        val barn1 = listOf(UUID.randomUUID())
        val barn2 = listOf(UUID.randomUUID(), UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 1),
                    datoTil = YearMonth.of(2025, 3),
                    utgifter = BigDecimal(1000),
                    barn = barn1,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 4),
                    datoTil = YearMonth.of(2025, 6),
                    utgifter = BigDecimal(1000),
                    barn = barn2,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(2)
        assertThat(resultat[0].antallBarn).isEqualTo(1)
        assertThat(resultat[1].antallBarn).isEqualTo(2)
    }

    @Test
    fun `skal ikke sammenslå beløpsperioder som ikke er sammenhengende`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 1),
                    datoTil = YearMonth.of(2025, 3),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 5),
                    datoTil = YearMonth.of(2025, 7),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(2)
        assertThat(resultat[0].datoTil).isEqualTo(YearMonth.of(2025, 3))
        assertThat(resultat[1].datoFra).isEqualTo(YearMonth.of(2025, 5))
    }

    @Test
    fun `skal dele opp beløpsperioder ved årsskifte når beløp endres pga maxbeløp`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 11),
                    datoTil = YearMonth.of(2026, 2),
                    utgifter = BigDecimal(10000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(2)
        assertThat(resultat[0].datoFra).isEqualTo(YearMonth.of(2025, 11))
        assertThat(resultat[0].datoTil).isEqualTo(YearMonth.of(2025, 12))
        assertThat(resultat[1].datoFra).isEqualTo(YearMonth.of(2026, 1))
        assertThat(resultat[1].datoTil).isEqualTo(YearMonth.of(2026, 2))

        assertThat(resultat[0].beløp).isNotEqualTo(resultat[1].beløp)
        assertThat(resultat[0].beløp).isEqualTo(4790)
        assertThat(resultat[1].beløp).isEqualTo(4895)
    }

    @Test
    fun `skal ikke dele opp beløpsperioder ved årsskifte når beløp ikke endres`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 11),
                    datoTil = YearMonth.of(2026, 2),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(1)
        assertThat(resultat[0].datoFra).isEqualTo(YearMonth.of(2025, 11))
        assertThat(resultat[0].datoTil).isEqualTo(YearMonth.of(2026, 2))

        assertThat(resultat[0].beløp).isEqualTo(640)
    }

    @Test
    fun `skal håndtere kompleks scenario med både sammenslåing og dele opp`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 1),
                    datoTil = YearMonth.of(2025, 3),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 4),
                    datoTil = YearMonth.of(2025, 6),
                    utgifter = BigDecimal(1000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2025, 11),
                    datoTil = YearMonth.of(2026, 2),
                    utgifter = BigDecimal(10000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(3)

        assertThat(resultat[0].datoFra).isEqualTo(YearMonth.of(2025, 1))
        assertThat(resultat[0].datoTil).isEqualTo(YearMonth.of(2025, 6))

        assertThat(resultat[1].datoFra).isEqualTo(YearMonth.of(2025, 11))
        assertThat(resultat[1].datoTil).isEqualTo(YearMonth.of(2025, 12))

        assertThat(resultat[2].datoFra).isEqualTo(YearMonth.of(2026, 1))
        assertThat(resultat[2].datoTil).isEqualTo(YearMonth.of(2026, 2))
    }

    @Test
    fun `skal dele opp beløpsperioder som går over flere år hvor beløp endres`() {
        val barn = listOf(UUID.randomUUID())
        val beregninger =
            listOf(
                BarnetilsynBeregning(
                    datoFra = YearMonth.of(2024, 10),
                    datoTil = YearMonth.of(2026, 3),
                    utgifter = BigDecimal(10000),
                    barn = barn,
                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                ),
            )

        val resultat = BeregningUtils.beregnBarnetilsynperiode(beregninger)

        assertThat(resultat).hasSize(3)

        assertThat(resultat[0].datoFra).isEqualTo(YearMonth.of(2024, 10))
        assertThat(resultat[0].datoTil).isEqualTo(YearMonth.of(2024, 12))
        assertThat(resultat[0].beløp).isEqualTo(4650)

        assertThat(resultat[1].datoFra).isEqualTo(YearMonth.of(2025, 1))
        assertThat(resultat[1].datoTil).isEqualTo(YearMonth.of(2025, 12))
        assertThat(resultat[1].beløp).isEqualTo(4790)

        assertThat(resultat[2].datoFra).isEqualTo(YearMonth.of(2026, 1))
        assertThat(resultat[2].datoTil).isEqualTo(YearMonth.of(2026, 3))
        assertThat(resultat[2].beløp).isEqualTo(4895)
    }
}

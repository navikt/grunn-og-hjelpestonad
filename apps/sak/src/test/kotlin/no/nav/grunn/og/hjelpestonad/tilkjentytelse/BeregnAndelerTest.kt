package no.nav.grunn.og.hjelpestonad.tilkjentytelse

import no.nav.grunn.og.hjelpestonad.vedtak.AktivitetstypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.GrunnstønadPeriode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregnAndelerTest {
    @Test
    fun `innvilget vedtak gir én andel per ordinær periode med første og siste dag i måneden`() {
        val kildeBehandlingId = UUID.randomUUID()
        val vedtak =
            Vedtak(
                behandlingId = UUID.randomUUID(),
                resultatType = ResultatType.INNVILGET,
                grunnstønadPerioder =
                    listOf(
                        lagOrdinærPeriode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 3),
                            utgifter = BigDecimal(5000),
                        ),
                    ),
                saksbehandlerIdent = "VL",
            )

        val andeler = beregnAndeler(vedtak, kildeBehandlingId)

        assertThat(andeler).hasSize(1)
        val andel = andeler.single()
        assertThat(andel.fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(andel.tom).isEqualTo(LocalDate.of(2025, 3, 31))
        assertThat(andel.beløp).isGreaterThan(0)
        assertThat(andel.kildeBehandlingId).isEqualTo(kildeBehandlingId)
    }

    @Test
    fun `perioder uten stønad gir ikke andeler`() {
        val vedtak =
            Vedtak(
                behandlingId = UUID.randomUUID(),
                resultatType = ResultatType.INNVILGET,
                grunnstønadPerioder =
                    listOf(
                        lagOrdinærPeriode(
                            datoFra = YearMonth.of(2025, 1),
                            datoTil = YearMonth.of(2025, 2),
                            utgifter = BigDecimal(5000),
                        ),
                        GrunnstønadPeriode(
                            datoFra = YearMonth.of(2025, 3),
                            datoTil = YearMonth.of(2025, 4),
                            utgifter = BigDecimal.ZERO,
                            barn = emptyList(),
                            periodetype = PeriodetypeBarnetilsyn.INGEN_STØNAD,
                            aktivitetstype = AktivitetstypeBarnetilsyn.IKKE_RELEVANT,
                        ),
                        lagOrdinærPeriode(
                            datoFra = YearMonth.of(2025, 5),
                            datoTil = YearMonth.of(2025, 6),
                            utgifter = BigDecimal(5000),
                        ),
                    ),
                saksbehandlerIdent = "VL",
            )

        val andeler = beregnAndeler(vedtak, UUID.randomUUID())

        assertThat(andeler.map { it.fom }).containsExactlyInAnyOrder(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 5, 1),
        )
    }

    @Test
    fun `opphør gir ingen andeler`() {
        val vedtak =
            Vedtak(
                behandlingId = UUID.randomUUID(),
                resultatType = ResultatType.OPPHØR,
                grunnstønadPerioder = emptyList(),
                saksbehandlerIdent = "VL",
                opphørFom = YearMonth.of(2025, 1),
            )

        val andeler = beregnAndeler(vedtak, UUID.randomUUID())

        assertThat(andeler).isEmpty()
    }

    private fun lagOrdinærPeriode(
        datoFra: YearMonth,
        datoTil: YearMonth,
        utgifter: BigDecimal,
    ) = GrunnstønadPeriode(
        datoFra = datoFra,
        datoTil = datoTil,
        utgifter = utgifter,
        barn = listOf(UUID.randomUUID()),
        periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
        aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
    )
}

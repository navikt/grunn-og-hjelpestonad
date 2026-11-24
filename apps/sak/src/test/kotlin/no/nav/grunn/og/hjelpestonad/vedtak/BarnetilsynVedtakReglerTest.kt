package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.YearMonth
import java.util.UUID

class BarnetilsynVedtakReglerTest {
    private val januar = YearMonth.of(2026, 1)
    private val ordinærPeriode =
        GrunnstønadPeriode(
            datoFra = januar,
            datoTil = januar,
            utgifter = 1000.toBigDecimal(),
            barn = listOf(UUID.randomUUID()),
            periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
            aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
        )

    @Test
    fun `innvilgelse med ordinær periode og begrunnelse kan lagres`() {
        BarnetilsynVedtakRegler.validerKanLagreVedtak(
            VedtakRequest(resultatType = ResultatType.INNVILGET, grunnstønadPerioder = listOf(ordinærPeriode), begrunnelse = "Begrunnelse"),
        )
    }

    @Test
    fun `ordinær periode uten barn kan ikke lagres`() {
        assertThatThrownBy {
            BarnetilsynVedtakRegler.validerKanLagreVedtak(
                VedtakRequest(resultatType = ResultatType.INNVILGET, grunnstønadPerioder = listOf(ordinærPeriode.copy(barn = emptyList())), begrunnelse = "Begrunnelse"),
            )
        }.isInstanceOf(UgyldigInput::class.java).hasMessage("Må ha med minst et barn på en periode som er ordinær")
    }

    @Test
    fun `opphør krever opphørsdato`() {
        assertThatThrownBy {
            BarnetilsynVedtakRegler.validerKanLagreVedtak(
                VedtakRequest(resultatType = ResultatType.OPPHØR, grunnstønadPerioder = emptyList(), begrunnelse = "Begrunnelse"),
            )
        }.isInstanceOf(UgyldigInput::class.java).hasMessage("Kan ikke opphøre uten å velge opphørsdato")
    }

    @Test
    fun `avslag kan ikke ha barnetilsynsperioder`() {
        assertThatThrownBy {
            BarnetilsynVedtakRegler.validerKanLagreVedtak(
                VedtakRequest(resultatType = ResultatType.AVSLÅTT, grunnstønadPerioder = listOf(ordinærPeriode), begrunnelse = "Begrunnelse"),
            )
        }.isInstanceOf(UgyldigInput::class.java).hasMessage("Kan ikke være barnetilsynsperioder på et opphørsvedtak")
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 40001])
    fun `beregning avviser utgifter utenfor tillatt beløpsintervall`(utgifter: Int) {
        assertThatThrownBy {
            BarnetilsynVedtakRegler.validerKanBeregne(
                BarnetilsynBeregningRequest(
                    listOf(BarnetilsynBeregning(datoFra = januar, datoTil = januar, utgifter = utgifter.toBigDecimal(), barn = ordinærPeriode.barn, periodetype = PeriodetypeBarnetilsyn.ORDINÆR)),
                ),
            )
        }.isInstanceOf(UgyldigInput::class.java)
    }

    @Test
    fun `beregning avviser hull mellom perioder`() {
        val januarBeregning = BarnetilsynBeregning(datoFra = januar, datoTil = januar, utgifter = 1000.toBigDecimal(), barn = ordinærPeriode.barn, periodetype = PeriodetypeBarnetilsyn.ORDINÆR)

        assertThatThrownBy {
            BarnetilsynVedtakRegler.validerKanBeregne(
                BarnetilsynBeregningRequest(listOf(januarBeregning, januarBeregning.copy(datoFra = januar.plusMonths(2), datoTil = januar.plusMonths(2)))),
            )
        }.isInstanceOf(UgyldigInput::class.java).hasMessage("Perioder må være sammenhengende")
    }
}

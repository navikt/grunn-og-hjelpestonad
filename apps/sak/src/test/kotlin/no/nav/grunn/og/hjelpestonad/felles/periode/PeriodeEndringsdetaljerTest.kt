package no.nav.grunn.og.hjelpestonad.felles.periode

import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

class PeriodeEndringsdetaljerTest {
    private data class TestPeriode(
        override val fraOgMedDato: LocalDate?,
        override val tilOgMedDato: LocalDate?,
    ) : Periodisert

    private val fraDato = LocalDate.of(2025, 11, 1)
    private val tilDato = LocalDate.of(2026, 6, 30)

    private fun historikk(
        fraOgMedDato: LocalDate? = fraDato,
        tilOgMedDato: LocalDate? = tilDato,
    ): String =
        historikktekst(
            endringsdetalj = "TESTPERIODE, Vurdering: JA",
            periode = TestPeriode(fraOgMedDato, tilOgMedDato),
        )

    @Test
    fun `begge datoer gir fra og til`() {
        assertThat(historikk()).isEqualTo("TESTPERIODE, Vurdering: JA, Periode: 01.11.2025 – 30.06.2026")
    }

    @Test
    fun `periode uten datoer er hele behandlingsperioden`() {
        assertThat(historikk(fraOgMedDato = null, tilOgMedDato = null))
            .isEqualTo("TESTPERIODE, Vurdering: JA, Periode: Hele behandlingsperioden")
    }

    @Test
    fun `periode uten fra-dato starter ved fødsel`() {
        assertThat(historikk(fraOgMedDato = null))
            .isEqualTo("TESTPERIODE, Vurdering: JA, Periode: Fra fødsel – 30.06.2026")
    }

    @Test
    fun `periode uten til-dato er løpende`() {
        assertThat(historikk(tilOgMedDato = null))
            .isEqualTo("TESTPERIODE, Vurdering: JA, Periode: 01.11.2025 – løpende")
    }
}

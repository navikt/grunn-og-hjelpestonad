package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksresultatTest {
    @Test
    fun `beregnede andeler gir innvilgelse for førstegang og revurdering`() {
        for (erRevurdering in listOf(false, true)) {
            assertThat(utledVedtaksresultat(true, true, true, erRevurdering)).isEqualTo(ResultatType.INNVILGET)
        }
    }

    @Test
    fun `førstegangsbehandling uten oppfylte inngangsvilkår gir avslag uten beregning`() {
        assertThat(utledVedtaksresultat(false, false, false, false)).isEqualTo(ResultatType.AVSLÅTT)
    }

    @Test
    fun `beregning uten andeler gir avslag ved førstegangsbehandling`() {
        assertThat(utledVedtaksresultat(true, true, false, false)).isEqualTo(ResultatType.AVSLÅTT)
    }

    @Test
    fun `oppfylte inngangsvilkår uten beregning skal ikke tolkes som avslag`() {
        assertThrows<UgyldigInput> { utledVedtaksresultat(true, false, false, false) }
    }

    @Test
    fun `revurdering uten andeler krever eksplisitt opphørsvedtak`() {
        assertThrows<UgyldigInput> { utledVedtaksresultat(false, true, false, true) }
        assertThrows<UgyldigInput> { utledVedtaksresultat(true, true, false, true) }
    }

    @Test
    fun `beregnede andeler uten oppfylte inngangsvilkår avvises`() {
        assertThrows<UgyldigInput> { utledVedtaksresultat(false, true, true, false) }
    }
}

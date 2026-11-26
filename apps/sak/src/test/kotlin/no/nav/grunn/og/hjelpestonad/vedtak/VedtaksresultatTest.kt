package no.nav.grunn.og.hjelpestonad.vedtak

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VedtaksresultatTest {
    @Test
    fun `beregnede andeler gir innvilgelse ved førstegangsbehandling`() {
        assertThat(utledVedtaksresultat(harBeregnedeAndeler = true)).isEqualTo(ResultatType.INNVILGET)
    }

    @Test
    fun `beregning uten andeler gir avslag ved førstegangsbehandling`() {
        assertThat(utledVedtaksresultat(harBeregnedeAndeler = false)).isEqualTo(ResultatType.AVSLÅTT)
    }
}

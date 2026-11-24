package no.nav.grunn.og.hjelpestonad.vedtak

import no.nav.grunn.og.hjelpestonad.felles.UgyldigInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VedtaksresultatTest {
    @Test
    fun `beregnede andeler gir innvilgelse for førstegang og revurdering`() {
        for (erRevurdering in listOf(false, true)) {
            assertThat(utledVedtaksresultat(harOppfylteInngangsvilkår = true, harBeregnedeAndeler = true, erRevurdering = erRevurdering)).isEqualTo(ResultatType.INNVILGET)
        }
    }

    @Test
    fun `førstegangsbehandling uten oppfylte inngangsvilkår gir avslag uten beregning`() {
        assertThat(utledVedtaksresultat(harOppfylteInngangsvilkår = false, harBeregnedeAndeler = false, erRevurdering = false)).isEqualTo(ResultatType.AVSLÅTT)
    }

    @Test
    fun `beregning uten andeler gir avslag ved førstegangsbehandling`() {
        assertThat(utledVedtaksresultat(harOppfylteInngangsvilkår = true, harBeregnedeAndeler = false, erRevurdering = false)).isEqualTo(ResultatType.AVSLÅTT)
    }

    @Test
    fun `revurdering uten andeler krever eksplisitt opphørsvedtak`() {
        assertThrows<UgyldigInput> { utledVedtaksresultat(harOppfylteInngangsvilkår = false, harBeregnedeAndeler = false, erRevurdering = true) }
        assertThrows<UgyldigInput> { utledVedtaksresultat(harOppfylteInngangsvilkår = true, harBeregnedeAndeler = false, erRevurdering = true) }
    }

    @Test
    fun `beregnede andeler uten oppfylte inngangsvilkår avvises`() {
        assertThrows<UgyldigInput> { utledVedtaksresultat(harOppfylteInngangsvilkår = false, harBeregnedeAndeler = true, erRevurdering = false) }
    }
}

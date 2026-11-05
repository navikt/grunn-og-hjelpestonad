package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.NesteStegEtterInngangsvilkår
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PeriodeMedOppfylteInngangsvilkår
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test

/**
 * Hvilket steg saken går til etter inngangsvilkårene. Ren test uten Spring og uten mockk.
 */
class NesteStegEtterInngangsvilkårTest {
    private val periode = PeriodeMedOppfylteInngangsvilkår(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))

    @Test
    fun `perioder med oppfylte inngangsvilkår går videre til sats og trygdetid`() {
        assertThat(NesteStegEtterInngangsvilkår.utled(listOf(periode)))
            .isEqualTo(NesteStegEtterInngangsvilkår.SATS_OG_TRYGDETID)
    }

    @Test
    fun `ingen rett i noen måned hopper rett til simulering`() {
        assertThat(NesteStegEtterInngangsvilkår.utled(emptyList()))
            .isEqualTo(NesteStegEtterInngangsvilkår.SIMULERING)
    }
}

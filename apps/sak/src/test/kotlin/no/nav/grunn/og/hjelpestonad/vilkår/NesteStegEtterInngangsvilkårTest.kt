package no.nav.grunn.og.hjelpestonad.vilkår

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.PeriodeMedOppfylteInngangsvilkår
import no.nav.grunn.og.hjelpestonad.steg.vilkårVurdering.utledStegEtterInngangsvilkår
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
        assertThat(utledStegEtterInngangsvilkår(listOf(periode))).isEqualTo(BehandlingSteg.SATS_OG_TRYGDETID)
    }

    @Test
    fun `ingen rett i noen måned hopper over sats og trygdetid til vedtak og brev`() {
        assertThat(utledStegEtterInngangsvilkår(emptyList())).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
    }
}

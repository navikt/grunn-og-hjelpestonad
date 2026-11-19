package no.nav.grunn.og.hjelpestonad.behandling

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

/**
 * Hvilket steg saken går til etter inngangsvilkårene. Ren test uten Spring og uten mockk.
 */
class StegEtterInngangsvilkårTest {
    @Test
    fun `perioder med oppfylte inngangsvilkår går videre til sats og trygdetid`() {
        assertThat(utledStegEtterInngangsvilkår(harPerioderMedOppfylteInngangsvilkår = true)).isEqualTo(BehandlingSteg.SATS_OG_TRYGDETID)
    }

    @Test
    fun `ingen rett i noen måned hopper over sats og trygdetid til vedtak og brev`() {
        assertThat(utledStegEtterInngangsvilkår(harPerioderMedOppfylteInngangsvilkår = false)).isEqualTo(BehandlingSteg.VEDTAK_OG_BREV)
    }
}

package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.Test

class BehandlingTest {
    @Test
    fun `validerHarNådd kaster for steg som kommer etter nåværende steg`() {
        assertThatThrownBy { lagBehandling(nåværendeSteg = BehandlingSteg.INNGANGSVILKÅR).validerHarNådd(BehandlingSteg.SATS_OG_TRYGDETID) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Fullfør «Vilkår» før du går videre.")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerHarNådd kaster ikke for samme steg som nåværende steg`() {
        assertThatCode { lagBehandling(nåværendeSteg = BehandlingSteg.SATS_OG_TRYGDETID).validerHarNådd(BehandlingSteg.SATS_OG_TRYGDETID) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `validerHarNådd kaster ikke for steg før nåværende steg`() {
        assertThatCode { lagBehandling(nåværendeSteg = BehandlingSteg.SATS_OG_TRYGDETID).validerHarNådd(BehandlingSteg.INNGANGSVILKÅR) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `validerErRedigerbar kaster for status som er låst i beslutningen`() {
        for (låstStatus in listOf(BehandlingStatus.FATTER_VEDTAK, BehandlingStatus.IVERKSETTER_VEDTAK, BehandlingStatus.FERDIGSTILT)) {
            assertThatThrownBy { lagBehandling(status = låstStatus).validerErRedigerbar() }
                .isInstanceOf(Feil::class.java)
                .hasMessage("Behandlingen er ikke redigerbar. Status: $låstStatus")
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Test
    fun `validerErRedigerbar kaster ikke for status OPPRETTET eller UTREDES`() {
        for (redigerbarStatus in listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES)) {
            assertThatCode { lagBehandling(status = redigerbarStatus).validerErRedigerbar() }
                .doesNotThrowAnyException()
        }
    }

    @Test
    fun `validerKanEndre kaster når behandlingen ikke er redigerbar`() {
        assertThatThrownBy {
            lagBehandling(status = BehandlingStatus.FATTER_VEDTAK, nåværendeSteg = BehandlingSteg.VEDTAK_OG_BREV)
                .validerKanEndre(BehandlingSteg.ÅRSAK)
        }.isInstanceOf(Feil::class.java)
            .hasMessage("Behandlingen er ikke redigerbar. Status: FATTER_VEDTAK")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerKanEndre kaster når steget ikke er nådd`() {
        assertThatThrownBy { lagBehandling(nåværendeSteg = BehandlingSteg.INNGANGSVILKÅR).validerKanEndre(BehandlingSteg.SATS_OG_TRYGDETID) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Fullfør «Vilkår» før du går videre.")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    private fun lagBehandling(
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        nåværendeSteg: BehandlingSteg = BehandlingSteg.ÅRSAK,
    ) = Behandling(
        fagsakId = UUID.randomUUID(),
        status = status,
        resultat = BehandlingResultat.IKKE_SATT,
        nåværendeSteg = nåværendeSteg,
    )
}

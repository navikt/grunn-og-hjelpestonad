package no.nav.grunn.og.hjelpestonad.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.Test

class BehandlingServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val behandlingService = BehandlingService(behandlingRepository)

    @Test
    fun `validerBehandlingErRedigerbar er redigerbar for status OPPRETTET`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.OPPRETTET)

        behandlingService.validerBehandlingErRedigerbar(behandlingId)
    }

    @Test
    fun `validerBehandlingErRedigerbar er redigerbar for status UTREDES`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.UTREDES)

        behandlingService.validerBehandlingErRedigerbar(behandlingId)
    }

    @Test
    fun `validerBehandlingErRedigerbar kaster feil for status FATTE_VEDTAK`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)

        assertThatThrownBy { behandlingService.validerBehandlingErRedigerbar(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerBehandlingErRedigerbar kaster feil for IVERKSETTER_VEDTAK`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.IVERKSETTER_VEDTAK)

        assertThatThrownBy { behandlingService.validerBehandlingErRedigerbar(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `validerBehandlingErRedigerbar kaster feil for FERDIGSTILT`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FERDIGSTILT)

        assertThatThrownBy { behandlingService.validerBehandlingErRedigerbar(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
    }

    @Test
    fun `hentBehandlingEllerKast gir 404 når behandlingen ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns null

        assertThatThrownBy { behandlingService.hentBehandlingEllerKast(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `settGjeldendeSteg skriver det nye steget til behandlingen`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.UTREDES)
        val oppdatertBehandling = slot<Behandling>()
        every { behandlingRepository.update(capture(oppdatertBehandling)) } answers { firstArg() }

        behandlingService.settGjeldendeSteg(behandlingId, BehandlingSteg.INNGANGSVILKÅR)

        assertThat(oppdatertBehandling.captured.gjeldendeSteg).isEqualTo(BehandlingSteg.INNGANGSVILKÅR)
    }

    @Test
    fun `validerKanEndreSteg kaster feil når steget er senere enn gjeldende steg`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns
            lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.SATS_OG_TRYGDETID)

        assertThatThrownBy { behandlingService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) }
            .isInstanceOf(Feil::class.java)
            .hasMessage("Fullfør «Sats og trygdetid» før du går videre.")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerKanEndreSteg kaster feil når behandlingen ikke er redigerbar`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)

        assertThatThrownBy { behandlingService.validerKanEndreSteg(behandlingId, BehandlingSteg.ÅRSAK) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen er ikke redigerbar")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `validerKanEndreSteg kaster ikke når steget er nådd`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns
            lagBehandling(behandlingId, BehandlingStatus.UTREDES, gjeldendeSteg = BehandlingSteg.VEDTAK_OG_BREV)

        assertThatCode { behandlingService.validerKanEndreSteg(behandlingId, BehandlingSteg.SATS_OG_TRYGDETID) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `validerHarNåddStegHvisRedigerbar kaster ikke for FATTER_VEDTAK selv om steget står på ÅRSAK`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)

        assertThatCode { behandlingService.validerHarNåddStegHvisRedigerbar(behandlingId, BehandlingSteg.VEDTAK_OG_BREV) }
            .doesNotThrowAnyException()
    }

    private fun lagBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
        gjeldendeSteg: BehandlingSteg = BehandlingSteg.ÅRSAK,
    ) = Behandling(
        id = behandlingId,
        fagsakId = UUID.randomUUID(),
        status = status,
        resultat = BehandlingResultat.IKKE_SATT,
        gjeldendeSteg = gjeldendeSteg,
    )
}

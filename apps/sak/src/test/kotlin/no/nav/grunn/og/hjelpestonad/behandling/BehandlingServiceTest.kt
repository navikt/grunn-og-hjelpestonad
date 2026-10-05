package no.nav.grunn.og.hjelpestonad.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
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

    private fun lagBehandling(
        behandlingId: UUID,
        status: BehandlingStatus,
    ) = Behandling(
        id = behandlingId,
        fagsakId = UUID.randomUUID(),
        status = status,
        resultat = BehandlingResultat.IKKE_SATT,
    )
}

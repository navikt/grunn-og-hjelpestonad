package no.nav.grunn.og.hjelpestonad.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.henleggBehandling.HenleggBehandlingService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test

class HenleggBehandlingServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val henleggBehandlingService =
        HenleggBehandlingService(
            behandlingService = BehandlingService(behandlingRepository),
            endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true),
            oppgaveService = mockk<OppgaveService>(relaxed = true),
            taskService = mockk<TaskService>(relaxed = true),
            objectMapper = mockk<ObjectMapper>(relaxed = true),
            ansvarligSaksbehandlerService = ansvarligSaksbehandlerService,
        )

    @Test
    fun `henleggBehandling kaster feil for status FERDIGSTILT`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FERDIGSTILT)

        assertThatThrownBy { henleggBehandlingService.henleggBehandling(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen kan ikke henlegges")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `henleggBehandling kaster feil for status IVERKSETTER_VEDTAK`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.IVERKSETTER_VEDTAK)

        assertThatThrownBy { henleggBehandlingService.henleggBehandling(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen kan ikke henlegges")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `henleggBehandling kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { henleggBehandlingService.henleggBehandling(behandlingId) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
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

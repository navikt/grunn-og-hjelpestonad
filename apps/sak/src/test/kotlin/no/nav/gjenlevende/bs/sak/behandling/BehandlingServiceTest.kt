package no.nav.gjenlevende.bs.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringshistorikkService
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.ManglerTilgang
import no.nav.gjenlevende.bs.sak.oppgave.AnsvarligSaksbehandlerService
import no.nav.gjenlevende.bs.sak.oppgave.OppgaveService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test

class BehandlingServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val lagBehandleSakOppgaveTask = mockk<LagBehandleSakOppgaveTask>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val objectMapper = mockk<ObjectMapper>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val behandlingService =
        BehandlingService(
            behandlingRepository = behandlingRepository,
            lagBehandleSakOppgaveTask = lagBehandleSakOppgaveTask,
            endringshistorikkService = endringshistorikkService,
            oppgaveService = oppgaveService,
            taskService = taskService,
            objectMapper = objectMapper,
            ansvarligSaksbehandlerService = ansvarligSaksbehandlerService,
        )

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
    fun `henleggBehandling kaster feil for status FERDIGSTILT`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.FERDIGSTILT)

        assertThatThrownBy { behandlingService.henleggBehandling(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Behandlingen kan ikke henlegges")
            .extracting("httpStatus")
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `henleggBehandling kaster feil for status IVERKSETTER_VEDTAK`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingRepository.findByIdOrNull(behandlingId) } returns lagBehandling(behandlingId, BehandlingStatus.IVERKSETTER_VEDTAK)

        assertThatThrownBy { behandlingService.henleggBehandling(behandlingId) }
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

        assertThatThrownBy { behandlingService.henleggBehandling(behandlingId) }
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

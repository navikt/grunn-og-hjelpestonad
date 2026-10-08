package no.nav.grunn.og.hjelpestonad.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling.OpprettBehandlingService
import no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl.PdlBehandlingsgrunnlagService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.task.SendStartBehandlingTilInfotrygdFeedTask
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OpprettBehandlingServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val pdlBehandlingsgrunnlagService = mockk<PdlBehandlingsgrunnlagService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val opprettBehandlingService =
        OpprettBehandlingService(
            behandlingService = BehandlingService(behandlingRepository),
            lagBehandleSakOppgaveTask = mockk<LagBehandleSakOppgaveTask>(relaxed = true),
            endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true),
            pdlBehandlingsgrunnlagService = pdlBehandlingsgrunnlagService,
            taskService = taskService,
        )

    @BeforeEach
    fun setUp() {
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "Z123456"
        every { behandlingRepository.insert(any()) } answers { firstArg() }
        every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns null
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(SikkerhetContext)
    }

    @Test
    fun `opprettBehandling oppretter behandling med standardstatus og resultat`() {
        every { behandlingRepository.existsByFagsakIdAndStatusIsNot(any(), any()) } returns false

        val fagsakId = UUID.randomUUID()
        val behandling = opprettBehandlingService.opprettBehandling(fagsakId = fagsakId)

        assertEquals(fagsakId, behandling.fagsakId)
        assertEquals(BehandlingStatus.OPPRETTET, behandling.status)
        assertEquals(BehandlingResultat.IKKE_SATT, behandling.resultat)
    }

    @Test
    fun `opprettBehandling innhenter behandlingsgrunnlag fra PDL`() {
        every { behandlingRepository.existsByFagsakIdAndStatusIsNot(any(), any()) } returns false

        val behandling = opprettBehandlingService.opprettBehandling(fagsakId = UUID.randomUUID())

        verify { pdlBehandlingsgrunnlagService.innhentBehandlingsgrunnlagFraPdl(behandling) }
    }

    @Test
    fun `opprettBehandling sender startbehandling til Infotrygd for første behandling`() {
        // Arrange
        every { behandlingRepository.existsByFagsakIdAndStatusIsNot(any(), any()) } returns false

        // Act
        val behandling = opprettBehandlingService.opprettBehandling(fagsakId = UUID.randomUUID())

        // Assert
        verify {
            taskService.save(
                match {
                    it.type == SendStartBehandlingTilInfotrygdFeedTask.TYPE &&
                        it.payload == behandling.id.toString()
                },
            )
        }
    }

    @Test
    fun `opprettBehandling ikke sender startbehandling for senere behandlinger`() {
        // Arrange
        every { behandlingRepository.existsByFagsakIdAndStatusIsNot(any(), any()) } returns false
        val forrigeBehandling = mockk<Behandling>()
        every { forrigeBehandling.id } returns UUID.randomUUID()
        every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns forrigeBehandling

        // Act
        opprettBehandlingService.opprettBehandling(fagsakId = UUID.randomUUID())

        // Assert
        verify(exactly = 0) {
            taskService.save(match { it.type == SendStartBehandlingTilInfotrygdFeedTask.TYPE })
        }
    }

    @Test
    fun `opprettBehandling kaster feil når fagsaken har en åpen behandling`() {
        every { behandlingRepository.existsByFagsakIdAndStatusIsNot(any(), any()) } returns true

        assertThatThrownBy { opprettBehandlingService.opprettBehandling(fagsakId = UUID.randomUUID()) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Finnes åpen behandling")

        verify(exactly = 0) { behandlingRepository.insert(any()) }
    }
}

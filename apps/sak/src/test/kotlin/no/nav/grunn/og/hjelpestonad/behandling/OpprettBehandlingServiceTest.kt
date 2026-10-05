package no.nav.grunn.og.hjelpestonad.behandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.oppretteBehandling.OpprettBehandlingService
import no.nav.grunn.og.hjelpestonad.behandlingsgrunnlag.pdl.PdlBehandlingsgrunnlagService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OpprettBehandlingServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>(relaxed = true)
    private val pdlBehandlingsgrunnlagService = mockk<PdlBehandlingsgrunnlagService>(relaxed = true)
    private val opprettBehandlingService =
        OpprettBehandlingService(
            behandlingService = BehandlingService(behandlingRepository),
            lagBehandleSakOppgaveTask = mockk<LagBehandleSakOppgaveTask>(relaxed = true),
            endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true),
            pdlBehandlingsgrunnlagService = pdlBehandlingsgrunnlagService,
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

        verify { pdlBehandlingsgrunnlagService.innhentOgLagre(behandling) }
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

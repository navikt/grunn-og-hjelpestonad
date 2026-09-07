package no.nav.grunn.og.hjelpestonad.beslutter

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.internal.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.LagBehandleSakOppgaveTask
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakDto
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.ManglerTilgang
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThatThrownBy
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test

class BeslutterServiceTest {
    private val behandlingService = mockk<BehandlingService>(relaxed = true)
    private val brevService = mockk<BrevService>(relaxed = true)
    private val endringshistorikkService = mockk<EndringshistorikkService>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
    private val taskService = mockk<TaskService>(relaxed = true)
    private val objectMapper = mockk<ObjectMapper>(relaxed = true)
    private val lagBehandleSakOppgaveTask = mockk<LagBehandleSakOppgaveTask>(relaxed = true)
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val vedtakService = mockk<VedtakService>(relaxed = true)

    private val beslutterService =
        BeslutterService(
            behandlingService = behandlingService,
            brevService = brevService,
            endringshistorikkService = endringshistorikkService,
            oppgaveService = oppgaveService,
            totrinnskontrollService = totrinnskontrollService,
            taskService = taskService,
            objectMapper = objectMapper,
            lagBehandleSakOppgaveTask = lagBehandleSakOppgaveTask,
            ansvarligSaksbehandlerService = ansvarligSaksbehandlerService,
            vedtakService = vedtakService,
        )

    @Test
    fun `sendTilBeslutter kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.sendTilBeslutter(behandlingId) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `angreSendTilBeslutter kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.angreSendTilBeslutter(behandlingId) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `besluttVedtak kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        val beslutteVedtakDto = BeslutteVedtakDto(godkjent = true)

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            ManglerTilgang("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.besluttVedtak(behandlingId, beslutteVedtakDto) }
            .isInstanceOf(ManglerTilgang::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }
}

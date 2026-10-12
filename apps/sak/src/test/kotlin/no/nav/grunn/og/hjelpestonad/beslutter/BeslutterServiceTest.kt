package no.nav.grunn.og.hjelpestonad.beslutter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import no.nav.familie.prosessering.internal.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.LagBehandleSakOppgaveTask
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakRequest
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeEYO
import no.nav.grunn.og.hjelpestonad.task.SendVedtakTilInfotrygdFeedTask
import no.nav.grunn.og.hjelpestonad.vedtak.AktivitetstypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.GrunnstønadPeriode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import java.time.YearMonth
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
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.sendTilBeslutter(behandlingId) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `angreSendTilBeslutter kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.angreSendTilBeslutter(behandlingId) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `angreSendTilBeslutter avviser annen aktiv oppgavetype uten endringer`() {
        val behandlingId = UUID.randomUUID()
        every { oppgaveService.hentAktivOppgavetype(behandlingId) } returns OppgavetypeEYO.BEH_UND_VED

        assertThatThrownBy { beslutterService.angreSendTilBeslutter(behandlingId) }
            .isInstanceOf(Feil::class.java)
            .extracting { (it as Feil).httpStatus }
            .isEqualTo(HttpStatus.BAD_REQUEST)

        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(any(), any()) }
        verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(any(), any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any()) }
    }

    @Test
    fun `besluttVedtak kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        val beslutteVedtakRequest = BeslutteVedtakRequest(godkjent = true)

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.besluttVedtak(behandlingId, beslutteVedtakRequest) }
            .isInstanceOf(IkkeAnsvarligSaksbehandlerFeil::class.java)
            .hasMessageContaining("ikke ansvarlig saksbehandler")
    }

    @Test
    fun `besluttVedtak avviser behandling som ikke venter på beslutning uten endringer`() {
        listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES, BehandlingStatus.FERDIGSTILT).forEach { status ->
            val behandlingId = UUID.randomUUID()
            every { behandlingService.hentBehandling(behandlingId) } returns
                Behandling(
                    id = behandlingId,
                    fagsakId = UUID.randomUUID(),
                    status = status,
                    resultat = BehandlingResultat.IKKE_SATT,
                )

            assertThatThrownBy {
                beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))
            }.isInstanceOf(Feil::class.java)
                .extracting { (it as Feil).httpStatus }
                .isEqualTo(HttpStatus.BAD_REQUEST)
            assertThatThrownBy {
                beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))
            }.hasMessageContaining("Behandlingen kan bare besluttes når den venter på beslutning")
                .hasMessageContaining("Status: $status")

            verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(any(), any()) }
            verify(exactly = 0) { behandlingService.oppdaterBehandlingResultat(any(), any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any()) }
        }
    }

    @Test
    fun `besluttVedtak sjekker status etter ansvarlig saksbehandler og før samme saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            Behandling(
                id = behandlingId,
                fagsakId = UUID.randomUUID(),
                status = BehandlingStatus.UTREDES,
                resultat = BehandlingResultat.IKKE_SATT,
            )

        assertThatThrownBy {
            beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))
        }.isInstanceOf(Feil::class.java)

        verifySequence {
            ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
            behandlingService.hentBehandling(behandlingId)
        }
    }

    @Test
    fun `godkjent innvilget vedtak sender vedtak til Infotrygd-feed`() {
        // Arrange
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            Behandling(
                id = behandlingId,
                fagsakId = UUID.randomUUID(),
                status = BehandlingStatus.FATTER_VEDTAK,
                resultat = BehandlingResultat.IKKE_SATT,
            )
        every { vedtakService.hentVedtak(behandlingId) } returns
            Vedtak(
                behandlingId = behandlingId,
                resultatType = ResultatType.INNVILGET,
                grunnstønadPerioder =
                    listOf(
                        GrunnstønadPeriode(
                            datoFra = YearMonth.of(2026, 1),
                            datoTil = YearMonth.of(2026, 12),
                            utgifter = java.math.BigDecimal("1000"),
                            barn = emptyList(),
                            periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                            aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
                        ),
                    ),
                saksbehandlerIdent = "Z123456",
                opprettetAv = "Z123456",
            )

        // Act
        beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))

        // Assert
        verify {
            taskService.save(
                match {
                    it.type == SendVedtakTilInfotrygdFeedTask.TYPE &&
                        it.payload == behandlingId.toString()
                },
            )
        }
    }

    @Test
    fun `besluttVedtak kan underkjenne behandling som venter på beslutning`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns
            Behandling(
                id = behandlingId,
                fagsakId = UUID.randomUUID(),
                status = BehandlingStatus.FATTER_VEDTAK,
                resultat = BehandlingResultat.IKKE_SATT,
            )

        beslutterService.besluttVedtak(
            behandlingId,
            BeslutteVedtakRequest(
                godkjent = false,
                årsakUnderkjent = ÅrsakUnderkjent.VILKÅR,
                begrunnelse = "Vilkår må vurderes på nytt",
            ),
        )

        verify { behandlingService.oppdaterBehandlingStatus(behandlingId, BehandlingStatus.UTREDES) }
    }
}

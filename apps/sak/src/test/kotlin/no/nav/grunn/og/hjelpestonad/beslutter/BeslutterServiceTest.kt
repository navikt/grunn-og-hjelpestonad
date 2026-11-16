package no.nav.grunn.og.hjelpestonad.beslutter

import efterlatte.prosessering.TaskId
import efterlatte.prosessering.TaskType
import efterlatte.prosessering.spring.TaskService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.årsak.Årsak
import no.nav.grunn.og.hjelpestonad.behandling.årsak.ÅrsakBehandling
import no.nav.grunn.og.hjelpestonad.behandling.årsak.ÅrsakBehandlingService
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakRequest
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.Oppgave
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeEYO
import no.nav.grunn.og.hjelpestonad.task.FerdigstillOppgaveTask
import no.nav.grunn.og.hjelpestonad.task.FerdigstillOppgaveTaskData
import no.nav.grunn.og.hjelpestonad.task.OpprettOppgaveTask
import no.nav.grunn.og.hjelpestonad.task.OpprettOppgaveTaskData
import no.nav.grunn.og.hjelpestonad.task.SendVedtakTilInfotrygdFeedTask
import no.nav.grunn.og.hjelpestonad.vedtak.AktivitetstypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.GrunnstønadPeriode
import no.nav.grunn.og.hjelpestonad.vedtak.PeriodetypeBarnetilsyn
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.Vedtak
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.http.HttpStatus
import java.time.LocalDate
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
    private val ansvarligSaksbehandlerService = mockk<AnsvarligSaksbehandlerService>(relaxed = true)
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val årsakBehandlingService = mockk<ÅrsakBehandlingService>(relaxed = true)

    private val beslutterService =
        BeslutterService(
            behandlingService = behandlingService,
            brevService = brevService,
            endringshistorikkService = endringshistorikkService,
            oppgaveService = oppgaveService,
            totrinnskontrollService = totrinnskontrollService,
            taskService = taskService,
            ansvarligSaksbehandlerService = ansvarligSaksbehandlerService,
            vedtakService = vedtakService,
            årsakBehandlingService = årsakBehandlingService,
        )

    @Test
    fun `sendTilBeslutter sender behandling med årsak til beslutter`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(behandlingId, BehandlingStatus.UTREDES)
        every { oppgaveService.hentOppgaveForBehandling(behandlingId) } returns behandleSakOppgave(behandlingId)
        every { årsakBehandlingService.hentÅrsakBehandling(behandlingId) } returns
            ÅrsakBehandling(behandlingId = behandlingId, kravdato = LocalDate.of(2026, 10, 1), årsak = Årsak.SØKNAD)

        beslutterService.sendTilBeslutter(behandlingId)

        verify { behandlingService.oppdaterBehandlingStatus(behandlingId, BehandlingStatus.FATTER_VEDTAK) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.SENDT_TIL_BESLUTTER) }
    }

    @Test
    fun `sendTilBeslutter avvises når behandlingen har status OPPRETTET`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(behandlingId, BehandlingStatus.OPPRETTET)

        assertThatThrownBy { beslutterService.sendTilBeslutter(behandlingId) }
            .hasMessage("Behandling må være i status UTREDES for å kunne sendes til beslutter")

        verifyIngenEndringer()
    }

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
    fun `godkjent innvilget vedtak sender vedtak til Infotrygd-feed`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns
            behandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)
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

        val typer = mutableListOf<TaskType<UUID>>()
        val payloads = mutableListOf<UUID>()
        every { taskService.opprett(capture(typer), capture(payloads), any(), any()) } returns TaskId(1)
        beslutterService.besluttVedtak(behandlingId, BeslutteVedtakRequest(godkjent = true))

        // Assert
        assertThat(
            typer
                .zip(payloads)
                .filter { (type, _) -> type.navn == SendVedtakTilInfotrygdFeedTask.TYPE.navn }
                .map { (_, payload) -> payload },
        ).containsExactly(behandlingId)
    }

    @Test
    fun `underkjent vedtak sendes tilbake til saksbehandler`() {
        val behandlingId = UUID.randomUUID()
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns behandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)
        every { totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId) } returns "Z123456"

        beslutterService.besluttVedtak(
            behandlingId,
            BeslutteVedtakRequest(
                godkjent = false,
                årsakUnderkjent = ÅrsakUnderkjent.VILKÅR,
                begrunnelse = "Vilkår må vurderes på nytt",
            ),
        )

        verify {
            taskService.opprett(
                FerdigstillOppgaveTask.TYPE,
                match<FerdigstillOppgaveTaskData> {
                    it.behandlingId == behandlingId && it.oppgavetype == OppgavetypeEYO.GOD_VED
                },
                any(),
                any(),
            )
        }
        verify { behandlingService.oppdaterBehandlingStatus(behandlingId, BehandlingStatus.UTREDES) }
        verify { endringshistorikkService.registrerUnderkjennelse(behandlingId, ÅrsakUnderkjent.VILKÅR, "Vilkår må vurderes på nytt") }
        verify {
            taskService.opprett(
                OpprettOppgaveTask.TYPE,
                match<OpprettOppgaveTaskData> {
                    it.behandlingId == behandlingId &&
                        it.oppgavetype == OppgavetypeEYO.BEH_UND_VED &&
                        it.tilordnetSaksbehandler == "Z123456"
                },
                any(),
                any(),
            )
        }
    }

    @Test
    fun `besluttVedtak avvises når behandlingen ikke venter på beslutning`() {
        (BehandlingStatus.entries - BehandlingStatus.FATTER_VEDTAK).forEach { status ->
            listOf(true, false).forEach { godkjent ->
                val behandlingId = UUID.randomUUID()
                every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns behandling(behandlingId, status)
                val request =
                    BeslutteVedtakRequest(
                        godkjent = godkjent,
                        årsakUnderkjent = ÅrsakUnderkjent.VILKÅR,
                        begrunnelse = "Vilkår må vurderes på nytt",
                    )

                assertThatThrownBy { beslutterService.besluttVedtak(behandlingId, request) }
                    .isInstanceOfSatisfying(Feil::class.java) {
                        assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
                        assertThat(it.melding).contains("Status: $status")
                    }

                verify(exactly = 0) { totrinnskontrollService.validerAtBeslutterIkkeErSammeSomSaksbehandler(behandlingId) }
                verify(exactly = 0) { behandlingService.oppdaterBehandlingResultat(any(), any()) }
                verify(exactly = 0) { endringshistorikkService.registrerUnderkjennelse(any(), any(), any()) }
                verifyIngenEndringer()
            }
        }
    }

    private fun verifyIngenEndringer() {
        verify(exactly = 0) {
            taskService.opprett(any<TaskType<FerdigstillOppgaveTaskData>>(), any<FerdigstillOppgaveTaskData>(), any(), any())
        }
        verify(exactly = 0) {
            taskService.opprett(any<TaskType<OpprettOppgaveTaskData>>(), any<OpprettOppgaveTaskData>(), any(), any())
        }
        verify(exactly = 0) { behandlingService.oppdaterBehandlingStatus(any(), any()) }
        verify(exactly = 0) { endringshistorikkService.registrerEndring(any(), any()) }
    }

    private fun behandling(
        id: UUID,
        status: BehandlingStatus,
    ) = Behandling(
        id = id,
        fagsakId = UUID.randomUUID(),
        status = status,
        resultat = BehandlingResultat.IKKE_SATT,
    )

    private fun behandleSakOppgave(behandlingId: UUID) =
        Oppgave(
            behandlingId = behandlingId,
            gsakOppgaveId = 1L,
            type = OppgavetypeEYO.BEH_SAK.name,
        )
}

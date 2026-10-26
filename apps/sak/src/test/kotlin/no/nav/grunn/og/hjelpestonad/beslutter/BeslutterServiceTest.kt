package no.nav.grunn.og.hjelpestonad.beslutter

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.familie.prosessering.internal.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.Behandling
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.LagBehandleSakOppgaveTask
import no.nav.grunn.og.hjelpestonad.behandling.årsak.Årsak
import no.nav.grunn.og.hjelpestonad.behandling.årsak.ÅrsakBehandling
import no.nav.grunn.og.hjelpestonad.behandling.årsak.ÅrsakBehandlingService
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakRequest
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.felles.sikkerhet.SikkerhetContext
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.IkkeAnsvarligSaksbehandlerFeil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.Oppgave
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeEYO
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
import tools.jackson.databind.ObjectMapper
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
    private val objectMapper = mockk<ObjectMapper>(relaxed = true)
    private val lagBehandleSakOppgaveTask = mockk<LagBehandleSakOppgaveTask>(relaxed = true)
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
            objectMapper = objectMapper,
            lagBehandleSakOppgaveTask = lagBehandleSakOppgaveTask,
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
    fun `angreSendTilBeslutter kaster ManglerTilgang når bruker ikke er ansvarlig saksbehandler`() {
        val behandlingId = UUID.randomUUID()

        every { ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId) } throws
            IkkeAnsvarligSaksbehandlerFeil("Innlogget saksbehandler er ikke ansvarlig saksbehandler for behandling $behandlingId")

        assertThatThrownBy { beslutterService.angreSendTilBeslutter(behandlingId) }
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
        // Arrange
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
    fun `angreSendTilBeslutter avvises når behandlingen ikke venter på beslutning`() {
        listOf(
            BehandlingStatus.UTREDES to OppgavetypeEYO.BEH_SAK,
            // Etter godkjenning er siste oppgave fortsatt GOD_VED
            BehandlingStatus.FERDIGSTILT to OppgavetypeEYO.GOD_VED,
            // Sendt til beslutter, men GOD_VED-oppgaven er ikke opprettet ennå
            BehandlingStatus.FATTER_VEDTAK to OppgavetypeEYO.BEH_SAK,
        ).forEach { (status, oppgavetype) ->
            val behandlingId = UUID.randomUUID()
            every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns behandling(behandlingId, status)
            every { oppgaveService.hentAktivOppgavetype(behandlingId) } returns oppgavetype

            assertThatThrownBy { beslutterService.angreSendTilBeslutter(behandlingId) }
                .isInstanceOfSatisfying(Feil::class.java) {
                    assertThat(it.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
                    assertThat(it.melding)
                        .isEqualTo("Kan ikke angre send til beslutter fordi behandlingen ikke venter på beslutning.")
                }

            verifyIngenEndringer()
        }
    }

    @Test
    fun `angreSendTilBeslutter setter behandlingen tilbake til UTREDES når den venter på beslutning`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(behandlingId, BehandlingStatus.FATTER_VEDTAK)
        every { behandlingService.hentBehandlingEllerKast(behandlingId) } returns behandling
        every { oppgaveService.hentAktivOppgavetype(behandlingId) } returns OppgavetypeEYO.GOD_VED
        mockkObject(SikkerhetContext)
        every { SikkerhetContext.hentSaksbehandler() } returns "Z123456"

        try {
            beslutterService.angreSendTilBeslutter(behandlingId)
        } finally {
            unmockkObject(SikkerhetContext)
        }

        verify { taskService.save(any()) }
        verify { lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(behandling, "Z123456") }
        verify { behandlingService.oppdaterBehandlingStatus(behandlingId, BehandlingStatus.UTREDES) }
        verify { endringshistorikkService.registrerEndring(behandlingId, EndringType.ANGRET_SEND_TIL_BESLUTTER) }
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
        verify(exactly = 0) { taskService.save(any()) }
        verify(exactly = 0) { lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(any(), any()) }
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

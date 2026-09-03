package no.nav.gjenlevende.bs.sak.beslutter

import no.nav.familie.prosessering.internal.TaskService
import no.nav.gjenlevende.bs.sak.behandling.BehandlingResultat
import no.nav.gjenlevende.bs.sak.behandling.BehandlingService
import no.nav.gjenlevende.bs.sak.behandling.BehandlingStatus
import no.nav.gjenlevende.bs.sak.behandling.LagBehandleSakOppgaveTask
import no.nav.gjenlevende.bs.sak.beslutter.dto.BeslutteVedtakDto
import no.nav.gjenlevende.bs.sak.brev.BrevService
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringType
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringshistorikkService
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import no.nav.gjenlevende.bs.sak.oppgave.AnsvarligSaksbehandlerService
import no.nav.gjenlevende.bs.sak.oppgave.OppgaveService
import no.nav.gjenlevende.bs.sak.oppgave.OppgavetypeEYO
import no.nav.gjenlevende.bs.sak.task.FerdigstillOppgaveTask
import no.nav.gjenlevende.bs.sak.task.OpprettOppgaveTask
import no.nav.gjenlevende.bs.sak.vedtak.VedtakService
import no.nav.gjenlevende.bs.sak.vedtak.tilBehandlingResultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class BeslutterService(
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
    private val endringshistorikkService: EndringshistorikkService,
    private val oppgaveService: OppgaveService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val taskService: TaskService,
    private val objectMapper: ObjectMapper,
    private val lagBehandleSakOppgaveTask: LagBehandleSakOppgaveTask,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val vedtakService: VedtakService,
) {
    @Transactional
    fun sendTilBeslutter(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        validerKanSendeTilBeslutter(behandlingId)

        brevService.oppdaterSaksbehandlerForBrev(behandlingId)
        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.FATTER_VEDTAK,
        )
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.SENDT_TIL_BESLUTTER,
        )

        val aktivOppgavetype = oppgaveService.hentAktivOppgavetype(behandlingId)

        FerdigstillOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = aktivOppgavetype,
            objectMapper = objectMapper,
            taskService = taskService,
        )

        OpprettOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = OppgavetypeEYO.GOD_VED,
            objectMapper = objectMapper,
            taskService = taskService,
        )
    }

    @Transactional
    fun angreSendTilBeslutter(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)

        val aktivOppgavetype = oppgaveService.hentAktivOppgavetype(behandlingId)

        if (aktivOppgavetype != OppgavetypeEYO.GOD_VED) {
            Feil("Kan kun angre send til beslutter hvis oppgaven er av type GOD_VED. Oppgave er ${aktivOppgavetype.name}")
        }

        FerdigstillOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = aktivOppgavetype,
            objectMapper = objectMapper,
            taskService = taskService,
        )

        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw IllegalStateException("Fant ikke behandling med id $behandlingId")

        lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(
            behandling = behandling,
            saksbehandler = SikkerhetContext.hentSaksbehandler(),
        )

        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.UTREDES,
        )

        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.ANGRET_SEND_TIL_BESLUTTER,
        )
    }

    @Transactional
    fun besluttVedtak(
        behandlingId: UUID,
        beslutteVedtakDto: BeslutteVedtakDto,
    ) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        totrinnskontrollService.validerAtBeslutterIkkeErSammeSomSaksbehandler(behandlingId)

        if (beslutteVedtakDto.godkjent) {
            godkjennVedtak(behandlingId)
        } else {
            underkjennVedtak(behandlingId = behandlingId, beslutteVedtakDto = beslutteVedtakDto)
        }
    }

    private fun godkjennVedtak(behandlingId: UUID) {
        val vedtak = vedtakService.hentVedtak(behandlingId)

        val resultat = (vedtak?.resultatType)?.tilBehandlingResultat() ?: BehandlingResultat.IKKE_SATT

        behandlingService.oppdaterBehandlingResultat(
            behandlingId = behandlingId,
            resultat = resultat,
        )

        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.FERDIGSTILT,
        )
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.BESLUTTER_GODKJENT,
        )

        FerdigstillOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = OppgavetypeEYO.GOD_VED,
            objectMapper = objectMapper,
            taskService = taskService,
        )
    }

    private fun validerUnderkjennelse(dto: BeslutteVedtakDto): Pair<ÅrsakUnderkjent, String> {
        val årsak = requireNotNull(dto.årsakUnderkjent) { "Årsak for underkjennelse må være satt" }
        val begrunnelse = requireNotNull(dto.begrunnelse?.takeIf { it.isNotBlank() }) { "Begrunnelse for underkjennelse må være utfylt" }
        return årsak to begrunnelse
    }

    private fun underkjennVedtak(
        behandlingId: UUID,
        beslutteVedtakDto: BeslutteVedtakDto,
    ) {
        val (årsakUnderkjent, begrunnelse) = validerUnderkjennelse(beslutteVedtakDto)

        val saksbehandlerSomSendteTilBeslutter =
            totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId)

        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.UTREDES,
        )

        endringshistorikkService.registrerUnderkjennelse(
            behandlingId = behandlingId,
            årsakUnderkjent = årsakUnderkjent,
            begrunnelse = begrunnelse,
        )

        FerdigstillOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = OppgavetypeEYO.GOD_VED,
            objectMapper = objectMapper,
            taskService = taskService,
        )

        OpprettOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = OppgavetypeEYO.BEH_UND_VED,
            tilordnetSaksbehandler = saksbehandlerSomSendteTilBeslutter,
            objectMapper = objectMapper,
            taskService = taskService,
        )
    }

    fun validerKanSendeTilBeslutter(behandlingId: UUID) {
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: throw IllegalStateException("Fant ikke behandling med id $behandlingId")

        if (behandling.status != BehandlingStatus.UTREDES) {
            throw IllegalStateException("Behandling må være i status UTREDES for å kunne sendes til beslutter")
        }

        val oppgave =
            oppgaveService.hentOppgaveForBehandling(behandlingId)
                ?: throw IllegalStateException("Fant ikke oppgave for behandling med id $behandlingId")

        val finnesFatterVedtakOppgaveFraFør = oppgave.type == OppgavetypeEYO.GOD_VED.name
        if (finnesFatterVedtakOppgaveFraFør) {
            throw IllegalStateException(
                "Det finnes allerede en godkjenn vedtak-oppgave for behandling med id $behandlingId. " +
                    "Behandlingen kan ikke sendes til beslutter på nytt før eksisterende oppgave er ferdigstilt.",
            )
        }
    }
}

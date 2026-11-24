package no.nav.grunn.og.hjelpestonad.beslutter

import efterlatte.prosessering.spring.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.beslutter.dto.BeslutteVedtakRequest
import no.nav.grunn.og.hjelpestonad.brev.BrevService
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppdrag.IverksettOppdragTask
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgavetypeGRU
import no.nav.grunn.og.hjelpestonad.task.FerdigstillOppgaveTask
import no.nav.grunn.og.hjelpestonad.task.FerdigstillOppgaveTaskData
import no.nav.grunn.og.hjelpestonad.task.OpprettOppgaveTask
import no.nav.grunn.og.hjelpestonad.task.OpprettOppgaveTaskData
import no.nav.grunn.og.hjelpestonad.task.SendVedtakTilInfotrygdFeedTask
import no.nav.grunn.og.hjelpestonad.vedtak.ResultatType
import no.nav.grunn.og.hjelpestonad.vedtak.VedtakService
import no.nav.grunn.og.hjelpestonad.vedtak.endrerUtbetaling
import no.nav.grunn.og.hjelpestonad.vedtak.tilBehandlingResultat
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BeslutterService(
    private val behandlingService: BehandlingService,
    private val behandlingStegService: BehandlingStegService,
    private val brevService: BrevService,
    private val endringshistorikkService: EndringshistorikkService,
    private val oppgaveService: OppgaveService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val taskService: TaskService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
    private val vedtakService: VedtakService,
) {
    @Transactional
    fun sendTilBeslutter(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        validerKanSendeTilBeslutter(behandlingId)
        vedtakService.lagreVedtakFraBehandlingsgrunnlag(behandlingId)
        vedtakService.validerVedtaksresultatMotBeregning(behandlingId)

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

        taskService.opprett(
            FerdigstillOppgaveTask.TYPE,
            FerdigstillOppgaveTaskData(behandlingId = behandlingId, oppgavetype = aktivOppgavetype),
        )

        taskService.opprett(
            OpprettOppgaveTask.TYPE,
            OpprettOppgaveTaskData(behandlingId = behandlingId, oppgavetype = OppgavetypeGRU.BEH_VED),
        )
    }

    @Transactional
    fun besluttVedtak(
        behandlingId: UUID,
        beslutteVedtakRequest: BeslutteVedtakRequest,
    ) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val behandling = behandlingService.hentBehandlingEllerKast(behandlingId)
        if (behandling.status != BehandlingStatus.FATTER_VEDTAK) {
            throw Feil(
                "Behandlingen kan bare besluttes når den venter på beslutning. Status: ${behandling.status}",
                HttpStatus.BAD_REQUEST,
            )
        }
        totrinnskontrollService.validerAtBeslutterIkkeErSammeSomSaksbehandler(behandlingId)

        if (beslutteVedtakRequest.godkjent) {
            godkjennVedtak(behandlingId)
        } else {
            underkjennVedtak(behandlingId = behandlingId, beslutteVedtakRequest = beslutteVedtakRequest)
        }
    }

    private fun godkjennVedtak(behandlingId: UUID) {
        val vedtak =
            vedtakService.hentVedtak(behandlingId)
                ?: throw Feil("Behandlingen mangler vedtak. Underkjenn og send den tilbake til saksbehandler.", HttpStatus.BAD_REQUEST)
        vedtakService.validerVedtaksresultatMotBeregning(behandlingId)

        val behandlingResultat = vedtak.resultatType.tilBehandlingResultat()

        behandlingService.oppdaterBehandlingResultat(
            behandlingId = behandlingId,
            resultat = behandlingResultat,
        )

        behandlingService.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = BehandlingStatus.FERDIGSTILT,
        )
        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.BESLUTTER_GODKJENT,
        )

        taskService.opprett(
            FerdigstillOppgaveTask.TYPE,
            FerdigstillOppgaveTaskData(behandlingId = behandlingId, oppgavetype = OppgavetypeGRU.BEH_VED),
        )

        if (vedtak.resultatType == ResultatType.INNVILGET) {
            taskService.opprett(SendVedtakTilInfotrygdFeedTask.TYPE, behandlingId)
        }

        if (vedtak.resultatType.endrerUtbetaling()) {
            taskService.opprett(
                IverksettOppdragTask.TYPE,
                IverksettOppdragTask.IverksettOppdragTaskData(
                    behandlingId = behandlingId,
                    saksbehandlerId = totrinnskontrollService.hentSaksbehandlerSomSendteTilBeslutter(behandlingId),
                ),
            )
        }
    }

    private fun validerUnderkjennelse(dto: BeslutteVedtakRequest): Pair<ÅrsakUnderkjent, String> {
        val årsak = requireNotNull(dto.årsakUnderkjent) { "Årsak for underkjennelse må være satt" }
        val begrunnelse = requireNotNull(dto.begrunnelse?.takeIf { it.isNotBlank() }) { "Begrunnelse for underkjennelse må være utfylt" }
        return årsak to begrunnelse
    }

    private fun underkjennVedtak(
        behandlingId: UUID,
        beslutteVedtakRequest: BeslutteVedtakRequest,
    ) {
        val (årsakUnderkjent, begrunnelse) = validerUnderkjennelse(beslutteVedtakRequest)

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

        taskService.opprett(
            FerdigstillOppgaveTask.TYPE,
            FerdigstillOppgaveTaskData(behandlingId = behandlingId, oppgavetype = OppgavetypeGRU.BEH_VED),
        )

        taskService.opprett(
            OpprettOppgaveTask.TYPE,
            OpprettOppgaveTaskData(
                behandlingId = behandlingId,
                oppgavetype = OppgavetypeGRU.BEH_SAK,
                tilordnetSaksbehandler = saksbehandlerSomSendteTilBeslutter,
            ),
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

        val finnesFatterVedtakOppgaveFraFør = oppgave.type == OppgavetypeGRU.BEH_VED.name
        if (finnesFatterVedtakOppgaveFraFør) {
            throw IllegalStateException(
                "Det finnes allerede en oppgave av typen Behandle vedtak for behandling med id $behandlingId. " +
                    "Behandlingen kan ikke sendes til beslutter på nytt før eksisterende oppgave er ferdigstilt.",
            )
        }
    }
}

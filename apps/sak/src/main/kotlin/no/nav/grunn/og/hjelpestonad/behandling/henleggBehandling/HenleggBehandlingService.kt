package no.nav.grunn.og.hjelpestonad.behandling.henleggBehandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingResultat
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingService
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStatus
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringType
import no.nav.grunn.og.hjelpestonad.endringshistorikk.EndringshistorikkService
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import no.nav.grunn.og.hjelpestonad.oppgave.OppgaveService
import no.nav.grunn.og.hjelpestonad.task.FerdigstillOppgaveTask
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class HenleggBehandlingService(
    private val behandlingService: BehandlingService,
    private val endringshistorikkService: EndringshistorikkService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val objectMapper: ObjectMapper,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    @Transactional
    fun henleggBehandling(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val behandling =
            behandlingService.hentBehandling(behandlingId)
                ?: error("Fant ikke behandling med id=$behandlingId")

        if (behandling.status in listOf(BehandlingStatus.FERDIGSTILT, BehandlingStatus.IVERKSETTER_VEDTAK)) {
            throw Feil(
                melding = "Behandlingen kan ikke henlegges med status: ${behandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        behandlingService.oppdater(
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.HENLAGT,
            ),
        )

        val aktivOppgavetype = oppgaveService.hentAktivOppgavetype(behandlingId)
        FerdigstillOppgaveTask.opprettTask(
            behandlingId = behandlingId,
            oppgavetype = aktivOppgavetype,
            objectMapper = objectMapper,
            taskService = taskService,
        )

        endringshistorikkService.registrerEndring(
            behandlingId = behandlingId,
            endringType = EndringType.BEHANDLING_HENLAGT,
        )
    }
}

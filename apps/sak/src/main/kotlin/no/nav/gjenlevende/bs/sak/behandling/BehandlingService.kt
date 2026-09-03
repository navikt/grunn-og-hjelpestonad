package no.nav.gjenlevende.bs.sak.behandling

import no.nav.familie.prosessering.internal.TaskService
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringType
import no.nav.gjenlevende.bs.sak.endringshistorikk.EndringshistorikkService
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.SikkerhetContext
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import no.nav.gjenlevende.bs.sak.oppgave.AnsvarligSaksbehandlerService
import no.nav.gjenlevende.bs.sak.oppgave.OppgaveService
import no.nav.gjenlevende.bs.sak.task.FerdigstillOppgaveTask
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val lagBehandleSakOppgaveTask: LagBehandleSakOppgaveTask,
    private val endringshistorikkService: EndringshistorikkService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
    private val objectMapper: ObjectMapper,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    @Transactional
    fun opprettBehandling(
        fagsakId: UUID,
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    ): Behandling {
        val forrigeBehandlingId = finnSisteIverksatteBehandling(fagsakId)?.id

        val behandling =
            Behandling(
                fagsakId = fagsakId,
                status = status,
                resultat = BehandlingResultat.IKKE_SATT,
                forrigeBehandlingId = forrigeBehandlingId,
            )

        behandlingRepository.insert(
            behandling,
        )

        lagBehandleSakOppgaveTask.opprettBehandleSakOppgaveTask(behandling = behandling, saksbehandler = SikkerhetContext.hentSaksbehandler())

        endringshistorikkService.registrerEndring(
            behandlingId = behandling.id,
            endringType = EndringType.BEHANDLING_OPPRETTET,
        )

        return behandling
    }

    fun validerBehandlingErRedigerbar(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId) ?: error("Fant ikke behandling med id=$behandlingId")

        // TODO: Litt usikker på om OPPRETTET blir riktig her, ser på dette siden. Gunnstein og jeg blir enig om at vi skal titte på OPPRETTET som konsept.
        if (behandling.status !in listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES)) {
            throw Feil(
                melding = "Behandlingen er ikke redigerbar. Status: ${behandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId) ?: return false

        return behandling.status in listOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES)
    }

    fun hentBehandling(behandlingId: UUID): Behandling? = behandlingRepository.findByIdOrNull(behandlingId)

    fun hentBehandlingerFraFagsak(fagsakId: UUID): List<Behandling> = behandlingRepository.findAllByFagsakId(fagsakId)

    fun finnesÅpenBehandling(fagsakId: UUID) = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsakId, BehandlingStatus.FERDIGSTILT)

    fun finnSisteIverksatteBehandling(fagsakId: UUID) = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun oppdaterBehandlingStatus(
        behandlingId: UUID,
        status: BehandlingStatus,
    ) {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId) ?: error("Fant ikke behandling med id=$behandlingId for oppdatering av BehandlingStatus")
        val oppdatertBehandling =
            behandling.copy(
                status = status,
            )
        behandlingRepository.update(oppdatertBehandling)
    }

    @Transactional
    fun henleggBehandling(behandlingId: UUID) {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        val behandling =
            behandlingRepository.findByIdOrNull(behandlingId)
                ?: error("Fant ikke behandling med id=$behandlingId")

        if (behandling.status in listOf(BehandlingStatus.FERDIGSTILT, BehandlingStatus.IVERKSETTER_VEDTAK)) {
            throw Feil(
                melding = "Behandlingen kan ikke henlegges med status: ${behandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        val henlagtBehandling =
            behandling.copy(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.HENLAGT,
            )
        behandlingRepository.update(henlagtBehandling)

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

    fun oppdaterBehandlingResultat(
        behandlingId: UUID,
        resultat: BehandlingResultat,
    ) {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId) ?: error("Fant ikke behandling med id=$behandlingId for oppdatering av BehandlingResultat")
        val oppdatertBehandling =
            behandling.copy(
                resultat = resultat,
            )
        behandlingRepository.update(oppdatertBehandling)
    }
}

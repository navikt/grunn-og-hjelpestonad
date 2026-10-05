package no.nav.grunn.og.hjelpestonad.behandling

import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BehandlingService(
    private val behandlingRepository: BehandlingRepository,
) {
    fun opprett(behandling: Behandling): Behandling = behandlingRepository.insert(behandling)

    fun oppdater(behandling: Behandling): Behandling = behandlingRepository.update(behandling)

    fun validerBehandlingErRedigerbar(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId) ?: error("Fant ikke behandling med id=$behandlingId")
        if (!behandling.erRedigerbar()) {
            throw Feil(
                melding = "Behandlingen er ikke redigerbar. Status: ${behandling.status}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    fun erBehandlingRedigerbar(behandlingId: UUID): Boolean = behandlingRepository.findByIdOrNull(behandlingId)?.erRedigerbar() ?: false

    fun hentBehandling(behandlingId: UUID): Behandling? = behandlingRepository.findByIdOrNull(behandlingId)

    fun hentBehandlingerFraFagsak(fagsakId: UUID): List<Behandling> = behandlingRepository.findAllByFagsakId(fagsakId)

    fun finnesÅpenBehandling(fagsakId: UUID) = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsakId, BehandlingStatus.FERDIGSTILT)

    fun finnSisteIverksatteBehandling(fagsakId: UUID) = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun finnAlleIverksatteBehandlinger(fagsakId: UUID): List<Behandling> = behandlingRepository.finnAlleIverksatteBehandlinger(fagsakId)

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

private fun Behandling.erRedigerbar(): Boolean = status in setOf(BehandlingStatus.OPPRETTET, BehandlingStatus.UTREDES)

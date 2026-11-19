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
        behandling.validerErRedigerbar()
    }

    fun hentBehandling(behandlingId: UUID): Behandling? = behandlingRepository.findByIdOrNull(behandlingId)

    fun hentBehandlingEllerKast(behandlingId: UUID): Behandling =
        hentBehandling(behandlingId) ?: throw Feil(
            melding = "Fant ikke behandling med id=$behandlingId",
            httpStatus = HttpStatus.NOT_FOUND,
        )

    /**
     * Sjekker at saksbehandler kan endre eller fullføre [steg]. Behandlingen må være redigerbar, og steget
     * må være nådd. Trenger du behandlingen etterpå, hent den og bruk `Behandling.validerKanEndre`.
     */
    fun validerKanEndreSteg(
        behandlingId: UUID,
        steg: BehandlingSteg,
    ) = hentBehandlingEllerKast(behandlingId).validerKanEndre(steg)

    /**
     * For operasjoner som bare leser, som simulering. Sjekker steget bare mens behandlingen er redigerbar.
     * Etter sending til beslutter kan ikke grunnlaget endres, og beslutter skal kunne se alt.
     */
    fun validerHarNåddStegHvisRedigerbar(
        behandlingId: UUID,
        steg: BehandlingSteg,
    ) {
        val behandling = hentBehandlingEllerKast(behandlingId)
        if (behandling.erRedigerbar()) behandling.validerHarNådd(steg)
    }

    fun settGjeldendeSteg(
        behandlingId: UUID,
        gjeldendeSteg: BehandlingSteg,
    ) {
        val behandling = hentBehandlingEllerKast(behandlingId)
        behandlingRepository.update(behandling.copy(gjeldendeSteg = gjeldendeSteg))
    }

    fun hentBehandlingerFraFagsak(fagsakId: UUID): List<Behandling> = behandlingRepository.findAllByFagsakId(fagsakId)

    fun finnesÅpenBehandling(fagsakId: UUID) = behandlingRepository.existsByFagsakIdAndStatusIsNot(fagsakId, BehandlingStatus.FERDIGSTILT)

    fun finnSisteIverksatteBehandling(fagsakId: UUID) = behandlingRepository.finnSisteIverksatteBehandling(fagsakId)

    fun finnAlleIverksatteBehandlinger(fagsakId: UUID): List<Behandling> = behandlingRepository.finnAlleIverksatteBehandlinger(fagsakId)

    fun oppdaterBehandlingStatus(
        behandlingId: UUID,
        status: BehandlingStatus,
    ) {
        val behandling = hentBehandlingEllerKast(behandlingId)
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
        val behandling = hentBehandlingEllerKast(behandlingId)
        val oppdatertBehandling =
            behandling.copy(
                resultat = resultat,
            )
        behandlingRepository.update(oppdatertBehandling)
    }
}

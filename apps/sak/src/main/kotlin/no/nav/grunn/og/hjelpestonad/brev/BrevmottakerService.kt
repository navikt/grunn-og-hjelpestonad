package no.nav.grunn.og.hjelpestonad.brev

import no.nav.grunn.og.hjelpestonad.behandling.BehandlingSteg
import no.nav.grunn.og.hjelpestonad.behandling.BehandlingStegService
import no.nav.grunn.og.hjelpestonad.brev.domain.Brevmottaker
import no.nav.grunn.og.hjelpestonad.oppgave.AnsvarligSaksbehandlerService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val behandlingStegService: BehandlingStegService,
    private val ansvarligSaksbehandlerService: AnsvarligSaksbehandlerService,
) {
    fun hentBrevmottakere(behandlingId: UUID): List<Brevmottaker> = brevmottakerRepository.findAllByBehandlingId(behandlingId)

    @Transactional
    fun oppdaterBrevmottakere(
        behandlingId: UUID,
        brevmottakere: List<Brevmottaker>,
    ): List<Brevmottaker> {
        ansvarligSaksbehandlerService.validerErAnsvarligSaksbehandler(behandlingId)
        behandlingStegService.validerKanEndreSteg(behandlingId, BehandlingSteg.VEDTAK_OG_BREV)
        brevmottakerRepository.deleteAllByBehandlingId(behandlingId)
        return brevmottakerRepository.insertAll(
            brevmottakere.map { it.copy(behandlingId = behandlingId) },
        )
    }
}

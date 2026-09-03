package no.nav.gjenlevende.bs.sak.felles.sikkerhet

import no.nav.gjenlevende.bs.sak.behandling.BehandlingRepository
import no.nav.gjenlevende.bs.sak.fagsak.FagsakPersonService
import no.nav.gjenlevende.bs.sak.fagsak.FagsakRepository
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilgangskontrollService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
) {
    fun hentPersonidentFraBehandling(behandlingId: UUID): String {
        val behandling =
            behandlingRepository.findByIdOrNull(behandlingId)
                ?: throw Feil("Finner ikke behandling med id $behandlingId")

        val fagsak =
            fagsakRepository
                .findById(behandling.fagsakId)
                .orElseThrow { Feil("Finner ikke fagsak med id ${behandling.fagsakId}") }

        return fagsakPersonService.hentAktivIdent(fagsak.fagsakPersonId)
    }

    fun hentPersonidentFraFagsak(fagsakId: UUID): String {
        val fagsak =
            fagsakRepository
                .findById(fagsakId)
                .orElseThrow { Feil("Finner ikke fagsak med id $fagsakId") }

        return fagsakPersonService.hentAktivIdent(fagsak.fagsakPersonId)
    }

    fun hentPersonidentFraFagsakPerson(fagsakPersonId: UUID): String = fagsakPersonService.hentAktivIdent(fagsakPersonId)
}

package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakResponse
import no.nav.grunn.og.hjelpestonad.fagsak.dto.tilResponse
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
open class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
) {
    @Transactional
    fun hentEllerOpprettFagsak(
        personident: String,
        stønadstype: StønadType,
    ): FagsakResponse {
        val fagsakPerson =
            fagsakPersonService.hentEllerOpprettPerson(
                personidenter = setOf(personident),
                gjeldendePersonident = personident,
            )
        val fagsak = hentEllerOpprett(fagsakPerson = fagsakPerson, stønadstype = stønadstype)

        return fagsak.tilResponse(personident)
    }

    fun hentFagsak(
        fagsakPersonId: UUID,
        stønadstype: StønadType,
    ): FagsakResponse {
        val fagsak =
            fagsakRepository.findByFagsakPersonIdAndStønadstype(fagsakPersonId, stønadstype)
                ?: throw Feil(
                    melding = "Fant ikke fagsak med stønadstype $stønadstype for fagsakPersonId=$fagsakPersonId",
                    httpStatus = HttpStatus.NOT_FOUND,
                )

        return fagsak.tilResponse(fagsakPersonService.hentAktivIdent(fagsakPersonId))
    }

    fun hentAktivIdent(fagsakId: UUID): String {
        val fagsak = fagsakRepository.findByIdOrNull(fagsakId) ?: error("Fant ikke fagsak med id=$fagsakId")
        return fagsakPersonService.hentAktivIdent(fagsak.fagsakPersonId)
    }

    private fun hentEllerOpprett(
        fagsakPerson: FagsakPerson,
        stønadstype: StønadType,
    ): Fagsak =
        fagsakRepository.findByFagsakPersonIdAndStønadstype(fagsakPerson.id, stønadstype)
            ?: fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = stønadstype))
}

package no.nav.gjenlevende.bs.sak.fagsak

import no.nav.gjenlevende.bs.sak.fagsak.domain.Fagsak
import no.nav.gjenlevende.bs.sak.fagsak.domain.FagsakPerson
import no.nav.gjenlevende.bs.sak.fagsak.domain.StønadType
import no.nav.gjenlevende.bs.sak.fagsak.dto.FagsakDto
import no.nav.gjenlevende.bs.sak.fagsak.dto.tilDto
import no.nav.gjenlevende.bs.sak.infrastruktur.exception.Feil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
) {
    @Transactional
    fun hentEllerOpprettFagsak(
        request: FagsakRequest,
    ): FagsakDto {
        val (fagsakPerson, personident) = hentFagsakPersonMedIdent(request)
        val fagsak = hentEllerOpprett(fagsakPerson = fagsakPerson, stønadstype = request.stønadstype)

        return fagsak.tilDto(personident)
    }

    private fun hentFagsakPersonMedIdent(request: FagsakRequest): Pair<FagsakPerson, String> =
        when {
            request.personident != null -> {
                val fagsakPerson =
                    fagsakPersonService.hentEllerOpprettPerson(
                        personidenter = setOf(request.personident),
                        gjeldendePersonident = request.personident,
                    )
                Pair(fagsakPerson, request.personident)
            }

            request.fagsakPersonId != null -> {
                val fagsakPerson =
                    fagsakPersonService.finnPersonMedId(request.fagsakPersonId)
                        ?: throw Feil("Fant ingen fagsakPerson med id ${request.fagsakPersonId}")
                val personident = fagsakPersonService.hentAktivIdent(request.fagsakPersonId)
                Pair(fagsakPerson, personident)
            }

            else -> {
                throw Feil("Må oppgi enten personident eller fagsakPersonId")
            }
        }

    private fun hentEllerOpprett(
        fagsakPerson: FagsakPerson,
        stønadstype: StønadType,
    ): Fagsak =
        fagsakRepository.findByFagsakPersonIdAndStønadstype(fagsakPerson.id, stønadstype)
            ?: fagsakRepository.insert(Fagsak(fagsakPersonId = fagsakPerson.id, stønadstype = stønadstype))
}

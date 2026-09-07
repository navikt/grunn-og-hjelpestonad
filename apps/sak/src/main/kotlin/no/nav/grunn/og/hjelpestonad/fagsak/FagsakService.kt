package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.Fagsak
import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.StønadType
import no.nav.grunn.og.hjelpestonad.fagsak.dto.FagsakDto
import no.nav.grunn.og.hjelpestonad.fagsak.dto.tilDto
import no.nav.grunn.og.hjelpestonad.infrastruktur.exception.Feil
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

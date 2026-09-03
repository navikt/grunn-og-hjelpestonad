package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.fagsak.domain.Personident
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakPersonService(
    private val fagsakPersonRepository: FagsakPersonRepository,
) {
    @Transactional
    open fun hentEllerOpprettPerson(
        personidenter: Set<String>,
        gjeldendePersonident: String,
    ): FagsakPerson =
        fagsakPersonRepository.findByIdent(personidenter)
            ?: opprettFagsakPerson(gjeldendePersonident)

    fun opprettFagsakPerson(gjeldendePersonident: String): FagsakPerson = fagsakPersonRepository.insert(FagsakPerson(identer = setOf(Personident(gjeldendePersonident))))

    fun finnPerson(personidenter: Set<String>): FagsakPerson? = fagsakPersonRepository.findByIdent(personidenter)

    fun finnPersonMedId(fagsakPersonId: UUID): FagsakPerson? = fagsakPersonRepository.findById(fagsakPersonId).orElse(null)

    fun hentAktivIdent(fagsakPersonId: UUID): String {
        val fagsakPerson =
            fagsakPersonRepository
                .findById(fagsakPersonId)
                .orElseThrow { IllegalArgumentException("Fant ingen fagsakPerson med id $fagsakPersonId") }

        return fagsakPerson.identer.maxByOrNull { it.sporbar.endret.endretTid }?.ident
            ?: throw IllegalStateException("FagsakPerson har ingen identer")
    }
}

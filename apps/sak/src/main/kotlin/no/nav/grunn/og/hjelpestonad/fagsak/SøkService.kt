package no.nav.grunn.og.hjelpestonad.fagsak

import no.nav.grunn.og.hjelpestonad.fagsak.domain.FagsakPerson
import no.nav.grunn.og.hjelpestonad.pdl.Navn
import no.nav.grunn.og.hjelpestonad.pdl.PdlService
import no.nav.grunn.og.hjelpestonad.pdl.PersonResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SøkService(
    private val fagsakPersonService: FagsakPersonService,
    private val pdlService: PdlService,
) {
    fun søkPerson(personident: String): SøkeresultatResponse {
        val fagsakPerson = fagsakPersonService.finnPerson(setOf(personident))

        val person =
            if (fagsakPerson == null) {
                pdlService.hentPersonMedPersonIdent(personident)
            } else {
                pdlService.hentPersonMedFagsakPersonId(fagsakPerson.id)
            }

        return tilSøkeresultat(personident = personident, fagsakPerson = fagsakPerson, person = person)
    }

    fun søkMedFagsakPersonId(fagsakPersonId: UUID): SøkeresultatResponse {
        val fagsakPerson = fagsakPersonService.finnPersonMedId(fagsakPersonId)

        if (fagsakPerson == null) {
            return tilSøkeresultat("Ukjent", null, null)
        }
        val personident = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val person = pdlService.hentPersonMedFagsakPersonId(fagsakPersonId)

        return tilSøkeresultat(personident, fagsakPerson, person)
    }

    private fun tilSøkeresultat(
        personident: String,
        fagsakPerson: FagsakPerson?,
        person: PersonResponse?,
    ): SøkeresultatResponse =
        SøkeresultatResponse(
            navn = person?.navn?.let { "${it.fornavn} ${it.etternavn}" } ?: "Ukjent navn",
            fødselsdato = person?.foedselsdato,
            personident = personident,
            fagsakPersonId = fagsakPerson?.id,
            harFagsak = fagsakPerson != null,
        )
}

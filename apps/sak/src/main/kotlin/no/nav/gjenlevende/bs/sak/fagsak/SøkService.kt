package no.nav.gjenlevende.bs.sak.fagsak

import no.nav.gjenlevende.bs.sak.fagsak.domain.FagsakPerson
import no.nav.gjenlevende.bs.sak.felles.sikkerhet.Tilgangskontroll
import no.nav.gjenlevende.bs.sak.pdl.Navn
import no.nav.gjenlevende.bs.sak.pdl.PdlService
import no.nav.gjenlevende.bs.sak.pdl.Person
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Tilgangskontroll
class SøkService(
    private val fagsakPersonService: FagsakPersonService,
    private val pdlService: PdlService,
) {
    fun søkPerson(personident: String): Søkeresultat {
        val fagsakPerson = fagsakPersonService.finnPerson(setOf(personident))

        val person =
            if (fagsakPerson == null) {
                pdlService.hentPersonMedPersonIdent(personident)
            } else {
                pdlService.hentPersonMedFagsakPersonId(fagsakPerson.id)
            }

        return tilSøkeresultat(personident = personident, fagsakPerson = fagsakPerson, person = person)
    }

    fun søkMedFagsakPersonId(fagsakPersonId: UUID): Søkeresultat {
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
        person: Person?,
    ): Søkeresultat =
        Søkeresultat(
            navn = person?.navn?.let { "${it.fornavn} ${it.etternavn}" } ?: "Ukjent navn",
            fødselsdato = person?.foedselsdato,
            personident = personident,
            fagsakPersonId = fagsakPerson?.id,
            harFagsak = fagsakPerson != null,
        )
}
